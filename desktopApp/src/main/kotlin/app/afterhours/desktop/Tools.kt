package app.afterhours.desktop

import app.afterhours.core.FetchException
import java.io.File
import java.util.concurrent.TimeUnit

/** Finds a command-line tool the listener installed: on the PATH, or where Homebrew, pip and winget put them. */
fun findTool(name: String): String? {
    val win = System.getProperty("os.name").startsWith("Win")
    val exe = if (win) "$name.exe" else name
    val home = System.getProperty("user.home")
    val dirs = System.getenv("PATH").orEmpty().split(File.pathSeparator) + listOf("/opt/homebrew/bin", "/usr/local/bin", "$home/.local/bin", "$home/bin",
        "$home/Library/Python/3.12/bin", "$home/Library/Python/3.11/bin", "${System.getenv("LOCALAPPDATA")}\\Microsoft\\WinGet\\Links",
        "${System.getenv("APPDATA")}\\Python\\Scripts")
    return dirs.filter { it.isNotBlank() }.map { File(it, exe) }.firstOrNull { it.isFile && it.canExecute() }?.absolutePath
}

/** Runs a tool and returns what it printed. Each line also goes to [onLine] as it arrives. */
fun runTool(exe: String, args: List<String>, timeoutSeconds: Long = 600, onLine: (String) -> Unit = {}): String {
    val p = ProcessBuilder(listOf(exe) + args).redirectErrorStream(false).start()
    val err = StringBuilder()
    val errReader = Thread { p.errorStream.bufferedReader().forEachLine { err.appendLine(it); onLine(it) } }.apply { isDaemon = true; start() }
    val out = StringBuilder()
    p.inputStream.bufferedReader().forEachLine { out.appendLine(it); onLine(it) }
    if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { p.destroyForcibly(); throw FetchException("$exe took too long.") }
    errReader.join(2000)
    if (p.exitValue() != 0) throw FetchException(err.lines().lastOrNull { it.isNotBlank() }?.removePrefix("ERROR: ")?.take(200) ?: "$exe stopped with an error.")
    return out.toString()
}

/** FFmpeg takes a local file as a plain path (a file: address with %20 in it wouldn't open). */
fun ffmpegInput(url: String): String = if (url.startsWith("file:")) runCatching { File(java.net.URI(url)).path }.getOrDefault(url) else url
