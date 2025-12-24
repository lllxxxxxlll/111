package top.isyuah.dev.yumuzk.mpipemvp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import top.isyuah.dev.yumuzk.mpipemvp.algo.*
import top.isyuah.dev.yumuzk.mpipemvp.data.api.AiService
import top.isyuah.dev.yumuzk.mpipemvp.data.api.StreamEvent
import top.isyuah.dev.yumuzk.mpipemvp.databinding.ActivityAlgoExperimentBinding
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AlgoExperimentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlgoExperimentBinding
    private lateinit var cameraExecutor: ExecutorService
    private var currentBitmap: Bitmap? = null
    
    private lateinit var chatSheetBehavior: BottomSheetBehavior<View>
    private var selectedAlgo: String? = null
    private var isAnalyzing = false
    private var currentEngine: IAlgoEngine? = null

    private var sessionId: String = UUID.randomUUID().toString()
    private val aiService: AiService by lazy {
        AiService(HttpClient(Android), NetworkConfig.BASE_URL)
    }
    
    private val chatAdapter = ChatAdapter()
    private var lastSyncedMatrix: List<List<Int>>? = null
    private var stateChangeJob: Job? = null

    // For Visual Smoothing & Latched Alignment
    private var lastSmoothCorners: List<PointF>? = null
    private var lostDetectionCount = 0
    private val MAX_LOST_FRAMES = 8 

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) checkAndStartExperiment() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlgoExperimentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!OpenCVLoader.initDebug()) Log.e("OpenCV", "OpenCV init failed")
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupUI()
        showAlgoSelectorDialog(isInitial = true)
    }

    private fun setupUI() {
        binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_chat_history).apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@AlgoExperimentActivity)
        }

        chatSheetBehavior = BottomSheetBehavior.from(binding.root.findViewById(R.id.algo_chat_bottom_sheet))
        chatSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        chatSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.fabAiChat.animate().alpha(0f).withEndAction { binding.fabAiChat.visibility = View.GONE }.start()
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.fabAiChat.visibility = View.VISIBLE
                    binding.fabAiChat.animate().alpha(1f).start()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (chatSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    binding.fabAiChat.alpha = 1f - slideOffset.coerceIn(0f, 1f)
                }
            }
        })
        
        binding.fabAiChat.setOnClickListener { chatSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
        binding.cardAlgoSelector.setOnClickListener { showAlgoSelectorDialog(isInitial = false) }
        
        binding.root.findViewById<View>(R.id.btn_send_chat).setOnClickListener {
            val input = binding.root.findViewById<android.widget.EditText>(R.id.et_chat_input)
            val msg = input.text.toString()
            if (msg.isNotBlank()) performAiChat(msg)
        }

        binding.btnActionTrigger.setOnClickListener { executeAlgorithmStep() }
    }

    private fun performAiChat(message: String) {
        val input = binding.root.findViewById<android.widget.EditText>(R.id.et_chat_input)
        val sendBtn = binding.root.findViewById<View>(R.id.btn_send_chat)
        val recycler = binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_chat_history)
        
        chatAdapter.addMessage(ChatMessage(message, isUser = true))
        recycler.scrollToPosition(chatAdapter.itemCount - 1)
        
        input.text.clear()
        input.isEnabled = false
        input.hint = "AI 正在思考中..."
        sendBtn.isEnabled = false

        chatAdapter.addMessage(ChatMessage("", isUser = false))
        var fullAiResponse = ""

        lifecycleScope.launch {
            try {
                captureAndSyncStateIfChanged()
                aiService.chatStream(sessionId = sessionId, message = message).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            fullAiResponse += event.text
                            chatAdapter.updateLastMessage(fullAiResponse)
                            recycler.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                        is StreamEvent.Done -> {
                            if (chatSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                                val preview = if (fullAiResponse.length > 20) fullAiResponse.take(20) + "..." else fullAiResponse
                                Toast.makeText(this@AlgoExperimentActivity, "AI 回复: $preview", Toast.LENGTH_LONG).show()
                            }
                        }
                        is StreamEvent.Error -> chatAdapter.updateLastMessage("错误: ${event.message}")
                    }
                }
            } catch (e: Exception) {
                chatAdapter.updateLastMessage("连接失败: ${e.message}")
            } finally {
                input.isEnabled = true
                input.hint = "询问 AI 算法建议..."
                sendBtn.isEnabled = true
            }
        }
    }

    private fun executeAlgorithmStep() {
        val bitmap = currentBitmap ?: return
        val engine = currentEngine ?: return
        val corners = lastSmoothCorners ?: run {
            Toast.makeText(this, "请先对齐棋盘", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val matrix = sampleMatrixFromImage(bitmap, corners)
            val result = engine.run(matrix)
            withContext(Dispatchers.Main) {
                if (result.success) {
                    displayResult(result, corners)
                    if (selectedAlgo == "生命游戏" || selectedAlgo == "迷宫生成") {
                        val newMatrix = result.data as List<List<Int>>
                        syncStatusToAi(newMatrix, "算法演算生成了新状态")
                        lastSyncedMatrix = newMatrix
                    } else {
                        syncStatusToAi(matrix, "运行了 $selectedAlgo")
                        lastSyncedMatrix = matrix
                    }
                }
            }
        }
    }

    private suspend fun captureAndSyncStateIfChanged() {
        val bitmap = currentBitmap ?: return
        val corners = lastSmoothCorners ?: return
        val matrix = sampleMatrixFromImage(bitmap, corners)
        if (matrix != lastSyncedMatrix) {
            syncStatusToAi(matrix, "检测到物理状态变化")
            lastSyncedMatrix = matrix
        }
    }

    private suspend fun syncStatusToAi(matrix: List<List<Int>>, event: String) {
        val matrixStr = matrix.joinToString("\n") { row -> row.joinToString(",") }
        val prompt = "物理更新: $event | 算法: $selectedAlgo\n当前矩阵:\n$matrixStr"
        try {
            aiService.chat(sessionId = sessionId, prompt = prompt)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AlgoExperimentActivity, "状态已同步 ($event)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { Log.e("AiChat", "Sync failed: ${e.message}") }
    }

    private suspend fun sampleMatrixFromImage(bitmap: Bitmap, screenCorners: List<PointF>): List<List<Int>> {
        return withContext(Dispatchers.Default) {
            val matrix = MutableList(9) { MutableList(9) { CellState.EMPTY } }
            val bmpCorners = mapScreenToBmp(screenCorners, bitmap.width.toFloat(), bitmap.height.toFloat())
            val p1 = bmpCorners[0]; val p2 = bmpCorners[1]; val p3 = bmpCorners[2]; val p4 = bmpCorners[3]
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    val p = interpolate((col + 0.5f) / 9f, (row + 0.5f) / 9f, p1, p2, p3, p4)
                    val pixel = if (p.x >= 0 && p.x < bitmap.width && p.y >= 0 && p.y < bitmap.height) {
                        bitmap.getPixel(p.x.toInt(), p.y.toInt())
                    } else Color.TRANSPARENT
                    matrix[row][col] = classifyPixel(pixel)
                }
            }
            matrix
        }
    }

    private fun classifyPixel(pixel: Int): Int {
        val hsv = FloatArray(3); Color.colorToHSV(pixel, hsv)
        return when {
            hsv[2] < 0.35f -> CellState.BLACK
            (hsv[0] in 0f..25f || hsv[0] in 335f..360f) && hsv[1] > 0.45f -> CellState.RED
            else -> CellState.EMPTY
        }
    }

    private fun displayResult(result: AlgorithmResult, corners: List<PointF>) {
        binding.overlay.setPath(null); binding.overlay.setHighlight(null); binding.overlay.setResultMatrix(null)
        when (selectedAlgo) {
            "A*路径搜索" -> {
                val path = result.data as List<AlgoPoint>
                binding.overlay.setPath(path.map { interpolate((it.col + 0.5f) / 9f, (it.row + 0.5f) / 9f, corners[0], corners[1], corners[2], corners[3]) })
            }
            "五子棋AI" -> {
                val move = result.data as AlgoPoint
                binding.overlay.setHighlight(interpolate((move.col + 0.5f) / 9f, (move.row + 0.5f) / 9f, corners[0], corners[1], corners[2], corners[3]))
            }
            "生命游戏", "迷宫生成" -> binding.overlay.setResultMatrix(result.data as List<List<Int>>)
        }
    }

    private fun interpolate(tx: Float, ty: Float, p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF {
        val topX = p1.x + (p2.x - p1.x) * tx; val topY = p1.y + (p2.y - p1.y) * tx
        val botX = p4.x + (p3.x - p4.x) * tx; val botY = p4.y + (p3.y - p4.y) * tx
        return PointF(topX + (botX - topX) * ty, topY + (botY - topY) * ty)
    }

    private fun mapScreenToBmp(pts: List<PointF>, bmpW: Float, bmpH: Float): List<PointF> {
        val viewW = binding.overlay.width.toFloat(); val viewH = binding.overlay.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return pts
        val scale = maxOf(viewW / bmpW, viewH / bmpH)
        val offsetX = (viewW - bmpW * scale) / 2
        val offsetY = (viewH - bmpH * scale) / 2
        return pts.map { p -> PointF((p.x - offsetX) / scale, (p.y - offsetY) / scale) }
    }

    private fun mapBmpToScreen(pts: List<PointF>, bmpW: Float, bmpH: Float): List<PointF> {
        val viewW = binding.overlay.width.toFloat(); val viewH = binding.overlay.height.toFloat()
        val scale = maxOf(viewW / bmpW, viewH / bmpH)
        val offsetX = (viewW - bmpW * scale) / 2
        val offsetY = (viewH - bmpH * scale) / 2
        return pts.map { p -> PointF((p.x * scale) + offsetX, (p.y * scale) + offsetY) }
    }

    private fun showAlgoSelectorDialog(isInitial: Boolean) {
        val algos = arrayOf("A*路径搜索", "五子棋AI", "生命游戏", "迷宫生成")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择算法").setItems(algos) { _, which ->
                selectedAlgo = algos[which]
                binding.tvCurrentAlgo.text = selectedAlgo
                currentEngine = AlgoFactory.createEngine(selectedAlgo!!)
                onAlgorithmChanged()
            }
        if (isInitial) builder.setCancelable(false).setNegativeButton("返回") { _, _ -> finish() }
        else builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun onAlgorithmChanged() {
        binding.overlay.setGridMode(true); binding.overlay.clear()
        checkAndStartExperiment()
        lifecycleScope.launch {
            try { aiService.chat(sessionId = sessionId, prompt = "用户切换算法为: $selectedAlgo") } catch (e: Exception) {}
        }
    }

    private fun checkAndStartExperiment() {
        if (allPermissionsGranted()) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        val matrix = android.graphics.Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        currentBitmap = rotatedBitmap
                        if (!isAnalyzing) {
                            isAnalyzing = true
                            performAdvancedOpenCVAnalysis(rotatedBitmap)
                        }
                        imageProxy.close()
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) { Log.e("AlgoExp", "Camera error", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun performAdvancedOpenCVAnalysis(bitmap: Bitmap) {
        val gridRect = binding.overlay.gridRect ?: run { isAnalyzing = false; return }
        val viewW = binding.overlay.width.toFloat()
        val viewH = binding.overlay.height.toFloat()
        if (viewW <= 0f) { isAnalyzing = false; return }

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val src = Mat(); Utils.bitmapToMat(bitmap, src)
                val gray = Mat(); Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
                val edges = Mat(); Imgproc.Canny(gray, edges, 50.0, 150.0)
                val contours = mutableListOf<MatOfPoint>()
                Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
                
                var bestCorners: List<PointF>? = null
                var maxArea = -1.0
                
                for (contour in contours) {
                    val m2f = MatOfPoint2f(*contour.toArray())
                    val peri = Imgproc.arcLength(m2f, true)
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(m2f, approx, 0.035 * peri, true)
                    
                    if (approx.total() == 4L) {
                        val area = Imgproc.contourArea(approx)
                        if (area > maxArea && area > 4000) {
                            maxArea = area
                            bestCorners = sortCorners(approx.toArray())
                        }
                    }
                    m2f.release(); approx.release()
                }

                withContext(Dispatchers.Main) {
                    if (bestCorners != null) {
                        val viewCorners = mapBmpToScreen(bestCorners!!, bitmap.width.toFloat(), bitmap.height.toFloat())
                        if (isNearUiGrid(viewCorners, gridRect)) {
                            lastSmoothCorners = if (lastSmoothCorners == null) viewCorners
                            else smoothTransition(lastSmoothCorners!!, viewCorners)
                            
                            binding.overlay.setSnapCorners(lastSmoothCorners)
                            binding.overlay.setAlignmentState(true)
                            lostDetectionCount = 0 
                            checkStateChangeDebounced(bitmap, lastSmoothCorners!!)
                        } else {
                            handleDetectionLoss()
                        }
                    } else {
                        handleDetectionLoss()
                    }
                }
                src.release(); gray.release(); edges.release()
                contours.forEach { it.release() }
            } catch (e: Exception) {
                Log.e("OpenCV", "Analysis failed", e)
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun handleDetectionLoss() {
        lostDetectionCount++
        if (lostDetectionCount >= MAX_LOST_FRAMES) {
            binding.overlay.setSnapCorners(null)
            binding.overlay.setAlignmentState(false)
            lastSmoothCorners = null
        }
    }

    private fun smoothTransition(old: List<PointF>, new: List<PointF>): List<PointF> {
        val alpha = 0.35f 
        return old.zip(new).map { (o, n) ->
            PointF(o.x * (1 - alpha) + n.x * alpha, o.y * (1 - alpha) + n.y * alpha)
        }
    }

    private fun checkStateChangeDebounced(bitmap: Bitmap, corners: List<PointF>) {
        stateChangeJob?.cancel()
        stateChangeJob = lifecycleScope.launch(Dispatchers.Default) {
            delay(1200)
            val matrix = sampleMatrixFromImage(bitmap, corners)
            if (matrix != lastSyncedMatrix) {
                syncStatusToAi(matrix, "物理棋盘变化")
                lastSyncedMatrix = matrix
            }
        }
    }

    private fun sortCorners(pts: Array<org.opencv.core.Point>): List<PointF> {
        val sorted = pts.map { PointF(it.x.toFloat(), it.y.toFloat()) }.sortedBy { it.x + it.y }
        val tl = sorted[0]; val br = sorted[3]
        val remaining = sorted.subList(1, 3).sortedBy { it.x }
        return listOf(tl, remaining[1], br, remaining[0])
    }

    private fun isNearUiGrid(pts: List<PointF>, uiRect: android.graphics.RectF): Boolean {
        val centerX = pts.map { it.x }.average().toFloat()
        val centerY = pts.map { it.y }.average().toFloat()
        return uiRect.contains(centerX, centerY)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
