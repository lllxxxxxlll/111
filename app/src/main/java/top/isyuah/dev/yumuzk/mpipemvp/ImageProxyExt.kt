package top.isyuah.dev.yumuzk.mpipemvp

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

fun ImageProxy.toBitmap(): Bitmap {
    val planeProxy = planes.firstOrNull()
        ?: throw IllegalStateException("ImageProxy has no planes")

    val buffer = planeProxy.buffer
    buffer.rewind()

    val pixelStride = planeProxy.pixelStride
    val rowStride = planeProxy.rowStride
    val rowPadding = rowStride - pixelStride * width

    val paddedWidth = width + rowPadding / pixelStride
    val bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)

    bitmap.copyPixelsFromBuffer(buffer.toReadOnlyCopy())
    return Bitmap.createBitmap(bitmap, 0, 0, width, height)
}

private fun ByteBuffer.toReadOnlyCopy(): ByteBuffer {
    val readOnly = asReadOnlyBuffer()
    readOnly.rewind()
    return readOnly
}

