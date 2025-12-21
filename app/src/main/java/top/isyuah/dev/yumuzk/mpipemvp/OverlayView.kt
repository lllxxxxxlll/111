package top.isyuah.dev.yumuzk.mpipemvp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // 保存当前的单一手部数据（不是 HandLandmarkerResult 了，而是直接存 List）
    private var currentHand: List<NormalizedLandmark>? = null

    // 状态控制
    private var isPointing = false
    private var loadingProgress = 0f

    private var linePaint = Paint()
    private var pointPaint = Paint()

    // 缩放参数 (默认用 View 自身的宽高)
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    private fun initPaints() {
        // 1. 准星画笔 (青色)
        linePaint.color = Color.CYAN
        linePaint.strokeWidth = 5f
        linePaint.style = Paint.Style.STROKE
        linePaint.isAntiAlias = true // 抗锯齿

        // 2. 进度条画笔 (红色)
        pointPaint.color = Color.RED
        pointPaint.strokeWidth = 8f
        pointPaint.style = Paint.Style.STROKE
        pointPaint.strokeCap = Paint.Cap.ROUND // 线头圆角
        pointPaint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // 如果没有手部数据，直接返回
        val hand = currentHand ?: return
        if (hand.isEmpty()) return

        // 获取食指指尖 (索引 8)
        val tip = hand[8]

        // 如果还没设置过图像尺寸，默认使用当前 View 的尺寸
        if (imageWidth == 1) {
            imageWidth = width
            imageHeight = height
        }

        // 计算缩放比例 (确保覆盖全屏)
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        // 坐标转换：归一化坐标(0~1) -> 屏幕像素坐标
        val x = tip.x() * imageWidth * scaleFactor
        val y = tip.y() * imageHeight * scaleFactor

        if (isPointing) {
            // --- 绘制准星模式 ---

            val size = 60f // 准星半径

            // 1. 画十字准星
            canvas.drawLine(x - size, y, x + size, y, linePaint)
            canvas.drawLine(x, y - size, x, y + size, linePaint)

            // 2. 画外围圆圈 (带透明度背景)
            linePaint.alpha = 80
            canvas.drawCircle(x, y, size * 1.5f, linePaint)
            linePaint.alpha = 255 // 恢复不透明

            // 3. 画红色倒计时进度条
            if (loadingProgress > 0) {
                val radius = size * 1.5f
                val rectF = RectF(x - radius, y - radius, x + radius, y + radius)
                // -90度开始，扫过 360 * progress 度
                canvas.drawArc(rectF, -90f, 360 * loadingProgress, false, pointPaint)
            }
        } else {
            // --- 未指向模式 ---
            // 可以画一个小点跟随指尖，或者什么都不画
            // 这里画一个小圆点表示“识别中”
            canvas.drawCircle(x, y, 10f, linePaint)
        }
    }

    // ⚠️这就是你报错缺失的那个方法
    fun showFocus(landmarks: List<NormalizedLandmark>, pointing: Boolean, progress: Float) {
        this.currentHand = landmarks
        this.isPointing = pointing
        this.loadingProgress = progress

        // 触发重绘
        invalidate()
    }

    fun clear() {
        currentHand = null
        isPointing = false
        loadingProgress = 0f
        invalidate()
    }

    // 保持这个方法，以便在 Activity 其他地方需要更新尺寸时调用
    fun setDetails(imageHeight: Int, imageWidth: Int) {
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        // 重新计算缩放
        this.scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }
}