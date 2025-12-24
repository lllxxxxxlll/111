package top.isyuah.dev.yumuzk.mpipemvp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONObject

class AiService(
    private val httpClient: HttpClient,
    private val apiBaseUrl: String = "http://10.0.2.2:8000" // 替换为你的服务器地址
) {
    suspend fun analyzePointedObject(
        imageDataUrl: String
    ): String {
        val body = JSONObject().apply {
            put("image", imageDataUrl)
        }

        val response = httpClient.post("$apiBaseUrl/ai/recognize") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }

        val resBody = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw AiApiException("API 错误 (${response.status.value}): $resBody")
        }

        return try {
            val jsonRes = JSONObject(resBody)
            jsonRes.getString("result")
        } catch (e: Exception) {
            throw AiApiException("解析失败: ${e.message}\n响应内容: $resBody", e)
        }
    }
}

class AiApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
