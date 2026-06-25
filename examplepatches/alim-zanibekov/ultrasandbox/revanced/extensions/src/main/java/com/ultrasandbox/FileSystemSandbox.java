package com.ultrasandbox;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileSystemSandbox {
    private static final int FIRST_APP_UID = 10000;

    private static final Set<String> HIDDEN_PATHS = new HashSet<>(List.of(
        "/system/xbin/su", "/system/bin/su", "/sbin/su",
        "/system/su", "/system/bin/.ext/.su", "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk", "/system/etc/init.d/99SuperSUDaemon",
        "/dev/com.koushikdutta.superuser.daemon/", "/sbin/.magisk",
        "/sbin/.core", "/dev/.magisk/", "/system/framework/XposedBridge.jar",
        "/system/lib/libxposed_art.so", "/system/xbin/busybox", "/system/bin/busybox"
    ));

    private static final Set<String> FILTERED_PATHS = new HashSet<>(List.of(
        "/proc/self/maps",
        "/proc/self/status",
        "/proc/net/tcp", "/proc/net/tcp6",
        "/proc/net/udp", "/proc/net/udp6",
        "/proc/net/route"
    ));

    private static boolean isHidden(File f) {
        return HIDDEN_PATHS.contains(f.getAbsolutePath());
    }

    public static boolean fileExists(File f) {
        if (isHidden(f)) return false;
        return f.exists();
    }

    public static boolean fileCanRead(File f) {
        if (isHidden(f)) return false;
        return f.canRead();
    }

    public static FileReader newFileReader(File f) throws FileNotFoundException {
        var path = f.getAbsolutePath();
        if (HIDDEN_PATHS.contains(path)) {
            throw new FileNotFoundException(path);
        }
        if (FILTERED_PATHS.contains(path)) {
            return new FilteredFileReader(f);
        }
        return new FileReader(f);
    }

    public static FileReader newFileReaderByPath(String path) throws FileNotFoundException {
        return newFileReader(new File(path));
    }

    public static FileInputStream openFileInputStream(File f) throws FileNotFoundException {
        var path = f.getAbsolutePath();
        if (HIDDEN_PATHS.contains(path)) throw new FileNotFoundException(path);
        if (FILTERED_PATHS.contains(path)) return new FilteredFileInputStream(f);
        return new FileInputStream(f);
    }

    public static FileInputStream openFileInputStreamByPath(String path) throws FileNotFoundException {
        return openFileInputStream(new File(path));
    }

    public static RandomAccessFile openRandomAccessFile(File f, String mode) throws FileNotFoundException {
        var path = f.getAbsolutePath();
        if (HIDDEN_PATHS.contains(path) || FILTERED_PATHS.contains(path)) {
            throw new FileNotFoundException(path);
        }
        return new RandomAccessFile(f, mode);
    }

    @android.annotation.TargetApi(26)
    public static byte[] readAllBytes(Path p) throws IOException {
        var path = p.toAbsolutePath().toString();
        if (HIDDEN_PATHS.contains(path)) throw new FileNotFoundException(path);
        if (FILTERED_PATHS.contains(path)) {
            return filterContent(path).getBytes(StandardCharsets.UTF_8);
        }
        return Files.readAllBytes(p);
    }

    // Serves filtered content. If SELinux blocks the file, super(f) throws — which is correct.
    private static class FilteredFileReader extends FileReader {
        private final InputStreamReader delegate;

        FilteredFileReader(File f) throws FileNotFoundException {
            super(f);
            var filtered = filterContent(f.getAbsolutePath());
            delegate = new InputStreamReader(
                new ByteArrayInputStream(filtered.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
            );
            try {
                super.close();
            } catch (IOException ignored) {}
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(char[] buf, int off, int len) throws IOException {
            return delegate.read(buf, off, len);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    // Serves filtered content. If SELinux blocks the file, super(f) throws — which is correct.
    private static class FilteredFileInputStream extends FileInputStream {
        private final ByteArrayInputStream delegate;

        FilteredFileInputStream(File f) throws FileNotFoundException {
            super(f);
            var content = filterContent(f.getAbsolutePath());
            delegate = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            try {super.close();} catch (IOException ignored) {}
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return delegate.read(b, off, len);
        }

        @Override
        public int available() {
            return delegate.available();
        }

        @Override
        public long skip(long n) {
            return delegate.skip(n);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }


    private static String filterContent(String path) {
        var sb = new StringBuilder();

        try (var br = new BufferedReader(new FileReader(path))) {
            var first = true;
            String line;
            while ((line = br.readLine()) != null) {
                if (first) { // header
                    sb.append(line).append('\n');
                    first = false;
                    continue;
                }

                switch (path) {
                    case "/proc/self/maps":
                        if (containsAnalysisTool(line)) continue;
                        break;
                    case "/proc/self/status":
                        if (line.startsWith("TracerPid:")) {
                            sb.append("TracerPid:\t0\n");
                            continue;
                        }
                        break;
                    case "/proc/net/tcp":
                    case "/proc/net/tcp6":
                    case "/proc/net/udp":
                    case "/proc/net/udp6":
                        if (!isOwnOrSystemEntry(line)) continue;
                        break;
                    case "/proc/net/route":
                        if (isVpnRoute(line)) continue;
                        break;
                }
                sb.append(line).append('\n');
            }
        } catch (IOException ignored) {}

        return sb.toString();
    }

    private static boolean containsAnalysisTool(String line) {
        var l = line.toLowerCase();
        return l.contains("frida") || l.contains("xposed")
            || l.contains("magisk") || l.contains("substrate");
    }

    // Keep only system uid (< 10000) and the app's own uid.
    private static boolean isOwnOrSystemEntry(String line) {
        var fields = line.trim().split("\\s+");
        if (fields.length < 8) return false;
        try {
            int uid = Integer.parseInt(fields[7]);
            return uid < FIRST_APP_UID || uid == android.os.Process.myUid();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Filter tun/tap/wg/ppp/ipsec interfaces and /32 host routes
    private static boolean isVpnRoute(String line) {
        var fields = line.trim().split("\\s+");
        if (fields.length < 8) return false;
        var iface = fields[0];
        var mask = fields[7];
        if (iface.matches("^(tun|tap|wg|ppp|pptp|ipsec)\\d*")) return true;
        return "FFFFFFFF".equals(mask) && !iface.startsWith("lo");
    }
}
