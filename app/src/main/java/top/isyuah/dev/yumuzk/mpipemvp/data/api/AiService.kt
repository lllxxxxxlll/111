package top.isyuah.dev.yumuzk.mpipemvp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
data class ChatResponse(
    val session_id: String,
    val sent: Boolean,
    val text: String,
    val history_len: Int
)

class AiService(
    private val httpClient: HttpClient,
    private val apiBaseUrl: String = "http://10.0.2.2:8000"
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun chatStream(
        sessionId: String? = null,
        prompt: String? = null,
        message: String? = null
    ): Flow<StreamEvent> = flow {
        val body = JSONObject().apply {
            sessionId?.let { put("session_id", it) }
            prompt?.let { put("prompt", it) }
            message?.let { put("message", it) }
        }

        httpClient.preparePost("$apiBaseUrl/ai/chat/stream") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.execute { response ->
            val channel: ByteReadChannel = response.body()
            var currentEvent = ""
            
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim()
                } else if (line.startsWith("data:")) {
                    val data = line.substring(5).trim()
                    if (data.isNotEmpty()) {
                        when (currentEvent) {
                            "delta" -> {
                                try {
                                    val jsonDelta = JSONObject(data)
                                    if (jsonDelta.has("delta")) {
                                        emit(StreamEvent.Delta(jsonDelta.getString("delta")))
                                    }
                                } catch (e: Exception) {
                                    emit(StreamEvent.Delta(data))
                                }
                            }
                            "done" -> {
                                try {
                                    val res = json.decodeFromString<ChatResponse>(data)
                                    emit(StreamEvent.Done(res))
                                } catch (e: Exception) { }
                            }
                            "error" -> emit(StreamEvent.Error(data))
                        }
                    }
                }
            }
        }
    }

    suspend fun chat(
        sessionId: String? = null,
        prompt: String? = null,
        message: String? = null
    ): ChatResponse {
        val body = JSONObject().apply {
            sessionId?.let { put("session_id", it) }
            prompt?.let { put("prompt", it) }
            message?.let { put("message", it) }
        }
        val response = httpClient.preparePost("$apiBaseUrl/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.execute { it.bodyAsText() }
        return json.decodeFromString(response)
    }

    suspend fun analyzePointedObject(imageDataUrl: String): String {
        val body = JSONObject().apply { put("image", imageDataUrl) }
        val response = httpClient.preparePost("$apiBaseUrl/ai/recognize") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.execute { it.bodyAsText() }
        val jsonRes = JSONObject(response)
        return jsonRes.getString("result")
    }
}

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Done(val response: ChatResponse) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
