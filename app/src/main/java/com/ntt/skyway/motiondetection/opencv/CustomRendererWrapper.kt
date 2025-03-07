package com.ntt.skyway.motiondetection.opencv

import com.ntt.skyway.core.SkyWayOptIn
import com.ntt.skyway.core.content.local.source.VideoFrame
import com.ntt.skyway.core.content.sink.CustomRenderer
import com.ntt.skyway.core.content.sink.SurfaceViewRenderer
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

@OptIn(SkyWayOptIn::class)
class CustomRendererWrapper(
    private val customRenderer: CustomRenderer,
    private val motionOverlay: MotionOverlayView
) {
    private var previousFrame: Mat? = null

    init {
        customRenderer.onFrameHandler = { buffer ->
            val currentFrame = convertToMat(buffer)

            // Detect motion and get bounding boxes
            val motionRects = detectMotionAndGetBoundingBoxes(previousFrame, currentFrame)

            // Pass the frame width and height to scale correctly
            motionOverlay.updateMotionRects(motionRects, buffer.width, buffer.height)

            // Update previous frame for the next iteration
            previousFrame = currentFrame.clone()
        }
    }

    private fun detectMotionAndGetBoundingBoxes(prevFrame: Mat?, currFrame: Mat): List<org.opencv.core.Rect> {
        if (prevFrame == null) return emptyList()

        // Ensure both frames are the same size
        if (prevFrame.size() != currFrame.size()) {
            Imgproc.resize(currFrame, currFrame, prevFrame.size())
        }

        // Convert frames to grayscale
        val grayPrev = Mat()
        val grayCurr = Mat()

        Imgproc.cvtColor(prevFrame, grayPrev, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(currFrame, grayCurr, Imgproc.COLOR_RGB2GRAY)

        // Compute absolute difference
        val diffFrame = Mat()
        Core.absdiff(grayPrev, grayCurr, diffFrame)

        // Apply threshold to detect motion
        Imgproc.threshold(diffFrame, diffFrame, 35.0, 255.0, Imgproc.THRESH_BINARY)

        // Find contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(diffFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Convert contours to bounding boxes and filter by area
        return contours.map { Imgproc.boundingRect(it) }.filter { it.area() > 5000 }
    }

    @OptIn(SkyWayOptIn::class)
    fun convertToMat(buffer: CustomRenderer.VideoFrameBuffer): Mat {
        val yPlane = ByteArray(buffer.dataY.remaining())
        buffer.dataY.get(yPlane)

        val mat = Mat(buffer.height, buffer.width, CvType.CV_8UC1)
        mat.put(0, 0, yPlane)

        // Convert grayscale Y-plane to RGB (for motion detection)
        val matRgb = Mat()
        Imgproc.cvtColor(mat, matRgb, Imgproc.COLOR_GRAY2RGB)

        return matRgb
    }
}


