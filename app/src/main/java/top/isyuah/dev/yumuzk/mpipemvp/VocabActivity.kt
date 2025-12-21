package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Toast

class VocabActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vocab)

        val lv = findViewById<ListView>(R.id.lv_vocab)
        val arr = readJsonArrayFromFile("vocab.json")
        val list = ArrayList<String>()
        for (i in 0 until arr.length()) list.add(arr.optString(i))
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        lv.adapter = adapter

        lv.setOnItemClickListener { _, _, position, _ ->
            val word = list[position]
            showWordDetails(word)
        }
    }

    private fun showWordDetails(word: String) {
        // show history entries that include this word
        val hist = readJsonArrayFromFile("history.json")
        val sb = StringBuilder()
        for (i in 0 until hist.length()) {
            val obj = hist.optJSONObject(i) ?: continue
            val w1 = obj.optString("word1")
            val w2 = obj.optString("word2")
            if (w1 == word || w2 == word) {
                sb.append(w1).append(" | ").append(w2).append("\n")
                sb.append(obj.optString("explanation")).append("\n\n")
            }
        }
        if (sb.isEmpty()) sb.append("无相关记录")
        AlertDialog.Builder(this)
            .setTitle(word)
            .setMessage(sb.toString())
            .setPositiveButton("关闭", null)
            .show()
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
}
