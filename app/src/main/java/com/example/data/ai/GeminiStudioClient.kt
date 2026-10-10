package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// --- Gemini REST API Serialization Models ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String
)

interface GeminiRestService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): JsonObject
}

data class GroundedSearchResponse(
    val answerText: String,
    val citations: List<Pair<String, String>>
)

data class GeneratedImageOutput(
    val bitmap: Bitmap?,
    val captionText: String,
    val errorMessage: String? = null
)

data class GeneratedMusicOutput(
    val modelUsed: String,
    val statusText: String,
    val audioBytes: ByteArray? = null,
    val errorMessage: String? = null
)

object GeminiStudioClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Model Constants strictly adhering to user requirements & gemini-api skill
    const val MODEL_FAST_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_GENERAL_FLASH = "gemini-3.5-flash"
    const val MODEL_PRO_HIGH_THINKING = "gemini-3.1-pro-preview"
    const val MODEL_PRO_IMAGE = "gemini-3-pro-image-preview"
    const val MODEL_LYRIA_CLIP = "lyria-3-clip-preview"
    const val MODEL_LYRIA_PRO = "lyria-3-pro-preview"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val service: GeminiRestService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestService::class.java)
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * 1. Low-Latency Fast Response (gemini-3.1-flash-lite-preview)
     * Used for instant exercise hints, rubber-duck nudges, and rapid syntax checks.
     */
    suspend fun generateFastLowLatencyResponse(
        prompt: String,
        tutorMode: String = "Hint-only"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext "Configure GEMINI_API_KEY in the AI Studio Secrets panel to enable live low-latency ($MODEL_FAST_LITE) hints."
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are the Anvil v5.0 Tutor operating in '$tutorMode' mode. Speak like a calm, direct, kind teaching assistant. Give concise, actionable guidance in 2-3 short sentences without patronizing language."
                    )
                )
            )
        )
        try {
            val raw = service.generateContent(MODEL_FAST_LITE, apiKey, request)
            extractFirstText(raw) ?: "No response text returned."
        } catch (e: Exception) {
            "Low-latency AI request failed: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * 2. General Socratic Tutor & Code Review (gemini-3.5-flash)
     */
    suspend fun generateGeneralTutorResponse(
        prompt: String,
        tutorMode: String = "Socratic"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext "Configure GEMINI_API_KEY in the AI Studio Secrets panel to use the $MODEL_GENERAL_FLASH Socratic Tutor."
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.6f),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are Anvil's AI Tutor in '$tutorMode' mode. Guide the learner to discover the solution, explain underlying computer science & scientific principles, and never act as a ghostwriter."
                    )
                )
            )
        )
        try {
            val raw = service.generateContent(MODEL_GENERAL_FLASH, apiKey, request)
            extractFirstText(raw) ?: "No response text returned."
        } catch (e: Exception) {
            "Tutor request failed: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * 3. High-Thinking Complex Reasoning Mode (gemini-3.1-pro-preview with ThinkingLevel.HIGH)
     * Note: Do NOT set maxOutputTokens per strict requirement.
     */
    suspend fun generateHighThinkingResponse(
        complexQuery: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext "Configure GEMINI_API_KEY in the AI Studio Secrets panel to unlock Deep High-Thinking ($MODEL_PRO_HIGH_THINKING, thinkingLevel = HIGH)."
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = complexQuery)))),
            generationConfig = GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are Anvil's Principal Systems & Scientific Computing Architect. Provide rigorous step-by-step formal reasoning, asymptotic complexity proofs, numerical stability analysis, and idiomatic code patterns."
                    )
                )
            )
        )
        try {
            val raw = service.generateContent(MODEL_PRO_HIGH_THINKING, apiKey, request)
            extractFirstText(raw) ?: "No response text returned."
        } catch (e: Exception) {
            "High-Thinking ($MODEL_PRO_HIGH_THINKING) error: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * 4. Google Search Grounding (gemini-3.5-flash with googleSearch tool)
     * Used for Anvil's "Is this still relevant?" version checks & Science/CS live research.
     */
    suspend fun searchGroundedQuery(query: String): GroundedSearchResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext GroundedSearchResponse(
                answerText = "Configure GEMINI_API_KEY in the AI Studio Secrets panel to query live Google Search grounding ($MODEL_GENERAL_FLASH + googleSearch tool).",
                citations = emptyList()
            )
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = query)))),
            tools = listOf(
                buildJsonObject {
                    putJsonObject("googleSearch") {}
                }
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are Anvil's Living Content & Science Research engine. Summarize current programming language release status, API relevance, and scientific computing developments accurately using Google Search."
                    )
                )
            )
        )
        try {
            val raw = service.generateContent(MODEL_GENERAL_FLASH, apiKey, request)
            val answer = extractFirstText(raw) ?: "No grounded summary returned."
            val citations = extractGroundingCitations(raw)
            GroundedSearchResponse(answerText = answer, citations = citations)
        } catch (e: Exception) {
            GroundedSearchResponse(
                answerText = "Search grounding error: ${e.localizedMessage ?: e.message}",
                citations = emptyList()
            )
        }
    }

    /**
     * 5. High-Quality Image Generation (gemini-3-pro-image-preview with 1K, 2K, 4K selector)
     */
    suspend fun generateHighQualityImage(
        prompt: String,
        imageSize: String = "1K", // "1K", "2K", or "4K"
        aspectRatio: String = "1:1"
    ): GeneratedImageOutput = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val normalizedSize = if (imageSize in listOf("1K", "2K", "4K")) imageSize else "1K"
        if (!isApiKeyConfigured()) {
            return@withContext GeneratedImageOutput(
                bitmap = null,
                captionText = "",
                errorMessage = "Configure GEMINI_API_KEY in the AI Studio Secrets panel to generate $normalizedSize images with $MODEL_PRO_IMAGE."
            )
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = normalizedSize),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        try {
            val raw = service.generateContent(MODEL_PRO_IMAGE, apiKey, request)
            val parts = raw["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray

            var decodedBitmap: Bitmap? = null
            val textParts = mutableListOf<String>()

            parts?.forEach { element ->
                val partObj = element.jsonObject
                partObj["text"]?.jsonPrimitive?.content?.let { textParts.add(it) }
                val inlineData = partObj["inlineData"]?.jsonObject
                val b64 = inlineData?.get("data")?.jsonPrimitive?.content
                if (!b64.isNullOrBlank() && decodedBitmap == null) {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }

            if (decodedBitmap != null) {
                GeneratedImageOutput(
                    bitmap = decodedBitmap,
                    captionText = textParts.joinToString("\n").ifBlank {
                        "Generated $normalizedSize diagram via $MODEL_PRO_IMAGE."
                    }
                )
            } else {
                GeneratedImageOutput(
                    bitmap = null,
                    captionText = textParts.joinToString("\n"),
                    errorMessage = "Model returned no inline image data."
                )
            }
        } catch (e: Exception) {
            GeneratedImageOutput(
                bitmap = null,
                captionText = "",
                errorMessage = "Image generation ($MODEL_PRO_IMAGE @ $normalizedSize) failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * 6. Music Generation (lyria-3-clip-preview for <= 30s clips, lyria-3-pro-preview for full tracks)
     */
    suspend fun generateFocusMusic(
        prompt: String,
        useFullLengthTrack: Boolean = false
    ): GeneratedMusicOutput = withContext(Dispatchers.IO) {
        val model = if (useFullLengthTrack) MODEL_LYRIA_PRO else MODEL_LYRIA_CLIP
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext GeneratedMusicOutput(
                modelUsed = model,
                statusText = "Synthesizing local ambient focus harmonic wave (configure GEMINI_API_KEY in AI Studio Secrets for cloud $model generation).",
                audioBytes = null,
                errorMessage = null
            )
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO")
            )
        )
        try {
            val raw = service.generateContent(model, apiKey, request)
            val parts = raw["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray

            var audioBytes: ByteArray? = null
            parts?.forEach { element ->
                val inlineData = element.jsonObject["inlineData"]?.jsonObject
                val b64 = inlineData?.get("data")?.jsonPrimitive?.content
                if (!b64.isNullOrBlank() && audioBytes == null) {
                    audioBytes = Base64.decode(b64, Base64.DEFAULT)
                }
            }

            if (audioBytes != null) {
                GeneratedMusicOutput(
                    modelUsed = model,
                    statusText = "Generated ${if (useFullLengthTrack) "Full-Length Track" else "30s Focus Clip"} with $model (${audioBytes!!.size / 1024} KB).",
                    audioBytes = audioBytes
                )
            } else {
                GeneratedMusicOutput(
                    modelUsed = model,
                    statusText = "Completed $model request; playing synthesized Anvil Focus Harmonic fallback.",
                    audioBytes = null
                )
            }
        } catch (e: Exception) {
            GeneratedMusicOutput(
                modelUsed = model,
                statusText = "Playing synthesized Anvil Focus Harmonic ($model status: ${e.localizedMessage ?: "ready"}).",
                audioBytes = null,
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun extractFirstText(root: JsonObject): String? {
        val parts = root["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray ?: return null
        val texts = parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
        return texts.joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun extractGroundingCitations(root: JsonObject): List<Pair<String, String>> {
        val citations = mutableListOf<Pair<String, String>>()
        try {
            val metadata = root["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("groundingMetadata")?.jsonObject ?: return emptyList()
            val chunks = metadata["groundingChunks"]?.jsonArray ?: return emptyList()
            chunks.forEach { chunk ->
                val web = chunk.jsonObject["web"]?.jsonObject
                val title = web?.get("title")?.jsonPrimitive?.content
                val uri = web?.get("uri")?.jsonPrimitive?.content
                if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                    citations.add(title to uri)
                }
            }
        } catch (_: Exception) {
        }
        return citations.distinctBy { it.second }.take(5)
    }
}

/**
 * Anvil Part 7 Procedural Sound & Focus Soundscape Synthesizer
 * Synthesizes crisp anvil strikes, XP chimes, and ambient focus harmonics at runtime with zero bundled audio files.
 */
object AnvilAudioSynthesizer {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudioBytesOrFallback(context: Context, audioBytes: ByteArray?, durationSeconds: Int = 4) {
        if (audioBytes != null && audioBytes.isNotEmpty()) {
            try {
                mediaPlayer?.release()
                val tempFile = File(context.cacheDir, "lyria_track.wav")
                FileOutputStream(tempFile).use { it.write(audioBytes) }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                }
                return
            } catch (_: Exception) {
                // Fall back to procedural harmonic synthesis if raw PCM/container differs
            }
        }
        playProceduralFocusChord(durationSeconds)
    }

    fun playAnvilStrike() {
        Thread {
            playToneSequence(listOf(587.33 to 70, 880.0 to 140), decayRate = 12.0)
        }.start()
    }

    fun playProceduralFocusChord(durationSeconds: Int = 3) {
        Thread {
            try {
                val sampleRate = 22050
                val totalSamples = sampleRate * durationSeconds.coerceIn(1, 8)
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = sin(PI * t / durationSeconds).coerceIn(0.0, 1.0)
                    val wave = 0.40 * sin(2.0 * PI * 220.0 * t) +
                        0.30 * sin(2.0 * PI * 277.18 * t) +
                        0.30 * sin(2.0 * PI * 329.63 * t)
                    buffer[i] = (wave * env * 12000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playPcmBuffer(buffer, sampleRate)
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun playToneSequence(notes: List<Pair<Double, Int>>, decayRate: Double = 8.0) {
        try {
            val sampleRate = 22050
            val totalMs = notes.sumOf { it.second }
            val totalSamples = (sampleRate * totalMs) / 1000
            val buffer = ShortArray(totalSamples)
            var offset = 0
            for ((freq, ms) in notes) {
                val count = (sampleRate * ms) / 1000
                for (i in 0 until count) {
                    if (offset + i >= totalSamples) break
                    val t = i.toDouble() / sampleRate
                    val env = exp(-decayRate * t)
                    val sample = sin(2.0 * PI * freq * t) * env
                    buffer[offset + i] = (sample * 14000).toInt().toShort()
                }
                offset += count
            }
            playPcmBuffer(buffer, sampleRate)
        } catch (_: Exception) {
        }
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        val byteSize = buffer.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(byteSize.coerceAtLeast(2048))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
    }
}
