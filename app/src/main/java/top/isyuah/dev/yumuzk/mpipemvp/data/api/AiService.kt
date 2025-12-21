package top.isyuah.dev.yumuzk.mpipemvp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject

data class AiConfig(
    val apiUrl: String,
    val model: String,
    val apiKey: String,
) {
    companion object {
        const val DEFAULT_API_URL = "https://api.siliconflow.cn/v1/chat/completions"
        const val DEFAULT_MODEL = "THUDM/GLM-4.1V-9B-Thinking"

        fun default(apiKey: String): AiConfig = AiConfig(
            apiUrl = DEFAULT_API_URL,
            model = DEFAULT_MODEL,
            apiKey = apiKey,
        )
    }
}

class AiService(
    private val httpClient: HttpClient,
    private val config: AiConfig,
) {
    suspend fun analyzePointedObject(
        imageDataUrl: String,
        prompt: String = DEFAULT_PROMPT,
    ): String {
        val requestBody = buildRequestBody(imageDataUrl = imageDataUrl, prompt = prompt)

        val response = httpClient.post(config.apiUrl) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val resBody = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw AiApiException("API 错误 (${response.status.value}): $resBody")
        }

        return parseResponse(resBody)
    }

    private fun buildRequestBody(imageDataUrl: String, prompt: String): String {
        val json = JSONObject().apply {
            put("model", config.model)

            val messages = JSONArray()
            val userMessage = JSONObject().apply { put("role", "user") }

            val content = JSONArray()
            val imageContent = JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply { put("url", imageDataUrl) })
            }
            content.put(imageContent)

            val textContent = JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            }
            content.put(textContent)

            userMessage.put("content", content)
            messages.put(userMessage)

            put("messages", messages)
        }

        return json.toString()
    }

    private fun parseResponse(resBody: String): String {
        try {
            val jsonRes = JSONObject(resBody)
            return jsonRes.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            throw AiApiException("解析失败: ${e.message}\n响应内容: $resBody", e)
        }
    }

    companion object {
        const val DEFAULT_PROMPT = "请分析图片中手指指向的物体是什么？"
    }
}

class AiApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
