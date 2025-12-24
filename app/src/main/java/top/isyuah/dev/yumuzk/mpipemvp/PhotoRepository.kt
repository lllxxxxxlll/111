package top.isyuah.dev.yumuzk.mpipemvp

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.util.Calendar

class PhotoRepository private constructor(private val context: Context) {
    private val prefsName = "photo_repo_prefs"
    private val key = "photos"
    private val questionCountKey = "question_count"
    private val firstUseDateKey = "first_use_date"

    private val prefs by lazy { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }

    companion object {
        @Volatile
        private var instance: PhotoRepository? = null
        
        fun getInstance(context: Context): PhotoRepository {
            return instance ?: synchronized(this) {
                instance ?: PhotoRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        // 记录首次使用日期
        if (!prefs.contains(firstUseDateKey)) {
            prefs.edit().putLong(firstUseDateKey, System.currentTimeMillis()).apply()
        }
    }

    fun load(): List<Uri> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Uri>()
            for (i in 0 until arr.length()) {
                val s = arr.optString(i)
                if (s.isNotEmpty()) list.add(Uri.parse(s))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getAllPhotos(): List<Uri> = load()

    private fun save(list: List<Uri>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toString()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun add(uri: Uri) {
        val cur = load().toMutableList()
        // add to top
        cur.add(0, uri)
        save(cur)
    }

    fun remove(uri: Uri) {
        val cur = load().toMutableList()
        if (cur.remove(uri)) save(cur)
    }

    fun replaceAll(list: List<Uri>) {
        save(list)
    }
    
    fun incrementQuestionCount() {
        val current = prefs.getInt(questionCountKey, 0)
        prefs.edit().putInt(questionCountKey, current + 1).apply()
    }
    
    fun getQuestionCount(): Int {
        return prefs.getInt(questionCountKey, 0)
    }
    
    fun getUsageDays(): Int {
        val firstUseDate = prefs.getLong(firstUseDateKey, System.currentTimeMillis())
        val daysDiff = (System.currentTimeMillis() - firstUseDate) / (1000 * 60 * 60 * 24)
        return (daysDiff + 1).toInt() // 加1表示包含第一天
    }
}
