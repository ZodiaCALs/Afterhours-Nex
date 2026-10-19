package app.afterhours.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

class Fetched(val bytes: ByteArray, val type: String, val finalUrl: String) {
    val text: String by lazy { Net.decode(bytes, type) }
}

/** Fetching from podcast sites, with a second, browser-like try when a site turns Afterhours away. */
object Net {
    const val APP_UA = "Mozilla/5.0 (compatible; ${AppInfo.NAME}/${AppInfo.VERSION}; podcast player)"
    const val BROWSER_UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    private const val ACCEPT = "application/rss+xml, application/xml;q=0.9, text/xml;q=0.9, application/json;q=0.8, text/html;q=0.7, */*;q=0.5"
    private val RETRY = setOf(403, 405, 406, 429, 451, 503)

    /** Decodes text in a charset other than UTF-8; set by the platform. */
    var charsetDecoder: (ByteArray, String) -> String? = { _, _ -> null }

    val client: HttpClient by lazy {
        HttpClient {
            followRedirects = true
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    suspend fun fetch(url: String, maxBytes: Int = 25 shl 20): Fetched {
        if (!url.startsWith("http://") && !url.startsWith("https://")) throw FetchException("That isn't a web address Afterhours can open.")
        var r = request(url, APP_UA)
        if (r.status.value in RETRY) r = request(url, BROWSER_UA)
        if (r.status.value >= 400) throw FetchException("The site answered with an error (${r.status.value}).")
        val length = r.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (length != null && length > maxBytes) throw FetchException("That file is too large to load.")
        val bytes: ByteArray = try { r.body() } catch (e: CancellationException) { throw e } catch (e: Exception) { throw FetchException("The site stopped answering partway through.") }
        return Fetched(if (bytes.size > maxBytes) bytes.copyOf(maxBytes) else bytes, r.headers[HttpHeaders.ContentType].orEmpty(), r.call.request.url.toString())
    }

    private suspend fun request(url: String, ua: String): HttpResponse = try {
        client.get(url) {
            header(HttpHeaders.UserAgent, ua)
            header(HttpHeaders.Accept, ACCEPT)
            if (ua == BROWSER_UA) header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9,ja;q=0.8,zh;q=0.7")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        throw FetchException("The site took too long to answer.")
    } catch (e: Exception) {
        throw FetchException("Couldn't reach the site (${e::class.simpleName ?: "network error"}).")
    }

    fun decode(bytes: ByteArray, type: String): String {
        val head = bytes.copyOf(minOf(bytes.size, 300)).decodeToString()
        val charset = Regex("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)
            ?: Regex("charset=[\"']?([^;\"'\\s]+)", RegexOption.IGNORE_CASE).find(type)?.groupValues?.get(1)
        if (charset == null || charset.equals("utf-8", true) || charset.equals("utf8", true)) return bytes.decodeToString()
        return charsetDecoder(bytes, charset) ?: bytes.decodeToString()
    }
}
