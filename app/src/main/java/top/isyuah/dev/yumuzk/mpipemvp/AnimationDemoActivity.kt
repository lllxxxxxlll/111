package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class AnimationDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation_demo)
        val web = findViewById<WebView>(R.id.web_ai)
        val html = intent.getStringExtra("html") ?: ""

        val settings: WebSettings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        if (html.isNotBlank()) {
            web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }
}
