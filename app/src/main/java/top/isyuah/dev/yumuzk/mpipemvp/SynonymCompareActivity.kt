package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.URL
import com.bumptech.glide.Glide
import android.widget.ImageView
import android.widget.ProgressBar
import android.util.Log
import com.google.android.material.card.MaterialCardView
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SynonymCompareActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var progress: ProgressBar
    private lateinit var ivGenerated: ImageView
    private lateinit var resultCard: MaterialCardView
    private lateinit var btnAddVocab: MaterialButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var lastW1: String = ""
    private var lastW2: String = ""
    private var lastExplanation: String = ""
    private var lastImageLocalPath: String? = null

    private val httpClient = HttpClient(Android)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_synonym_compare)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }
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
            if (lastW1.isNotEmpty() || lastW2.isNotEmpty()) {
                addToVocab(lastW1, lastW2)
                Toast.makeText(this, "已加入生词本", Toast.LENGTH_SHORT).show()
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_open_drawer) {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            } else false
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> startActivity(android.content.Intent(this, HistoryActivity::class.java))
                R.id.nav_vocab -> startActivity(android.content.Intent(this, VocabActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnCompare.setOnClickListener {
            val w1 = etWord1.text?.toString()?.trim().orEmpty()
            val w2 = etWord2.text?.toString()?.trim().orEmpty()
            if (w1.isEmpty() && w2.isEmpty()) return@setOnClickListener

            resultCard.visibility = View.GONE
            tvResult.text = ""
            ivGenerated.visibility = View.GONE
            progress.visibility = View.VISIBLE
            btnAddVocab.visibility = View.GONE

            lifecycleScope.launch {
                try {
                    if (w1.isNotEmpty() && w2.isNotEmpty()) {
                        val response = httpClient.post("${NetworkConfig.BASE_URL}/ai/synonym/compare") {
                            contentType(ContentType.Application.Json)
                            setBody(JSONObject().apply {
                                put("text1", w1)
                                put("text2", w2)
                            }.toString())
                        }
                        val json = JSONObject(response.bodyAsText())
                        val explain = json.getString("explain")
                        val imageUrl = json.getString("image")

                        val localPath = withContext(Dispatchers.IO) { downloadImageToLocal(imageUrl) }
                        
                        tvResult.text = explain
                        if (localPath != null) {
                            ivGenerated.visibility = View.VISIBLE
                            Glide.with(this@SynonymCompareActivity).load("file://$localPath").into(ivGenerated)
                        }
                        
                        saveHistoryEntry(w1, w2, explain, localPath)
                        lastW1 = w1; lastW2 = w2; lastExplanation = explain; lastImageLocalPath = localPath
                    } else {
                        val word = if (w1.isNotEmpty()) w1 else w2
                        val response = httpClient.post("${NetworkConfig.BASE_URL}/ai/explain") {
                            contentType(ContentType.Application.Json)
                            setBody(JSONObject().apply { put("word", word) }.toString())
                        }
                        val json = JSONObject(response.bodyAsText())
                        val explain = json.getString("explain")
                        tvResult.text = explain
                        saveHistoryEntry(word, "", explain, null)
                        lastW1 = word; lastExplanation = explain
                    }
                    resultCard.visibility = View.VISIBLE
                    btnAddVocab.visibility = View.VISIBLE
                } catch (e: Exception) {
                    tvResult.text = "请求失败: ${e.message}"
                    resultCard.visibility = View.VISIBLE
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }
    }

    private fun downloadImageToLocal(imageUrl: String): String? {
        return try {
            val imagesDir = File(filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val name = "img_${System.currentTimeMillis()}.jpg"
            val outFile = File(imagesDir, name)
            val url = URL(imageUrl)
            BufferedInputStream(url.openStream()).use { input ->
                BufferedOutputStream(FileOutputStream(outFile)).use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun saveHistoryEntry(word1: String, word2: String, explanation: String, localImagePath: String?) {
        try {
            val filename = "history.json"
            val arr = readJsonArrayFromFile(filename)
            val obj = JSONObject().apply {
                put("word1", word1)
                put("word2", word2)
                put("explanation", explanation)
                put("imagePath", localImagePath ?: JSONObject.NULL)
                put("ts", System.currentTimeMillis())
            }
            val newArr = JSONArray().apply { put(obj) }
            for (i in 0 until arr.length()) {
                if (newArr.length() >= 100) break
                newArr.put(arr.get(i))
            }
            writeJsonArrayToFile(filename, newArr)
        } catch (e: Exception) {}
    }

    private fun addToVocab(w1: String, w2: String) {
        try {
            val filename = "vocab.json"
            val arr = readJsonArrayFromFile(filename)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) set.add(arr.optString(i))
            if (w1.isNotEmpty()) set.add(w1)
            if (w2.isNotEmpty()) set.add(w2)
            val newArr = JSONArray()
            set.forEach { newArr.put(it) }
            writeJsonArrayToFile(filename, newArr)
        } catch (e: Exception) {}
    }

    private fun readJsonArrayFromFile(filename: String): JSONArray {
        return try {
            openFileInput(filename).bufferedReader().use { JSONArray(it.readText()) }
        } catch (e: Exception) { JSONArray() }
    }

    private fun writeJsonArrayToFile(filename: String, arr: JSONArray) {
        try {
            openFileOutput(filename, MODE_PRIVATE).use { it.write(arr.toString().toByteArray()) }
        } catch (e: Exception) {}
    }
}
