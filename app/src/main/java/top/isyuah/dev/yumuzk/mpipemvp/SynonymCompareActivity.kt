package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bumptech.glide.Glide
import android.widget.ImageView
import android.widget.ProgressBar
import android.util.Log
import com.google.android.material.card.MaterialCardView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SynonymCompareActivity : AppCompatActivity() {

    // 用户提供的 Key（按要求放在代码中）
    private val OPENAI_API_KEY = "sk-W4ucREHoMBC46eta7jWum4UZGePjwASoIeXTKUREOBznVasX"
    // 使用 Moonshot / Kimi 的 API 地址，避免默认调用 api.openai.com 导致网络超时
    private val OPENAI_ENDPOINT = "https://api.moonshot.cn/v1/chat/completions"
    // 推荐使用 Kimi 的 preview 模型名称
    private val MODEL = "kimi-k2-turbo-preview"

    private lateinit var tvResult: TextView
    private lateinit var progress: ProgressBar
    private lateinit var ivGenerated: ImageView
    private lateinit var resultCard: MaterialCardView
    private lateinit var btnAddVocab: MaterialButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // 保存最近展示的条目，供“加入生词本”使用
    private var lastW1: String = ""
    private var lastW2: String = ""
    private var lastExplanation: String = ""
    private var lastImageLocalPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_synonym_compare)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }
        // inflate toolbar menu and handle drawer open
        toolbar.inflateMenu(R.menu.toolbar_menu)

        val etWord1 = findViewById<TextInputEditText>(R.id.et_word1)
        val etWord2 = findViewById<TextInputEditText>(R.id.et_word2)
        val btnCompare = findViewById<Button>(R.id.btn_compare)
        tvResult = findViewById<TextView>(R.id.tv_result)
        progress = findViewById<ProgressBar>(R.id.progress_loading)
        ivGenerated = findViewById(R.id.iv_generated)
        resultCard = findViewById(R.id.card_result)
        btnAddVocab = findViewById(R.id.btn_add_vocab)
        btnAddVocab.visibility = View.GONE
        btnAddVocab.setOnClickListener {
            // 将当前展示的单词加入生词本
            if (lastW1.isNotEmpty() || lastW2.isNotEmpty()) {
                addToVocab(lastW1, lastW2)
                Toast.makeText(this, "已加入生词本", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "当前无可加入的单词", Toast.LENGTH_SHORT).show()
            }
        }
        // Drawer and navigation
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        // toolbar menu click to open drawer
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_open_drawer) {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            } else false
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> {
                    startActivity(android.content.Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_vocab -> {
                    startActivity(android.content.Intent(this, VocabActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnCompare.setOnClickListener {
            val w1 = etWord1.text?.toString()?.trim().orEmpty()
            val w2 = etWord2.text?.toString()?.trim().orEmpty()
            if (w1.isEmpty() && w2.isEmpty()) {
                tvResult.text = "请输入至少一个单词。"
                return@setOnClickListener
            }

            // 开始新请求：隐藏上次结果并显示加载动画
            try { resultCard.visibility = View.GONE } catch (_: Exception) {}
            tvResult.text = ""
            ivGenerated.visibility = View.GONE
            try { ivGenerated.setImageDrawable(null) } catch (_: Exception) {}
            progress.visibility = View.VISIBLE
            btnAddVocab.visibility = View.GONE
            // 清除上次缓存的展示数据
            lastW1 = ""; lastW2 = ""; lastExplanation = ""; lastImageLocalPath = null

            // 确保进度在最上层
            try { progress.bringToFront(); progress.elevation = 20f } catch (_: Exception) {}

            lifecycleScope.launch {
                var finalImageLocalPath: String? = null
                val results = withContext(Dispatchers.IO) {
                    if (w1.isNotEmpty() && w2.isNotEmpty()) {
                        // 获取对比文本
                        val cmp = callOpenAiCompare(w1, w2)
                        // 第一次生成初始图片（远端 URL）
                        val initialImageUrl = if (cmp != null) callAliGenerateInitial(w1, w2, cmp) else null
                        // 下载并保存到本地
                        val localPath = if (initialImageUrl != null) downloadImageToLocal(initialImageUrl) else null
                        finalImageLocalPath = localPath
                        Pair(cmp, localPath)
                    } else {
                        val r1 = if (w1.isNotEmpty()) callOpenAiExplain(w1) else null
                        val r2 = if (w2.isNotEmpty()) callOpenAiExplain(w2) else null
                        Pair(r1, r2)
                    }
                }

                // 展示逻辑：如果是双词对比，等文本与最终图片准备好后一起显示
                if (w1.isNotEmpty() && w2.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        progress.visibility = View.GONE
                        // 统一显示结果卡片（文本与/或图片）
                        resultCard.visibility = View.VISIBLE
                        if (results.first != null && finalImageLocalPath != null) {
                            // 处理文本以突出符号
                            val processedText = results.first?.replace("\n- ", "\n• ")?.replace("\n1. ", "\n1. ")?.replace("\n2. ", "\n2. ")?.replace("\n3. ", "\n3. ") ?: results.first
                            tvResult.text = processedText
                            ivGenerated.visibility = View.VISIBLE
                            // Glide 支持 file:// 路径
                            loadImageIntoView(ivGenerated, "file://" + finalImageLocalPath, progress, tvResult)

                            // 保存历史（包含本地图片路径）
                            saveHistoryEntry(w1, w2, processedText ?: "", finalImageLocalPath)

                            // 更新最近展示，用于加入生词本
                            lastW1 = w1; lastW2 = w2; lastExplanation = processedText ?: ""; lastImageLocalPath = finalImageLocalPath
                            btnAddVocab.visibility = View.VISIBLE
                        } else if (results.first != null) {
                            // 有文本但未生成或下载图片
                            tvResult.text = results.first + "\n\n（图片生成或下载失败，请检查返回信息）"
                            saveHistoryEntry(w1, w2, results.first ?: "", null)
                            lastW1 = w1; lastW2 = w2; lastExplanation = results.first ?: ""; lastImageLocalPath = null
                            btnAddVocab.visibility = View.VISIBLE
                        } else {
                            tvResult.text = "未获取到解释或图片，请稍后重试。"
                        }
                    }
                } else {
                    // 单词单独解释的展示
                    val sb = StringBuilder()
                    if (results.first != null) {
                        sb.append("单词 1: ").append(w1).append("\n")
                        sb.append(results.first).append("\n\n")
                    }
                    if (results.second != null) {
                        sb.append("单词 2: ").append(w2).append("\n")
                        sb.append(results.second).append("\n\n")
                    }
                    if (sb.isEmpty()) sb.append("未获取到解释，请稍后重试。")
                    withContext(Dispatchers.Main) {
                        progress.visibility = View.GONE
                        tvResult.text = sb.toString()
                    }
                }
            }
        }
    }

    // 阿里第一次生成（初始）—— 返回图片 URL（首个 choice 的第一个 content.image）
    private fun callAliGenerateInitial(word1: String, word2: String, compareText: String?): String? {
        try {
            val url = URL("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer sk-69a5bf4044b6494f829cca56f20b6008")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            val promptText = StringBuilder()
            promptText.append("请生成一张横向并排的图像，左侧表现单词 '${word1}' 的典型意象，右侧表现单词 '${word2}' 的典型意象；画面应清晰分为左右两部分，左/右两侧风格一致，整体尺寸 1440x500，画风写实且干净，便于对比。图像中不要出现任何文字，除了单词 '${word1}' 和 '${word2}' 本身。")
            compareText?.let { promptText.append(" 可参考差别描述：").append(it) }

            val payload = JSONObject()
            payload.put("model", "wan2.6-t2i")
            val input = JSONObject()
            val messages = org.json.JSONArray()
            val msg = JSONObject()
            msg.put("role", "user")
            val contentArr = org.json.JSONArray()
            val textObj = JSONObject()
            textObj.put("text", promptText.toString())
            contentArr.put(textObj)
            msg.put("content", contentArr)
            messages.put(msg)
            input.put("messages", messages)
            payload.put("input", JSONObject().put("messages", messages))

            val params = JSONObject()
            params.put("negative_prompt", "")
            params.put("prompt_extend", true)
            params.put("watermark", false)
            params.put("n", 1)
            params.put("size", "1440*500")
            payload.put("parameters", params)

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val inputS = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(inputS))
            val respText = reader.readText()
            reader.close()

            if (code !in 200..299) {
                // show raw response for debugging
                try {
                    runOnUiThread { tvResult.text = "阿里 API 返回错误（code $code）：$respText" }
                } catch (t: Exception) { Log.d("SynonymCompare", "ui set error", t) }
                return null
            }

            val respJson = JSONObject(respText)
            val out = respJson.optJSONObject("output")
            val choices = out?.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                val content = message?.optJSONArray("content")
                if (content != null && content.length() > 0) {
                    val imgObj = content.getJSONObject(0)
                    return imgObj.optString("image")
                } else {
                    try {
                        runOnUiThread { tvResult.text = "未在返回中找到 image，响应：$respText" }
                    } catch (t: Exception) { Log.d("SynonymCompare", "ui set missing image", t) }
                }
            }
            return null
        } catch (e: Exception) {
            try { runOnUiThread { tvResult.text = "阿里 API 调用异常：${e.message}" } } catch (_: Exception) {}
            return null
        }
    }

    // 阿里第二次精化调用 — 把初始图片 URL 纳入提示词，返回最终图片 URL
    private fun callAliGenerateRefine(word1: String, word2: String, referenceImageUrl: String, compareText: String?): String? {
        try {
            val url = URL("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer sk-69a5bf4044b6494f829cca56f20b6008")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            val prompt = StringBuilder()
            prompt.append("基于已生成的图片：").append(referenceImageUrl).append("，请生成一张更强调左右对比的图片，左侧展示 '${word1}' 的意象，右侧展示 '${word2}' 的意象，保持 1440x500 的尺寸，色调与构图更利于对比，去除不必要的复杂背景。")
            compareText?.let { prompt.append(" 可参考差别描述：").append(it) }

            val payload = JSONObject()
            payload.put("model", "wan2.6-t2i")
            val messages = org.json.JSONArray()
            val msg = JSONObject()
            msg.put("role", "user")
            val contentArr = org.json.JSONArray()
            val textObj = JSONObject()
            textObj.put("text", prompt.toString())
            contentArr.put(textObj)
            msg.put("content", contentArr)
            messages.put(msg)
            payload.put("input", JSONObject().put("messages", messages))

            val params = JSONObject()
            params.put("negative_prompt", "")
            params.put("prompt_extend", true)
            params.put("watermark", false)
            params.put("n", 1)
            params.put("size", "1440*500")
            payload.put("parameters", params)

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val inputS = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(inputS))
            val respText = reader.readText()
            reader.close()

            if (code !in 200..299) {
                try { runOnUiThread { tvResult.text = "阿里 API 返回错误（code $code）：$respText" } } catch (_: Exception) {}
                return null
            }

            val respJson = JSONObject(respText)
            val out = respJson.optJSONObject("output")
            val choices = out?.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                val content = message?.optJSONArray("content")
                if (content != null && content.length() > 0) {
                    val imgObj = content.getJSONObject(0)
                    return imgObj.optString("image")
                } else {
                    try { runOnUiThread { tvResult.text = "未在返回中找到 image，响应：$respText" } } catch (_: Exception) {}
                }
            }
            return null
        } catch (e: Exception) {
            try { runOnUiThread { tvResult.text = "阿里 API 调用异常：${e.message}" } } catch (_: Exception) {}
            return null
        }
    }

    private fun loadImageIntoView(iv: ImageView, imageUrl: String, progress: android.widget.ProgressBar, tvResult: TextView) {
        try {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .error(android.R.drawable.stat_notify_error)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        tvResult.text = "图片加载失败"
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        return false
                    }
                })
                .into(iv)
        } catch (e: Exception) {
            progress.visibility = View.GONE
            tvResult.text = "图片加载异常：${e.message}"
        }
    }

    // 下载远程图片并保存到应用私有文件夹，返回本地路径（绝对路径）
    private fun downloadImageToLocal(imageUrl: String): String? {
        try {
            val imagesDir = File(filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            // 生成文件名
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
            val name = "img_" + sdf.format(Date()) + ".jpg"
            val outFile = File(imagesDir, name)

            val url = URL(imageUrl)
            BufferedInputStream(url.openStream()).use { input ->
                BufferedOutputStream(FileOutputStream(outFile)).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            return outFile.absolutePath
        } catch (e: Exception) {
            Log.d("SynonymCompare", "download image failed: ${e.message}")
            return null
        }
    }

    // 保存历史记录（最近 100 条）
    private fun saveHistoryEntry(word1: String, word2: String, explanation: String, localImagePath: String?) {
        try {
            val filename = "history.json"
            val arr = readJsonArrayFromFile(filename)

            val obj = JSONObject()
            obj.put("word1", word1)
            obj.put("word2", word2)
            obj.put("explanation", explanation)
            obj.put("imagePath", localImagePath ?: JSONObject.NULL)
            obj.put("ts", System.currentTimeMillis())

            // 新条目放到最前
            val newArr = JSONArray()
            newArr.put(obj)
            for (i in 0 until arr.length()) {
                if (newArr.length() >= 100) break
                newArr.put(arr.get(i))
            }

            writeJsonArrayToFile(filename, newArr)
        } catch (e: Exception) {
            Log.d("SynonymCompare", "save history failed: ${e.message}")
        }
    }

    // 将单词加入生词本（永久保存）
    private fun addToVocab(w1: String, w2: String) {
        try {
            val filename = "vocab.json"
            val arr = readJsonArrayFromFile(filename)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.optString(i))
            }
            if (w1.isNotEmpty()) set.add(w1)
            if (w2.isNotEmpty()) set.add(w2)

            val newArr = JSONArray()
            for (s in set) newArr.put(s)
            writeJsonArrayToFile(filename, newArr)
        } catch (e: Exception) {
            Log.d("SynonymCompare", "add vocab failed: ${e.message}")
        }
    }

    private fun readJsonArrayFromFile(filename: String): JSONArray {
        return try {
            val fis = openFileInput(filename)
            val text = fis.bufferedReader().use { it.readText() }
            JSONArray(text)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun writeJsonArrayToFile(filename: String, arr: JSONArray) {
        try {
            openFileOutput(filename, MODE_PRIVATE).use { fos ->
                fos.write(arr.toString().toByteArray())
            }
        } catch (e: IOException) {
            Log.d("SynonymCompare", "write file failed: ${e.message}")
        }
    }

    // 显示历史记录的简单对话框
    private fun showHistoryDialog() {
        val arr = readJsonArrayFromFile("history.json")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val w1 = obj.optString("word1")
            val w2 = obj.optString("word2")
            val expl = obj.optString("explanation")
            val img = if (obj.has("imagePath") && !obj.isNull("imagePath")) obj.optString("imagePath") else ""
            val ts = obj.optLong("ts", 0L)
            val date = if (ts > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts)) else ""
            sb.append(date).append("\n")
            sb.append(w1).append(" | ").append(w2).append("\n")
            sb.append(expl).append("\n")
            if (img.isNotEmpty()) sb.append("图片: ").append(img).append("\n")
            sb.append("\n")
        }
        if (sb.isEmpty()) sb.append("无历史记录")
        AlertDialog.Builder(this)
            .setTitle("历史记录")
            .setMessage(sb.toString())
            .setPositiveButton("关闭", null)
            .show()
    }

    // 显示生词本的简单对话框
    private fun showVocabDialog() {
        val arr = readJsonArrayFromFile("vocab.json")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            sb.append(arr.optString(i)).append("\n")
        }
        if (sb.isEmpty()) sb.append("生词本为空")
        AlertDialog.Builder(this)
            .setTitle("生词本")
            .setMessage(sb.toString())
            .setPositiveButton("关闭", null)
            .show()
    }

    // 当同时输入两个单词时，调用此方法请求模型重点比较差别
    private fun callOpenAiCompare(word1: String, word2: String): String? {
        try {
            val url = URL(OPENAI_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $OPENAI_API_KEY")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            // 更简洁的提示词，要求回答简明扼要并给出图像提示
            val systemMsg = "你是语言对比专家，回答中文且尽量简洁。按：词义（1-2 行）、主要差别（最多 3 条）、例句（每词 1 条并翻译）、图像提示（左|右，1-2 关键词）。使用 IPA 音标。"
            val userPrompt = "请简洁比较 '$word1' 与 '$word2' 的差别，按 system 要求输出。"

            val payload = JSONObject()
            payload.put("model", MODEL)
            val messages = org.json.JSONArray()

            val sys = JSONObject()
            sys.put("role", "system")
            sys.put("content", systemMsg)
            messages.put(sys)

            val msg = JSONObject()
            msg.put("role", "user")
            msg.put("content", userPrompt)
            messages.put(msg)

            payload.put("messages", messages)
            payload.put("max_tokens", 600)

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(input))
            val respText = reader.readText()
            reader.close()

            if (code !in 200..299) {
                return "请求失败（HTTP $code）：$respText"
            }

            val respJson = JSONObject(respText)
            val choices = respJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                val content = message?.optString("content")
                return content ?: respText
            }

            return respText
        } catch (e: Exception) {
            return "调用 AI 出错：${e.message}"
        }
    }

    private fun callOpenAiExplain(word: String): String? {
        try {
            val url = URL(OPENAI_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $OPENAI_API_KEY")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            val userPrompt = "请用简短清晰的中文解释单词 '$word' 的含义，给出常见同义词，并提供一个英文例句和中文翻译。"

            val payload = JSONObject()
            payload.put("model", MODEL)
            val messages = org.json.JSONArray()
            val msg = JSONObject()
            msg.put("role", "user")
            msg.put("content", userPrompt)
            messages.put(msg)
            payload.put("messages", messages)
            payload.put("max_tokens", 400)

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(input))
            val respText = reader.readText()
            reader.close()

            if (code !in 200..299) {
                return "请求失败（HTTP $code）：$respText"
            }

            val respJson = JSONObject(respText)
            val choices = respJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                val content = message?.optString("content")
                return content ?: respText
            }

            return respText
        } catch (e: Exception) {
            return "思考失败：${e.message}"
        }
    }
}
