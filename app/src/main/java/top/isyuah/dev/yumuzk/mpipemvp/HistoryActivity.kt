package top.isyuah.dev.yumuzk.mpipemvp

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONObject
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import java.io.FileInputStream

class HistoryActivity : AppCompatActivity() {

    private val REQUEST_WRITE_STORAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val lv = findViewById<ListView>(R.id.lv_history)
        val arr = readJsonArrayFromFile("history.json")
        val titles = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val w1 = obj.optString("word1")
            val w2 = obj.optString("word2")
            val ts = obj.optLong("ts", 0L)
            val date = if (ts > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts)) else ""
            titles.add("$date  $w1 | $w2")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        lv.adapter = adapter

        lv.setOnItemClickListener { _, _, position, _ ->
            val obj = arr.optJSONObject(position) ?: return@setOnItemClickListener
            showDetailDialog(obj)
        }
    }

    private fun showDetailDialog(obj: JSONObject) {
        val imagePath = if (obj.has("imagePath") && !obj.isNull("imagePath")) obj.optString("imagePath") else null
        val expl = obj.optString("explanation")
        val w1 = obj.optString("word1")
        val w2 = obj.optString("word2")

        val linear = android.widget.LinearLayout(this)
        linear.orientation = android.widget.LinearLayout.VERTICAL

        val iv = ImageView(this)
        iv.adjustViewBounds = true
        if (imagePath != null && File(imagePath).exists()) {
            Glide.with(this).load("file://" + imagePath).into(iv)
            linear.addView(iv)
        }

        val tv = TextView(this)
        tv.text = "$w1 | $w2\n\n$expl"
        linear.addView(tv)

        val dlgBuilder = AlertDialog.Builder(this)
            .setView(linear)
            .setPositiveButton("加入生词本") { _, _ ->
                addToVocab(w1, w2)
                Toast.makeText(this, "已加入生词本", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)

        if (imagePath != null && File(imagePath).exists()) {
            dlgBuilder.setNeutralButton("保存图片") { _, _ ->
                saveImageToGallery(imagePath)
            }
        }

        dlgBuilder.show()
    }

    private fun saveImageToGallery(imagePath: String) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
            return
        }
        try {
            val file = File(imagePath)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SynonymCompare")
            }
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { output ->
                    FileInputStream(file).use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存异常: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "权限已授予，请重新点击保存", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
        }
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
            for (s in set) newArr.put(s)
            writeJsonArrayToFile(filename, newArr)
        } catch (_: Exception) {}
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
        } catch (_: Exception) {}
    }
}
