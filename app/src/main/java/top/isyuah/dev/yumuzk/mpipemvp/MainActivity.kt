package top.isyuah.dev.yumuzk.mpipemvp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import top.isyuah.dev.yumuzk.mpipemvp.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    
    private var handLandmarker: HandLandmarker? = null
    private var objectDetector: ObjectDetector? = null
    
    private var lastPointingTime: Long = 0
    private var currentBitmap: Bitmap? = null
    private var isHovering = false
    private val HOVER_THRESHOLD = 1500L

    // --- AI 配置 (请在此填写你的 API 信息) ---
    private val AI_API_URL = "https://api.siliconflow.cn/v1/chat/completions"
    private val AI_MODEL = "THUDM/GLM-4.1V-9B-Thinking"
    private val AI_API_KEY = "sk-phxtmtidlesleiqynjtxskegyqdhljsalilvhguwhgcihpzf"
    private val client = OkHttpClient()

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startCamera() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) startCamera() else requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupMediaPipe()
    }

    private fun setupMediaPipe() {
        val handBaseOptions = BaseOptions.builder()
            .setDelegate(Delegate.GPU)
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val handOptions = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(handBaseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setNumHands(2)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> processHandResult(result) }
            .build()
        handLandmarker = HandLandmarker.createFromOptions(this, handOptions)

        try {
            val objOptions = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder()
                    .setDelegate(Delegate.GPU)
                    .setModelAssetPath("efficientdet_lite2.tflite")
                    .build())
                .setScoreThreshold(0.35f)
                .setRunningMode(RunningMode.IMAGE) 
                .build()
            objectDetector = ObjectDetector.createFromOptions(this, objOptions)
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Model error: ${e.message}")
        }
    }

    private fun processHandResult(result: HandLandmarkerResult) {
        runOnUiThread {
            val landmarksList = result.landmarks()
            if (landmarksList.isNotEmpty()) {
                val hand = landmarksList[0]
                if (isPointing(hand)) {
                    if (!isHovering) {
                        isHovering = true
                        lastPointingTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - lastPointingTime > HOVER_THRESHOLD) {
                        performCropAndAnalyze(hand)
                        isHovering = false 
                    }
                    val progress = (System.currentTimeMillis() - lastPointingTime).toFloat() / HOVER_THRESHOLD
                    viewBinding.overlay.showFocus(hand, true, progress.coerceIn(0f, 1f))
                } else {
                    isHovering = false
                    viewBinding.overlay.showFocus(hand, false, 0f)
                }
            } else {
                viewBinding.overlay.clear()
                isHovering = false
            }
        }
    }

    private fun performCropAndAnalyze(hand: List<NormalizedLandmark>) {
        val fullImage = currentBitmap ?: return
        if (fullImage.isRecycled) return

        val imgW = fullImage.width.toFloat()
        val imgH = fullImage.height.toFloat()
        
        val mpImage = BitmapImageBuilder(fullImage).build()
        val detectionResult = try { objectDetector?.detect(mpImage) } catch (e: Exception) { null }

        val tip = hand[8]
        val base = hand[5]
        val dx = tip.x() - base.x()
        val dy = tip.y() - base.y()
        
        val startX = tip.x() - dx * 0.5f
        val startY = tip.y() - dy * 0.5f
        var maxT = 1.0f 
        
        detectionResult?.detections()?.let { detections ->
            for (detection in detections) {
                val box = detection.boundingBox()
                for (step in 1..20) {
                    val t = step * 0.15f
                    val px = (tip.x() + dx * t) * imgW
                    val py = (tip.y() + dy * t) * imgH
                    if (box.contains(px, py)) {
                        maxT = maxOf(maxT, t + 0.3f)
                        break
                    }
                }
            }
        }
        
        val endX = tip.x() + dx * maxT
        val endY = tip.y() + dy * maxT
        val minX = min(startX, endX)
        val maxX = max(startX, endX)
        val minY = min(startY, endY)
        val maxY = max(startY, endY)

        val padding = 0.05f
        val rectW = (maxX - minX + padding * 2) * imgW
        val rectH = (maxY - minY + padding * 2) * imgH
        val size = max(max(rectW, rectH), 350f).toInt()
        val centerX = ((minX + maxX) / 2 * imgW).toInt()
        val centerY = ((minY + maxY) / 2 * imgH).toInt()

        val left = (centerX - size / 2).toInt().coerceIn(0, imgW.toInt() - size)
        val top = (centerY - size / 2).toInt().coerceIn(0, imgH.toInt() - size)
        val finalSize = size.coerceAtMost(min(imgW.toInt(), imgH.toInt()))

        try {
            val croppedBitmap = Bitmap.createBitmap(fullImage, left, top, finalSize, finalSize)
            runOnUiThread {
                showEnhancedDebugDialog(fullImage, croppedBitmap, left, top, finalSize, hand, detectionResult)
            }
        } catch (e: Exception) {
            Log.e("Crop", "Error: ${e.message}")
        }
    }

    private fun showEnhancedDebugDialog(
        original: Bitmap,
        crop: Bitmap,
        x: Int, y: Int, size: Int,
        hand: List<NormalizedLandmark>,
        detectionResult: ObjectDetectorResult?
    ) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("AI 指向分析")
            .setCancelable(false)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)
        
        val cropView = android.widget.ImageView(this)
        cropView.setImageBitmap(crop)
        layout.addView(cropView, android.widget.LinearLayout.LayoutParams(700, 700).apply { 
            gravity = android.view.Gravity.CENTER 
            setMargins(0,0,0,30)
        })

        val statusText = TextView(this)
        statusText.text = "点击“发送AI”开始识别图片内容..."
        layout.addView(statusText)

        dialog.setView(layout)
        dialog.setPositiveButton("发送AI") { _, _ -> 
            analyzeImageWithAI(crop, statusText) 
        }
        dialog.setNegativeButton("取消", null)
        
        val alertDialog = dialog.create()
        alertDialog.show()
        
        // 我们需要手动处理 PositiveButton 的逻辑，因为网络请求是异步的，不希望对话框立刻消失
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            statusText.text = "正在处理图片并请求 AI..."
            it.isEnabled = false // 禁用按钮防止重复点击
            analyzeImageWithAI(crop, statusText)
        }
    }

    private fun analyzeImageWithAI(bitmap: Bitmap, outputView: TextView) {
        // 1. 转 Base64
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64Image"

        // 2. 构造 JSON
        val json = JSONObject()
        json.put("model", AI_MODEL)
        
        val messages = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        
        val content = JSONArray()
        
        val imageContent = JSONObject()
        imageContent.put("type", "image_url")
        val imageUrl = JSONObject()
        imageUrl.put("url", dataUrl)
        imageContent.put("image_url", imageUrl)
        content.put(imageContent)
        
        val textContent = JSONObject()
        textContent.put("type", "text")
        textContent.put("text", "请分析图片中手指指向的物体是什么？")
        content.put(textContent)
        
        userMessage.put("content", content)
        messages.put(userMessage)
        json.put("messages", messages)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(AI_API_URL)
            .addHeader("Authorization", "Bearer $AI_API_KEY")
            .post(body)
            .build()

        // 3. 发送请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputView.text = "请求失败: ${e.message}" }
            }

            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && resBody != null) {
                        try {
                            val jsonRes = JSONObject(resBody)
                            val result = jsonRes.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                            
                            // 结果可能很长，用 ScrollView 包裹
                            outputView.text = result
                        } catch (e: Exception) {
                            outputView.text = "解析失败: ${e.message}\n响应内容: $resBody"
                        }
                    } else {
                        outputView.text = "API 错误 (${response.code}): $resBody"
                    }
                }
            }
        })
    }

    private fun isPointing(landmarks: List<NormalizedLandmark>): Boolean {
        fun dist(p1: NormalizedLandmark, p2: NormalizedLandmark) = sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2))
        return dist(landmarks[0], landmarks[8]) > dist(landmarks[0], landmarks[12]) * 1.35f
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                    .also { it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider) }
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            val matrix = android.graphics.Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                            currentBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            handLandmarker?.detectAsync(BitmapImageBuilder(currentBitmap).build(), System.currentTimeMillis())
                            imageProxy.close()
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) { Log.e("CameraX", "Error", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handLandmarker?.close()
        objectDetector?.close()
    }
}
