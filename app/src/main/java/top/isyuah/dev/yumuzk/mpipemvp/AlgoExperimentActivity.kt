package top.isyuah.dev.yumuzk.mpipemvp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import top.isyuah.dev.yumuzk.mpipemvp.databinding.ActivityAlgoExperimentBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AlgoExperimentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlgoExperimentBinding
    private lateinit var cameraExecutor: ExecutorService
    private var currentBitmap: Bitmap? = null
    
    private lateinit var chatSheetBehavior: BottomSheetBehavior<View>
    private var selectedAlgo: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) checkAndStartExperiment() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlgoExperimentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        
        // Initial state: ask user to select an algorithm first
        showAlgoSelectorDialog(isInitial = true)
    }

    private fun setupUI() {
        // Bottom Sheet Setup
        chatSheetBehavior = BottomSheetBehavior.from(binding.root.findViewById(R.id.algo_chat_bottom_sheet))
        chatSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        chatSheetBehavior.isHideable = true
        chatSheetBehavior.peekHeight = 0

        chatSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    binding.fabAiChat.hide()
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.fabAiChat.show()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Animate FAB alpha or scale based on slide if needed
                binding.fabAiChat.alpha = 1f - slideOffset.coerceIn(0f, 1f)
            }
        })
        
        binding.fabAiChat.setOnClickListener {
            chatSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.cardAlgoSelector.setOnClickListener {
            showAlgoSelectorDialog(isInitial = false)
        }
    }

    private fun showAlgoSelectorDialog(isInitial: Boolean) {
        val algos = arrayOf(
            getString(R.string.algo_gomoku),
            getString(R.string.algo_maze_gen),
            getString(R.string.algo_astar),
            getString(R.string.algo_maze_bfs),
            getString(R.string.algo_maze_dfs),
            getString(R.string.algo_life)
        )
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.select_algo)
            .setItems(algos) { _, which ->
                val newAlgo = algos[which]
                if (selectedAlgo != newAlgo) {
                    selectedAlgo = newAlgo
                    binding.tvCurrentAlgo.text = selectedAlgo
                    onAlgorithmChanged()
                }
            }
        
        if (isInitial) {
            builder.setCancelable(false) // Force selection on start
            builder.setNegativeButton("返回") { _, _ -> finish() }
        } else {
            builder.setNegativeButton("取消", null)
        }
        
        builder.show()
    }

    private fun onAlgorithmChanged() {
        // Here we would reset WS connection and send new configuration
        Toast.makeText(this, "已切换算法: $selectedAlgo, 正在重新连接...", Toast.LENGTH_SHORT).show()
        
        // Ensure camera is started if permissions are granted
        checkAndStartExperiment()
    }

    private fun checkAndStartExperiment() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        val matrix = android.graphics.Matrix().apply {
                            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        }
                        currentBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("AlgoExp", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
