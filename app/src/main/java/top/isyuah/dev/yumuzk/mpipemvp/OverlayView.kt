package top.isyuah.dev.yumuzk.mpipemvp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import top.isyuah.dev.yumuzk.mpipemvp.algo.CellState
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var currentHand: List<NormalizedLandmark>? = null
    private var isPointing = false
    private var loadingProgress = 0f

    var gridRect: RectF? = null
        private set
    var snapCorners: List<PointF>? = null
        private set
    
    private var pathPoints: List<PointF>? = null
    private var highlightPoint: PointF? = null
    private var resultMatrix: List<List<Int>>? = null 
    
    private var showGrid = false
    private var isAligned = false
    private var gridColor = Color.parseColor("#40FFFFFF") 

    private val linePaint = Paint().apply {
        color = Color.CYAN; strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true
    }
    private val pathPaint = Paint().apply {
        color = Color.YELLOW; strokeWidth = 8f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
        setShadowLayer(15f, 0f, 0f, Color.YELLOW)
    }
    private val pointPaint = Paint().apply {
        color = Color.RED; strokeWidth = 8f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL; isAntiAlias = true
    }

    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    fun setGridMode(enabled: Boolean) {
        this.showGrid = enabled
        if (!enabled) { gridRect = null; snapCorners = null }
        else {
            val size = minOf(width, height) * 0.8f
            val left = (width - size) / 2
            val top = (height - size) / 2
            gridRect = RectF(left, top, left + size, top + size)
        }
        invalidate()
    }

    fun setSnapCorners(corners: List<PointF>?) { this.snapCorners = corners; invalidate() }
    fun setAlignmentState(aligned: Boolean) {
        this.isAligned = aligned
        this.gridColor = if (aligned) Color.GREEN else Color.parseColor("#80FFFFFF")
        invalidate()
    }
    fun setPath(points: List<PointF>?) { this.pathPoints = points; invalidate() }
    fun setHighlight(point: PointF?) { this.highlightPoint = point; invalidate() }
    fun setResultMatrix(matrix: List<List<Int>>?) { this.resultMatrix = matrix; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showGrid) drawGuideGrid(canvas)
        drawResultMatrix(canvas)
        drawPath(canvas)
        drawHighlight(canvas)
        drawHandTracker(canvas)
    }

    private fun drawGuideGrid(canvas: Canvas) {
        val rect = gridRect ?: return
        gridPaint.color = gridColor
        val corners = snapCorners
        if (corners != null && corners.size == 4) {
            drawSnappedGrid(canvas, corners)
        } else {
            canvas.drawRect(rect, gridPaint)
            gridPaint.alpha = if (isAligned) 100 else 60
            val step = rect.width() / 9
            for (i in 1 until 9) {
                val offset = i * step
                canvas.drawLine(rect.left + offset, rect.top, rect.left + offset, rect.bottom, gridPaint)
                canvas.drawLine(rect.left, rect.top + offset, rect.right, rect.top + offset, gridPaint)
            }
        }
    }

    private fun drawSnappedGrid(canvas: Canvas, corners: List<PointF>) {
        val p1 = corners[0]; val p2 = corners[1]; val p3 = corners[2]; val p4 = corners[3]
        val path = Path()
        path.moveTo(p1.x, p1.y); path.lineTo(p2.x, p2.y); path.lineTo(p3.x, p3.y); path.lineTo(p4.x, p4.y)
        path.close(); canvas.drawPath(path, gridPaint)

        gridPaint.alpha = 100
        for (i in 1 until 9) {
            val t = i / 9f
            canvas.drawLine(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t, p4.x + (p3.x - p4.x) * t, p4.y + (p3.y - p4.y) * t, gridPaint)
            canvas.drawLine(p1.x + (p4.x - p1.x) * t, p1.y + (p4.y - p1.y) * t, p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t, gridPaint)
        }
    }

    private fun drawResultMatrix(canvas: Canvas) {
        val matrix = resultMatrix ?: return
        val corners = snapCorners ?: return
        if (corners.size != 4) return
        
        val p1 = corners[0]; val p2 = corners[1]; val p3 = corners[2]; val p4 = corners[3]
        
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val state = matrix[r][c]
                if (state != CellState.EMPTY) {
                    fillPaint.color = when(state) {
                        CellState.BLACK -> Color.argb(180, 20, 20, 20)
                        CellState.RED -> Color.argb(180, 255, 60, 60)
                        else -> Color.argb(180, 60, 60, 255)
                    }

                    val center = interpolate((c + 0.5f) / 9f, (r + 0.5f) / 9f, p1, p2, p3, p4)
                    val pEdge = interpolate((c + 0.85f) / 9f, (r + 0.5f) / 9f, p1, p2, p3, p4)
                    val radius = sqrt((pEdge.x - center.x).pow(2) + (pEdge.y - center.y).pow(2))

                    canvas.drawCircle(center.x, center.y, radius, fillPaint)
                }
            }
        }
    }

    private fun interpolate(tx: Float, ty: Float, p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF {
        val topX = p1.x + (p2.x - p1.x) * tx
        val topY = p1.y + (p2.y - p1.y) * tx
        val botX = p4.x + (p3.x - p4.x) * tx
        val botY = p4.y + (p3.y - p4.y) * tx
        return PointF(topX + (botX - topX) * ty, topY + (botY - topY) * ty)
    }

    private fun drawPath(canvas: Canvas) {
        val points = pathPoints ?: return
        if (points.size < 2) return
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        canvas.drawPath(path, pathPaint)
    }

    private fun drawHighlight(canvas: Canvas) {
        val p = highlightPoint ?: return
        pointPaint.color = Color.GREEN; pointPaint.style = Paint.Style.FILL_AND_STROKE; pointPaint.alpha = 150
        canvas.drawCircle(p.x, p.y, 30f, pointPaint)
        pointPaint.style = Paint.Style.STROKE; pointPaint.alpha = 255
        canvas.drawCircle(p.x, p.y, 40f, pointPaint)
    }

    private fun drawHandTracker(canvas: Canvas) {
        val hand = currentHand ?: return
        val tip = hand[8]
        val scale = max(width * 1f / imageWidth, height * 1f / imageHeight)
        val x = tip.x() * imageWidth * scale
        val y = tip.y() * imageHeight * scale
        if (isPointing) {
            val size = 60f
            canvas.drawLine(x - size, y, x + size, y, linePaint)
            canvas.drawLine(x, y - size, x, y + size, linePaint)
        }
    }

    fun showFocus(landmarks: List<NormalizedLandmark>, pointing: Boolean, progress: Float) {
        this.currentHand = landmarks; this.isPointing = pointing; this.loadingProgress = progress; invalidate()
    }

    fun clear() {
        currentHand = null; isPointing = false; loadingProgress = 0f
        pathPoints = null; highlightPoint = null; snapCorners = null; resultMatrix = null; invalidate()
    }

    fun setDetails(h: Int, w: Int) { this.imageHeight = h; this.imageWidth = w }
}
