package com.ultrasandbox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProcessSandbox {

    public static boolean isDebuggerConnected() {
        return false;
    }

    private static boolean shouldBlock(String cmd) {
        if (cmd == null) return false;
        var str = cmd.trim().toLowerCase();
        if (str.isEmpty()) return false;

        return str.contains("dumpsys") && (str.contains("vpn") ||
            str.contains("connectivity")) ||
            str.equals("su") || str.startsWith("su ") ||
            str.contains("which su") || str.contains("/system/xbin/su") ||
            str.contains("/sbin/su");
    }

    private static boolean shouldBlock(String[] cmd) {
        if (cmd == null || cmd.length == 0) return false;
        return shouldBlock(String.join(" ", cmd));
    }

    public static Process execArray(Runtime runtime, String[] cmd) throws IOException {
        if (shouldBlock(cmd)) return new FakeProcess();
        return runtime.exec(cmd);
    }

    public static Process execString(Runtime runtime, String cmd) throws IOException {
        if (shouldBlock(cmd)) return new FakeProcess();
        return runtime.exec(cmd);
    }

    public static Process execFull(Runtime runtime, String[] cmd, String[] env, File dir) throws IOException {
        if (shouldBlock(cmd)) return new FakeProcess();
        return runtime.exec(cmd, env, dir);
    }

    public static Process processBuilderStart(ProcessBuilder pb) throws IOException {
        var cmd = pb.command();
        if (cmd != null && shouldBlock(cmd.toArray(new String[0]))) return new FakeProcess();
        return pb.start();
    }

    private static class NoopOutputStream extends OutputStream {
        @Override
        public void write(int b) {}

        @Override
        public void write(byte[] b, int off, int len) {}
    }

    private static class FakeProcess extends Process {
        private final InputStream emptyIn = new ByteArrayInputStream(new byte[0]);
        private final InputStream errorIn = new ByteArrayInputStream("Permission Denial\n".getBytes());
        private final OutputStream noopOut = new NoopOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return noopOut;
        }

        @Override
        public InputStream getInputStream() {
            return emptyIn;
        }

        @Override
        public InputStream getErrorStream() {
            return errorIn;
        }

        @Override
        public int waitFor() {
            return 1;
        }

        @Override
        public int exitValue() {
            return 1;
        }

        @Override
        public void destroy() {}
    }
}
