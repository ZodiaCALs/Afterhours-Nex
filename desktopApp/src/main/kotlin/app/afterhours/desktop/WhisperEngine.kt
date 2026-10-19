package app.afterhours.desktop

import app.afterhours.core.AsrOptions
import app.afterhours.core.CaptionEngine
import app.afterhours.core.Cue
import app.afterhours.core.FetchException
import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import io.github.givimad.whisperjni.WhisperSamplingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/** Auto captions on desktop: OpenAI's Whisper models through whisper.cpp, with FFmpeg reading the episode. */
class WhisperEngine(dataDir: File) : CaptionEngine {
    override val models = listOf("tiny" to "75 MB", "base" to "142 MB", "small" to "466 MB", "medium" to "1.5 GB", "large-v3-turbo" to "1.6 GB", "large-v3" to "2.9 GB")
    private val modelDir = File(dataDir, "models").apply { mkdirs() }
    private val jni: WhisperJNI by lazy {
        WhisperJNI.loadLibrary()
        WhisperJNI.setLibraryLogger(null)
        WhisperJNI()
    }
    private var loaded: Pair<String, WhisperContext>? = null

    private fun modelFile(name: String) = File(modelDir, "ggml-$name.bin")
    override fun hasModel(name: String) = modelFile(name).length() > 0

    override suspend fun transcribe(url: String, startMs: Long, durationMs: Long, options: AsrOptions,
                                    status: (String, Float) -> Unit, onCues: (List<Cue>) -> Unit) = withContext(Dispatchers.IO) {
        val ctx = context(options.model, status)
        status("Auto captions: recognising speech…", -1f)
        val proc = ProcessBuilder(Ffmpeg.path, "-nostdin", "-hide_banner", "-loglevel", "error", "-ss", String.format(Locale.ROOT, "%.2f", startMs / 1000.0),
            "-i", url, "-vn", "-ac", "1", "-ar", "16000", "-f", "f32le", "pipe:1").redirectError(ProcessBuilder.Redirect.DISCARD).start()
        try {
            val input = proc.inputStream
            val bytes = ByteArray(16000 * 30 * 4)          // 30 seconds of 16 kHz audio at a time
            var offset = startMs
            while (true) {
                ensureActive()
                val n = input.readNBytes(bytes, 0, bytes.size)
                if (n < 16000 * 4) break                     // less than a second left
                val samples = FloatArray(n / 4)
                ByteBuffer.wrap(bytes, 0, samples.size * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(samples)
                val result = jni.full(ctx, params(options), samples, samples.size)
                if (result != 0) throw FetchException("Speech recognition stopped unexpectedly (code $result).")
                val cues = (0 until jni.fullNSegments(ctx)).map { i ->
                    Cue(offset + jni.fullGetSegmentTimestamp0(ctx, i) * 10, offset + jni.fullGetSegmentTimestamp1(ctx, i) * 10, jni.fullGetSegmentText(ctx, i).trim())
                }.filter { it.text.isNotBlank() && !it.text.startsWith("[") && !it.text.startsWith("(") }
                ensureActive()
                onCues(cues)
                offset += samples.size / 16L
                status("Auto captions: recognising speech", if (durationMs > 0) (offset.toFloat() / durationMs).coerceIn(0f, 1f) else -1f)
                if (n < bytes.size) break
            }
        } finally {
            proc.destroyForcibly()
        }
    }

    @Synchronized
    private fun context(name: String, status: (String, Float) -> Unit): WhisperContext {
        loaded?.let { (current, ctx) -> if (current == name) return ctx; ctx.close() }
        loaded = null
        val file = modelFile(name)
        if (!hasModel(name)) download(name, file, status)
        status("Loading the $name speech model…", -1f)
        val ctx = jni.init(file.toPath()) ?: throw FetchException("The $name speech model couldn’t be loaded.")
        loaded = name to ctx
        return ctx
    }

    private fun download(name: String, file: File, status: (String, Float) -> Unit) {
        val size = models.firstOrNull { it.first == name }?.second.orEmpty()
        val message = "Downloading the $name speech model ($size). This happens once."
        status(message, 0f)
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        val request = HttpRequest.newBuilder(URI("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-$name.bin"))
            .header("User-Agent", "AfterhoursNex/1.0.0").build()
        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (e: Exception) {
            throw FetchException("The speech model couldn’t be downloaded. Check your internet connection, then try again.")
        }
        if (response.statusCode() != 200) throw FetchException("The speech model couldn’t be downloaded (error ${response.statusCode()}).")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
        val part = File(file.path + ".part")
        response.body().use { input ->
            part.outputStream().use { out ->
                val buf = ByteArray(1 shl 16)
                var done = 0L
                var reported = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (done - reported > (2 shl 20)) { reported = done; status(message, if (total > 0) done.toFloat() / total else -1f) }
                }
            }
        }
        if (total > 0 && part.length() != total) { part.delete(); throw FetchException("The speech model download was cut short. Try again.") }
        if (!part.renameTo(file)) throw FetchException("The speech model couldn’t be saved.")
    }

    // whisper-jni names its beam-search option differently between releases, so find it by name.
    private val BEAM: WhisperSamplingStrategy = enumValues<WhisperSamplingStrategy>()
        .firstOrNull { it.name.startsWith("BEAM") || it.name.startsWith("BEAN") } ?: WhisperSamplingStrategy.GREEDY

    private fun params(o: AsrOptions): WhisperFullParams {
        val p = WhisperFullParams(if (o.beam > 1) BEAM else WhisperSamplingStrategy.GREEDY)
        p.nThreads = if (o.threads > 0) o.threads else (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 8)
        p.language = o.language
        p.translate = o.translate
        p.temperature = o.temperature
        p.noSpeechThold = o.noSpeech
        p.initialPrompt = o.prompt.ifBlank { null }
        // These options are named differently (or missing) in some whisper-jni releases, so they're set by name.
        p.option(o.maxChars, "maxLen", "maxLength", "max_len")
        p.option(true, "splitOnWord", "split_on_word")
        p.option(true, "tokenTimestamps", "token_timestamps")
        p.option(true, "suppressBlank", "suppress_blank")
        p.option(false, "printProgress", "print_progress")
        p.option(false, "printRealtime", "print_realtime")
        p.option(false, "printTimestamps", "print_timestamps")
        if (o.beam > 1) p.option(o.beam, "beamSearchBeamSize", "beamSize", "beam_size")
        return p
    }

    /** Sets a whisper.cpp option if this whisper-jni release has a field by one of these names. */
    private fun WhisperFullParams.option(value: Any, vararg names: String) {
        for (name in names) {
            val field = runCatching { javaClass.getField(name) }.getOrNull() ?: continue
            if (runCatching { field.set(this, value) }.isSuccess) return
        }
    }
}
