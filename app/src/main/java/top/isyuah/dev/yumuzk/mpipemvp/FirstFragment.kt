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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.webkit.WebView
import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
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
                // 对于长期持有的 Uri，尝试持久化权限
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
                repo.remove(uri)
                adapter.remove(uri)
                updatePhotoCount()
                updateEmptyState()
            },
            onItemClick = { uri ->
                showFullImageDialog(uri)
            }
        )

        binding.rvPhotos.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
        binding.rvPhotos.adapter = adapter

        val list = repo.load()
        adapter.replaceAll(list)
        updatePhotoCount()
        updateEmptyState()

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCamera.setOnClickListener {
            // Android 10+ 实际上拍照不需要 CAMERA 权限，但清单声明了建议还是请求下
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnGallery.setOnClickListener {
            // 使用 OpenDocument 不需要存储权限，由系统选择器处理
            openDocumentLauncher.launch(arrayOf("image/*"))
        }

        binding.btnSelectHost.setOnClickListener {
            runAiAnswer()
        }
    }

    private fun createPhotoFile() {
        // 使用外部私有目录，不需要存储权限
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
        
        val imagesArray = JSONArray()
        imageUris.forEach { uri ->
            val dataUrl = uriToDataUrl(uri)
            if (dataUrl.isNotEmpty()) {
                imagesArray.put(dataUrl)
            }
        }

        val body = JSONObject().apply {
            put("images", imagesArray)
        }

        val response = client.post("${NetworkConfig.BASE_URL}/ai/homework") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }

        val responseText = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw Exception("服务器错误 (${response.status.value}): $responseText")
        }

        return responseText
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
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val answerView = dialog.findViewById<android.widget.TextView>(R.id.tv_ai_answer_content)
        val analysisView = dialog.findViewById<android.widget.TextView>(R.id.tv_ai_analysis_content)
        val btnRender = dialog.findViewById<android.widget.Button>(R.id.btn_render_html)

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
        try {
            val cacheDir = requireContext().cacheDir
            cacheDir.listFiles()?.forEach {
                if (it.name.startsWith("photo_")) it.delete()
            }
            val externalDir = requireContext().getExternalFilesDir(null)
            externalDir?.listFiles()?.forEach {
                if (it.name.startsWith("photo_") && it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                    it.delete()
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
