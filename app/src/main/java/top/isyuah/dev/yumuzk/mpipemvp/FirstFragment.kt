package top.isyuah.dev.yumuzk.mpipemvp

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.webkit.WebView
import android.webkit.WebSettings
import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import top.isyuah.dev.yumuzk.mpipemvp.databinding.FragmentFirstBinding
import java.io.File

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: PhotoRepository
    private lateinit var adapter: PhotoAdapter

    private var currentPhotoUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            createPhotoFile()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openDocumentLauncher.launch(arrayOf("image/*"))
        } else {
            Toast.makeText(requireContext(), "需要存储权限才能访问相册", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestWriteStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Toast.makeText(requireContext(), "需要存储权限才能保存照片", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            repo.add(currentPhotoUri!!)
            adapter.addToTop(currentPhotoUri!!)
            currentPhotoUri = null
            updatePhotoCount()
            updateEmptyState()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: Exception) {
                // ignore
            }
            repo.add(it)
            adapter.addToTop(it)
            updatePhotoCount()
            updateEmptyState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = PhotoRepository.getInstance(requireContext())

        adapter = PhotoAdapter(mutableListOf(),
            onDelete = { uri ->
                // on delete button click -> remove
                repo.remove(uri)
                adapter.remove(uri)
                updatePhotoCount()
                updateEmptyState()
            },
            onItemClick = { uri ->
                // on click -> show full image
                showFullImageDialog(uri)
            }
        )

        binding.rvPhotos.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
        binding.rvPhotos.adapter = adapter

        // load existing photos
        val list = repo.load()
        adapter.replaceAll(list)
        updatePhotoCount()
        updateEmptyState()

        // Back button
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCamera.setOnClickListener {
            // check permission
            requestWriteStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        binding.btnGallery.setOnClickListener {
            requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        binding.btnSelectHost.setOnClickListener {
            // AI 解答（使用 DashScope 兼容 OpenAI 接口）
            runAiAnswer()
        }
    }

    private fun createPhotoFile() {
        val photoFile = File(requireContext().getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(currentPhotoUri)
    }

    private fun showFullImageDialog(uri: Uri) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_full_image)
            .create()

        dialog.show()

        val imgView = dialog.findViewById<ImageView>(R.id.img_full)
        imgView?.let {
            Glide.with(requireContext())
                .load(uri)
                .into(it)
        }
    }

    private fun runAiAnswer() {
        val images = repo.load()
        if (images.isEmpty()) {
            Toast.makeText(requireContext(), "请先拍照或选择相册图片", Toast.LENGTH_SHORT).show()
            return
        }

        val loading = AlertDialog.Builder(requireContext())
            .setMessage("AI 正在解答，请稍候…")
            .setCancelable(false)
            .create()
        loading.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultJson = callAiForImages(images.take(3))
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    // 增加问题计数
                    repo.incrementQuestionCount()
                    showAiResultDialog(resultJson)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(requireContext(), "AI 调用失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun callAiForImages(imageUris: List<Uri>): String {
        val client = HttpClient(Android)
        val contentArr = JSONArray()
        imageUris.forEach { uri ->
            val dataUrl = uriToDataUrl(uri)
            val imgObj = JSONObject()
                .put("type", "image_url")
                .put("image_url", JSONObject().put("url", dataUrl))
            contentArr.put(imgObj)
        }
        contentArr.put(
            JSONObject().put("type", "text").put("text", """
                请严格只返回JSON字符串，不要任何其它文字。结构必须为：
                {"answer":字符串,"analysis":字符串(尽量精炼、条目化),"html":字符串}
                其中 html 要构建一个可在移动端竖屏直接运行的“电子小实验”页面：
                - 所有资源内联（CSS/JS），不依赖外部网络；
                - 响应式布局，视口 meta 正确，避免溢出；
                - 以参数调节为核心（滑块/输入/按钮），用户可改变关键实验参数并实时看到结果变化；
                - 提供清晰的状态显示（当前参数、实验结果、提示信息），并支持步骤化说明（上一/下一步、重置）；
                - 使用 requestAnimationFrame 或 CSS transform 保持流畅动画（如波形、轨迹、速率变化等）；
                - 页面内置交互控件即可操作，无需外部函数调用；
                - UI 元素简洁友好，字体与控件适合移动端触控；
                - 示例可以结合理科知识点（如电路/力学/化学反应的参数变化）但必须一般化，不依赖题目图片文字内容。
            """.trim())
        )

        val sys = JSONObject().put("role", "system").put("content", "只输出JSON字符串（answer、analysis、html）。html需为移动端竖屏的电子小实验，内联资源、参数可调、交互丰富、说明简洁，不需要也不依赖外部函数或网络。")
        val msg = JSONObject().put("role", "user").put("content", contentArr)
        val body = JSONObject()
            .put("model", "qwen3-vl-plus")
            .put("messages", JSONArray().put(sys).put(msg))
            .put("temperature", 0.2)

        val responseText = client.post("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.DASHSCOPE_API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.bodyAsText()

        val obj = JSONObject(responseText)
        val choices = obj.optJSONArray("choices")
        val first = choices?.optJSONObject(0)
        val message = first?.optJSONObject("message")
        val content = message?.optString("content")
        return content ?: responseText
    }

    private fun uriToDataUrl(uri: Uri): String {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri)
            val bytes = input?.use { it.readBytes() } ?: ByteArray(0)
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            ""
        }
    }

    private fun showAiResultDialog(jsonText: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_ai_result)
            .create()
        
        // 设置对话框窗口背景为透明，显示自定义圆角背景
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.show()

        val answerView = dialog.findViewById<android.widget.TextView>(R.id.tv_ai_answer_content)
        val analysisView = dialog.findViewById<android.widget.TextView>(R.id.tv_ai_analysis_content)
        val btnRender = dialog.findViewById<android.widget.Button>(R.id.btn_render_html)
        val web = dialog.findViewById<WebView>(R.id.web_ai)

        var html = ""
        try {
            val obj = JSONObject(jsonText)
            val ans = obj.optString("answer")
            val ana = obj.optString("analysis")
            html = obj.optString("html")
            answerView?.text = ans
            analysisView?.text = ana
        } catch (e: Exception) {
            answerView?.text = "解析失败，原始返回：\n$jsonText"
            analysisView?.text = ""
        }

        btnRender?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), AnimationDemoActivity::class.java)
            intent.putExtra("answer", answerView?.text?.toString() ?: "")
            intent.putExtra("analysis", analysisView?.text?.toString() ?: "")
            intent.putExtra("html", html)
            startActivity(intent)
        }
    }

    private fun updatePhotoCount() {
        val count = repo.load().size
        binding.tvPhotoCount.text = getString(R.string.photo_count_placeholder, count)
    }

    private fun updateEmptyState() {
        val isEmpty = repo.load().isEmpty()
        binding.emptyState.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.rvPhotos.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // 清理缓存中本 fragment 生成的临时图片（如果有）
        try {
            val cacheDir = requireContext().cacheDir
            cacheDir.listFiles()?.forEach {
                if (it.name.startsWith("photo_")) it.delete()
            }
            // 清理外部文件中的旧照片（可选，保留最近的）
            val externalDir = requireContext().getExternalFilesDir(null)
            externalDir?.listFiles()?.forEach {
                if (it.name.startsWith("photo_") && it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                    it.delete() // 删除超过24小时的照片
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
