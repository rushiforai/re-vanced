package com.ultrasandbox.test

import android.accounts.AccountManager
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ClipboardManager
import android.database.Cursor
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Debug
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private var pass = 0
    private var fail = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            setBackgroundColor(0xFF0d1117.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        header("ULTRASANDBOX TEST")

        section("NETWORK")
        testNetworkCaps()
        testInterfaces()
        testMTU()
        testPorts()
        testDNS()
        testProxy()

        section("PACKAGES")
        testInstalledPackages()
        testInstalledApplications()
        testSpecificPkgs()
        testVpnServices()
        testQueryActivities()

        section("WIFI / BLUETOOTH")
        testWifi()
        testBluetooth()

        section("DEVICE IDENTITY")
        testAndroidId()
        testTelephony()
        testAccounts()
        testClipboard()

        section("CONTENT")
        testContent("Contacts", ContactsContract.Contacts.CONTENT_URI)
        testContent("Call log", CallLog.Calls.CONTENT_URI)
        testContent("SMS", Telephony.Sms.CONTENT_URI)

        section("FILESYSTEM")
        testRootFiles()
        testProcNetRead()
        testProcNetCanRead()
        testProcMaps()
        testProcStatus()
        testFileInputStreamHidden()
        testFileReaderHidden()

        section("PROCESS")
        testDebugger()
        testExecArray()
        testExecString()
        testExecDumpsys()
        testProcessBuilder()

        section("LOCATION")
        testCellInfo()
        testCellLocation()

        section("RESULT")
        val total = pass + fail
        result("Score", "$pass clean / $fail exposed / $total total", fail == 0)
    }

    // Network

    private fun testNetworkCaps() = safe("NetworkCaps") {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: run { result("NetworkCaps", "no network", true); return@safe }

        result(
            "hasTransport(VPN)", "${caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)}",
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        )

        result(
            "hasCapability(NOT_VPN)",
            "${caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)}",
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        )

        val str = caps.toString()
        val markers = str.contains("IS_VPN") || str.contains("VpnTransportInfo")
        result("caps.toString", if (markers) "LEAKED" else "clean", !markers)
    }

    private fun testInterfaces() = safe("Interfaces") {
        val vpn = NetworkInterface.getNetworkInterfaces()?.toList()
            ?.filter { it.name.lowercase().matches(Regex("^(tun|tap|wg|ppp|pptp|ipsec)\\d*")) }
            ?.map { it.name } ?: emptyList()
        result("VPN interfaces", vpn.ifEmpty { listOf("none") }.toString(), vpn.isEmpty())
    }

    private fun testMTU() = safe("MTU") {
        val bad = NetworkInterface.getNetworkInterfaces()?.toList()
            ?.filter { it.isUp && !it.isLoopback && it.mtu in 1200..1499 }
            ?.map { "${it.name}=${it.mtu}" } ?: emptyList()
        result("MTU anomaly", bad.ifEmpty { listOf("normal") }.toString(), bad.isEmpty())
    }

    private fun testPorts() = safe("Ports") {
        // Test both connect(addr, timeout) and connect(addr) variants
        val ports = intArrayOf(10808, 10809, 7890, 9090, 1080, 2080, 3066, 27042)
        val open = ports.filter { port ->
            runCatching {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }; true
            }.getOrDefault(false)
        }
        result(
            "Localhost (timeout)",
            if (open.isEmpty()) "all closed" else "OPEN: $open",
            open.isEmpty()
        )

        // No-timeout variant
        val openNoTimeout = intArrayOf(10808, 7890).filter { port ->
            runCatching {
                val s = Socket()
                s.soTimeout = 200
                s.connect(InetSocketAddress("127.0.0.1", port)); s.close(); true
            }.getOrDefault(false)
        }
        result(
            "Localhost (no-timeout)",
            if (openNoTimeout.isEmpty()) "all closed" else "OPEN: $openNoTimeout",
            openNoTimeout.isEmpty()
        )
    }

    private fun testDNS() = safe("DNS") {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val dns = cm.activeNetwork?.let { cm.getLinkProperties(it) }?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.filter { it.startsWith("10.") || it.startsWith("192.168.") || it.startsWith("100.64.") }
            ?: emptyList()
        result("Private DNS", dns.ifEmpty { listOf("none") }.toString(), dns.isEmpty())
    }

    private fun testProxy() = safe("Proxy") {
        val keys = listOf(
            "http.proxyHost", "http.proxyPort", "socksProxyHost", "socksProxyPort",
            "https.proxyHost", "ftp.proxyHost"
        )
        val found = keys.mapNotNull { k ->
            System.getProperty(k)?.takeIf { it.isNotEmpty() }?.let { "$k=$it" }
        }
        result("Proxy props", found.ifEmpty { listOf("none") }.toString(), found.isEmpty())
    }

    // Packages

    private fun testInstalledPackages() = safe("InstalledPkgs") {
        val n = packageManager.getInstalledPackages(0).size
        result("getInstalledPackages", "$n", n < 150)
    }

    private fun testInstalledApplications() = safe("InstalledApps") {
        val n = packageManager.getInstalledApplications(0).size
        result("getInstalledApplications", "$n", n < 150)
    }

    private fun testSpecificPkgs() = safe("SpecificPkgs") {
        val targets = listOf(
            "com.v2ray.ang", "io.nekohasekai.sfa", "org.torproject.torbrowser",
            "com.wireguard.android", "org.amnezia.vpn", "net.typeblog.shelter",
            "com.github.shadowsocks", "moe.nb4a"
        )
        val found =
            targets.filter { runCatching { packageManager.getPackageInfo(it, 0) }.isSuccess }
        result("VPN/privacy pkgs", found.ifEmpty { listOf("none") }.toString(), found.isEmpty())
    }

    private fun testVpnServices() = safe("VpnSvc") {
        val n = packageManager.queryIntentServices(
            android.content.Intent("android.net.VpnService"),
            0
        ).size
        result("queryIntentServices(VPN)", "$n", n == 0)
    }

    private fun testQueryActivities() = safe("QueryAct") {
        // Query for a common action that non-system apps handle
        val intent =
            android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain" }
        val all = packageManager.queryIntentActivities(intent, 0)
        val nonSystem = all.filter { ri ->
            val ai = ri.activityInfo?.applicationInfo ?: return@filter false
            (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
        }
        result("queryIntentActivities non-system", "${nonSystem.size}", nonSystem.isEmpty())
    }

    // WiFi / Bluetooth

    @Suppress("DEPRECATION")
    private fun testWifi() = safe("WiFi") {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        result("getScanResults", "${wm.scanResults.size}", wm.scanResults.isEmpty())
        result(
            "getConfiguredNetworks", "${wm.configuredNetworks?.size ?: 0}",
            (wm.configuredNetworks?.size ?: 0) == 0
        )
        wm.connectionInfo?.let { wi ->
            result("getSSID", wi.ssid ?: "null", wi.ssid?.contains("unknown") == true)
            result(
                "getBSSID", wi.bssid ?: "null",
                wi.bssid == null || wi.bssid == "02:00:00:00:00:00"
            )
            result("getMacAddress", wi.macAddress ?: "null", wi.macAddress == "02:00:00:00:00:00")
        }
    }

    private fun testBluetooth() = safe("BT") {
        val bt = BluetoothAdapter.getDefaultAdapter()
            ?: run { result("getBondedDevices", "no adapter", true); return@safe }
        result("getBondedDevices", "${bt.bondedDevices.size}", bt.bondedDevices.isEmpty())
    }

    // Device

    private fun testAndroidId() = safe("AndroidID") {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        result("Android ID", id ?: "null", id != null && !id.contains("PLACEHOLDER"))
    }

    @Suppress("DEPRECATION")
    private fun testTelephony() = safe("Telephony") {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val deviceId = runCatching { tm.deviceId }.getOrNull()
        result("getDeviceId", deviceId ?: "null", deviceId == null)

        val imei = runCatching { tm.imei }.getOrNull()
        result("getImei", imei ?: "null", imei == null)

        val sub = runCatching { tm.subscriberId }.getOrNull()
        result("getSubscriberId", sub ?: "null", sub == null)

        val sim = runCatching { tm.simSerialNumber }.getOrNull()
        result("getSimSerialNumber", sim ?: "null", sim == null)

        val line = runCatching { tm.line1Number }.getOrNull()
        result("getLine1Number", line ?: "null", line == null)
    }

    private fun testAccounts() = safe("Accounts") {
        val all = AccountManager.get(this).accounts
        val nonGoogle = all.filter { it.type != "com.google" }
        result("Non-Google accounts", "${nonGoogle.size}", nonGoogle.isEmpty())
    }

    private fun testClipboard() = safe("Clipboard") {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        result("hasPrimaryClip", "${cm.hasPrimaryClip()}", !cm.hasPrimaryClip())
        result(
            "getPrimaryClip",
            if (cm.primaryClip != null) "has data" else "null",
            cm.primaryClip == null
        )
    }

    // Content

    private fun testContent(label: String, uri: Uri) = safe(label) {
        var c: Cursor? = null
        try {
            c = contentResolver.query(uri, null, null, null, null)
            val n = c?.count ?: -1
            result(label, if (n < 0) "null (blocked)" else "$n rows", n <= 0)
        } catch (e: Exception) {
            result(label, "blocked", true)
        } finally {
            c?.close()
        }
    }

    // FS

    private fun testRootFiles() = safe("RootFiles") {
        val paths = listOf(
            "/system/xbin/su", "/system/bin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/sbin/.magisk",
            "/system/framework/XposedBridge.jar", "/system/xbin/busybox"
        )
        val found = paths.filter { File(it).exists() }
        result("File.exists(root)", found.ifEmpty { listOf("none") }.toString(), found.isEmpty())
    }

    private fun testProcNetRead() = safe("ProcNetRead") {
        // Via FileInputStream
        try {
            val lines =
                BufferedReader(InputStreamReader(FileInputStream("/proc/net/tcp"))).use { it.readLines() }
            val vpnPorts = setOf("2A30", "2A31", "2A32", "1ED2", "1ED3", "0820", "0821")
            val suspicious = lines.drop(1).filter { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 4) return@filter false
                parts[1].substringAfter(":").uppercase() in vpnPorts
            }
            result(
                "FIS /proc/net/tcp ports",
                if (suspicious.isEmpty()) "clean" else "${suspicious.size} found",
                suspicious.isEmpty()
            )
        } catch (e: Exception) {
            result("FIS /proc/net/tcp", "blocked: ${e.javaClass.simpleName}", true)
        }

        // Via FileReader
        try {
            val lines = BufferedReader(FileReader("/proc/net/tcp")).use { it.readLines() }
            val myUid = android.os.Process.myUid()
            val otherApps = lines.drop(1).count { line ->
                val uid = line.trim().split("\\s+".toRegex()).getOrNull(7)?.toIntOrNull()
                    ?: return@count false
                uid >= 10000 && uid != myUid
            }
            result(
                "FR /proc/net/tcp other apps",
                if (otherApps == 0) "hidden" else "$otherApps visible",
                otherApps == 0
            )
        } catch (e: Exception) {
            result("FR /proc/net/tcp", "blocked: ${e.javaClass.simpleName}", true)
        }

        // Via File.readLines (Kotlin ext)
        try {
            val lines = File("/proc/net/tcp").readLines()
            result("readLines /proc/net/tcp", "${lines.size} lines", true)
        } catch (e: Exception) {
            result("readLines /proc/net/tcp", "blocked: ${e.javaClass.simpleName}", true)
        }
    }

    private fun testProcNetCanRead() = safe("ProcNetCanRead") {
        val paths = listOf("/proc/net/tcp", "/proc/net/route", "/proc/net/udp")
        val readable = paths.filter { File(it).canRead() }
        // On API 29+, SELinux blocks these. On older, our sandbox blocks.
        // Either way, canRead=false is correct.
        result(
            "File.canRead(/proc/net)",
            if (readable.isEmpty()) "all blocked" else readable.toString(),
            readable.isEmpty()
        )
    }

    private fun testProcMaps() = safe("ProcMaps") {
        try {
            val content = FileInputStream("/proc/self/maps").bufferedReader().readText().lowercase()
            val leaks =
                listOf("frida", "xposed", "magisk", "substrate").filter { content.contains(it) }
            result(
                "/proc/self/maps",
                if (leaks.isEmpty()) "clean" else "LEAKED: $leaks",
                leaks.isEmpty()
            )
        } catch (e: Exception) {
            result("/proc/self/maps", "error: ${e.javaClass.simpleName}", true)
        }
    }

    private fun testProcStatus() = safe("ProcStatus") {
        try {
            val lines = FileInputStream("/proc/self/status").bufferedReader().readLines()
            val pid = lines.find { it.startsWith("TracerPid:") }
                ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
            result("TracerPid", "$pid", pid == 0)
        } catch (e: Exception) {
            result("TracerPid", "error: ${e.javaClass.simpleName}", true)
        }
    }

    private fun testFileInputStreamHidden() = safe("FIS") {
        val blocked = try {
            FileInputStream("/system/xbin/su").close(); false
        } catch (e: java.io.FileNotFoundException) {
            true
        }
        result("FIS /system/xbin/su", if (blocked) "blocked" else "READABLE", blocked)
    }

    private fun testFileReaderHidden() = safe("FR") {
        val blocked = try {
            FileReader("/system/xbin/su").close(); false
        } catch (e: java.io.FileNotFoundException) {
            true
        }
        result("FR /system/xbin/su", if (blocked) "blocked" else "READABLE", blocked)
    }

    // Process

    private fun testDebugger() = safe("Debugger") {
        result(
            "isDebuggerConnected",
            "${Debug.isDebuggerConnected()}",
            !Debug.isDebuggerConnected()
        )
    }

    private fun testExecArray() = safe("ExecArr") {
        try {
            val exit = Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor()
            result("exec(String[]) su", "exit=$exit", exit != 0)
        } catch (e: Exception) {
            result("exec(String[]) su", "blocked", true)
        }
    }

    private fun testExecString() = safe("ExecStr") {
        try {
            val exit = Runtime.getRuntime().exec("su -c id").waitFor()
            result("exec(String) su", "exit=$exit", exit != 0)
        } catch (e: Exception) {
            result("exec(String) su", "blocked", true)
        }
    }

    private fun testExecDumpsys() = safe("Dumpsys") {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("dumpsys", "vpn_management"))
            val output = p.inputStream.bufferedReader().readText()
            val error = p.errorStream.bufferedReader().readText()
            val exit = p.waitFor()
            val blocked = exit != 0 || error.contains("Permission") || output.isEmpty()
            result("dumpsys vpn", if (blocked) "blocked (exit=$exit)" else "LEAKED", blocked)
        } catch (e: Exception) {
            result("dumpsys vpn", "blocked", true)
        }
    }

    private fun testProcessBuilder() = safe("PB") {
        try {
            val exit = ProcessBuilder("su", "-c", "id").start().waitFor()
            result("ProcessBuilder su", "exit=$exit", exit != 0)
        } catch (e: Exception) {
            result("ProcessBuilder su", "blocked", true)
        }
    }

    // Location

    @Suppress("DEPRECATION")
    private fun testCellInfo() = safe("CellInfo") {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val n = runCatching { tm.allCellInfo }.getOrNull()?.size ?: 0
        result("getAllCellInfo", "$n", n == 0)
    }

    @Suppress("DEPRECATION")
    private fun testCellLocation() = safe("CellLoc") {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val loc = runCatching { tm.cellLocation }.getOrNull()
        result("getCellLocation", if (loc == null) "null" else "present", loc == null)
    }

    // UI

    private fun header(text: String) {
        root.addView(TextView(this).apply {
            this.text = text; textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(0xFF58a6ff.toInt()); gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        })
    }

    private fun section(text: String) {
        root.addView(TextView(this).apply {
            this.text = "━━━ $text ━━━"; textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(0xFF8b949e.toInt()); setPadding(0, 24, 0, 8)
        })
    }

    private fun result(label: String, value: String, clean: Boolean) {
        if (clean) pass++ else fail++
        val color = if (clean) 0xFF3fb950.toInt() else 0xFFf85149.toInt()
        root.addView(LinearLayout(this).apply {
            setPadding(0, 6, 0, 6)
            addView(TextView(context).apply {
                text = "● "; setTextColor(color); typeface = Typeface.MONOSPACE; textSize = 14f
            })
            addView(TextView(context).apply {
                text = "$label: $value"; setTextColor(0xFFc9d1d9.toInt())
                typeface = Typeface.MONOSPACE; textSize = 12f
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        })
    }

    private fun safe(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            result(tag, "error: ${e.message}", true)
        }
    }
}
