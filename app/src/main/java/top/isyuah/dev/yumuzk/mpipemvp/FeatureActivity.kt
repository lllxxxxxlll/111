package top.isyuah.dev.yumuzk.mpipemvp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.isyuah.dev.yumuzk.mpipemvp.databinding.ActivityFeatureBinding
import top.isyuah.dev.yumuzk.mpipemvp.data.api.AiService
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class FeatureActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityFeatureBinding
    private lateinit var cameraExecutor: ExecutorService
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var handLandmarker: HandLandmarker? = null
    private var objectDetector: ObjectDetector? = null

    private var lastPointingTime: Long = 0
    private var currentBitmap: Bitmap? = null
    private var isHovering = false
    private val hoverThresholdMs = 1500L

    private val httpClient: HttpClient = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 45_000
            socketTimeoutMillis = 45_000
        }
    }

    private val aiService: AiService by lazy {
        AiService(
            httpClient = httpClient,
            apiBaseUrl = NetworkConfig.BASE_URL
        )
    }

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startCamera() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFeatureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupMediaPipe()

        if (allPermissionsGranted()) startCamera() else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                .setBaseOptions(
                    BaseOptions.builder()
                        .setDelegate(Delegate.GPU)
                        .setModelAssetPath("efficientdet_lite2.tflite")
                        .build(),
                )
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
                    } else if (System.currentTimeMillis() - lastPointingTime > hoverThresholdMs) {
                        performCropAndAnalyze(hand)
                        isHovering = false
                    }
                    val progress =
                        (System.currentTimeMillis() - lastPointingTime).toFloat() / hoverThresholdMs
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
        val detectionResult = try {
            objectDetector?.detect(mpImage)
        } catch (e: Exception) {
            null
        }

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
                showAnalyzeDialog(croppedBitmap)
            }
        } catch (e: Exception) {
            Log.e("Crop", "Error: ${e.message}")
        }
    }

    private fun showAnalyzeDialog(crop: Bitmap) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pointing_result, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivCrop = dialogView.findViewById<ImageView>(R.id.iv_crop_preview)
        val tvAnalysis = dialogView.findViewById<TextView>(R.id.tv_analysis_content)
        val btnAction = dialogView.findViewById<MaterialButton>(R.id.btn_action)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pb_loading)

        ivCrop.setImageBitmap(crop)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnAction.setOnClickListener {
            tvAnalysis.text = "正在处理图片并请求 AI..."
            pbLoading.visibility = View.VISIBLE
            btnAction.isEnabled = false
            
            analyzeImageWithAI(
                bitmap = crop,
                onResult = { result ->
                    tvAnalysis.text = result
                    pbLoading.visibility = View.GONE
                    btnAction.text = "再次分析"
                    btnAction.isEnabled = true
                }
            )
        }

        dialog.show()
    }

    private fun analyzeImageWithAI(
        bitmap: Bitmap,
        onResult: (String) -> Unit,
    ) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64Image"

        activityScope.launch {
            val result = runCatching {
                aiService.analyzePointedObject(imageDataUrl = dataUrl)
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                onResult(result.getOrElse { e -> e.message ?: "请求失败（未知错误）" })
            }
        }
    }

    private fun isPointing(landmarks: List<NormalizedLandmark>): Boolean {
        fun dist(p1: NormalizedLandmark, p2: NormalizedLandmark) =
            sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2))
        return dist(landmarks[0], landmarks[8]) > dist(landmarks[0], landmarks[12]) * 1.35f
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable {
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
                                val matrix = android.graphics.Matrix().apply {
                                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                }
                                currentBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    0,
                                    0,
                                    bitmap.width,
                                    bitmap.height,
                                    matrix,
                                    true,
                                )
                                handLandmarker?.detectAsync(
                                    BitmapImageBuilder(currentBitmap).build(),
                                    System.currentTimeMillis(),
                                )
                                imageProxy.close()
                            }
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                } catch (e: Exception) {
                    Log.e("CameraX", "Error", e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        cameraExecutor.shutdown()
        handLandmarker?.close()
        objectDetector?.close()
        httpClient.close()
    }
}
