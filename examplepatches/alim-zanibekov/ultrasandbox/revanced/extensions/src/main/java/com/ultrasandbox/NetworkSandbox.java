package com.ultrasandbox;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

public final class NetworkSandbox {

    private NetworkSandbox() {}

    private static final Set<Integer> ALLOWED_LOOPBACK_PORTS = new HashSet<>(List.of(
        53,        // DNS
        67, 68,    // DHCP
        123,       // NTP
        5353,      // mDNS
        9000, 9001 // DNS resolver proxy
    ));

    private static final Set<String> PROXY_KEYS = new HashSet<>(List.of(
        "http.proxyHost", "http.proxyPort",
        "https.proxyHost", "https.proxyPort",
        "socksProxyHost", "socksProxyPort", "socksProxyVersion",
        "ftp.proxyHost", "ftp.proxyPort"
    ));


    public static boolean hasTransport(NetworkCapabilities nc, int transport) {
        if (transport == NetworkCapabilities.TRANSPORT_VPN) return false;
        return nc.hasTransport(transport);
    }

    public static boolean hasCapability(NetworkCapabilities nc, int cap) {
        if (cap == NetworkCapabilities.NET_CAPABILITY_NOT_VPN) return true;
        return nc.hasCapability(cap);
    }

    public static String capsToString(NetworkCapabilities nc) {
        var s = nc.toString().replace("IS_VPN", "");
        int start;
        while ((start = s.indexOf("VpnTransportInfo{")) != -1) {
            int end = s.indexOf('}', start);
            s = s.substring(0, start) + (end >= 0 ? s.substring(end + 1) : "");
        }
        if (!s.contains("NOT_VPN")) {
            s = s.replace("Capabilities: ", "Capabilities: NOT_VPN&");
        }
        return s;
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        var all = NetworkInterface.getNetworkInterfaces();
        if (all == null) return null;

        var filtered = new Vector<NetworkInterface>();
        while (all.hasMoreElements()) {
            NetworkInterface iface = all.nextElement();

            if (iface.getName() != null && iface.getName().matches("^(tun|tap|wg|ppp|pptp|ipsec)\\d*")) {
                continue;
            }
            filtered.add(iface);
        }

        return filtered.elements();
    }

    public static int getMTU(NetworkInterface iface) throws SocketException {
        var mtu = iface.getMTU();
        if (!iface.isLoopback() && mtu >= 1200 && mtu <= 1499) return 1500;
        return mtu;
    }

    public static void socketConnect(Socket sock, SocketAddress addr, int timeout) throws IOException {
        checkBlockedAddress(addr);
        sock.connect(addr, timeout);
    }

    public static void socketConnectNoTimeout(Socket sock, SocketAddress addr) throws IOException {
        checkBlockedAddress(addr);
        sock.connect(addr);
    }

    private static void checkBlockedAddress(SocketAddress addr) throws ConnectException {
        if (!(addr instanceof InetSocketAddress)) return;

        var isa = (InetSocketAddress) addr;
        if (isLoopback(isa) && !ALLOWED_LOOPBACK_PORTS.contains(isa.getPort())) {
            throw new ConnectException("Connection refused");
        }
    }

    private static boolean isLoopback(InetSocketAddress isa) {
        var addr = isa.getAddress();
        if (addr != null) {
            return addr.isLoopbackAddress();
        }

        var host = isa.getHostName();
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    public static List<InetAddress> getDnsServers(LinkProperties lp) {
        var filtered = lp.getDnsServers()
            .stream()
            .filter(it -> !it.isSiteLocalAddress() && !it.isLoopbackAddress() && !isCGNAT(it))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            try {
                filtered.add(InetAddress.getByName("8.8.8.8"));
            } catch (Exception ignored) {}
        }

        return filtered;
    }

    // strip routes through tun/tap/wg/ppp/ipsec interfaces
    public static List<RouteInfo> getRoutes(LinkProperties lp) {
        return lp.getRoutes().stream()
            .filter(r -> {
                var iface = r.getInterface();
                if (iface == null) return true;
                return !iface.matches("^(tun|tap|wg|ppp|pptp|ipsec)\\d*");
            })
            .collect(java.util.stream.Collectors.toList());
    }

    // Tailscale/WG
    private static boolean isCGNAT(InetAddress addr) {
        var ab = addr.getAddress();
        if (ab == null || ab.length != 4) return false;
        return (ab[0] & 0xFF) == 100 && (ab[1] & 0xC0) == 64;
    }

    public static String getProperty(String key) {
        if (PROXY_KEYS.contains(key)) return null;
        return System.getProperty(key);
    }

    public static String getPropertyDefault(String key, String def) {
        if (PROXY_KEYS.contains(key)) return null;
        return System.getProperty(key, def);
    }
}
