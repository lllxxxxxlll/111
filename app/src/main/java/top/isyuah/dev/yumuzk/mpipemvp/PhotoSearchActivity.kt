package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PhotoSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_search)
        supportActionBar?.title = getString(R.string.photo_search_card_title)
    }
}
