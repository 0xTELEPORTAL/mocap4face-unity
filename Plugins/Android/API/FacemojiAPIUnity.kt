package co.facemoji.mocap4face

import android.content.Context
import android.util.Log
import android.view.WindowManager
import co.facemoji.api.FacemojiAPI
import co.facemoji.io.ApplicationContext
import co.facemoji.math.Quaternion
import co.facemoji.tracker.FaceTrackerResult
import co.facemoji.tracker.OpenGLTexture
import co.facemoji.ui.CameraTextureView
import kotlin.math.max
import kotlin.math.min

class FacemojiAPIUnity {

    private var cameraTracker: CameraTracker? = null
    private var onActivateListener: OnActivateListener? = null;

    fun initialize(apiKey: String, context: Context, onActivateListener: OnActivateListener) {
        this.onActivateListener = onActivateListener;
        // API key input was formerly here
        onActivateListener.onActivate(true);
    }

    /**
     * Converts head rotation to blendshape-like coefficients to display in the UI
     */
    private fun faceRotationToSliders(rotation: Quaternion): Map<String, Float> {
        val euler = rotation.toEuler()
        val halfPi = Math.PI.toFloat() * 0.5f
        return mapOf(
            "headLeft" to max(0f, euler.y) / halfPi,
            "headRight" to -min(0f, euler.y) / halfPi,
            "headUp" to -min(0f, euler.x) / halfPi,
            "headDown" to max(0f, euler.x) / halfPi,
            "headRollLeft" to -min(0f, euler.z) / halfPi,
            "headRollRight" to max(0f, euler.z) / halfPi,
        )
    }

    private fun onTracker(cameraImage: OpenGLTexture, trackerResult: FaceTrackerResult?) {
        if (trackerResult != null) {
            val blendshapes = trackerResult.blendshapes +
                    faceRotationToSliders(trackerResult.rotationQuaternion)

            onActivateListener?.onBlendShapeValues(blendshapes.values.toFloatArray())

            val rot = trackerResult.rotationQuaternion.xyzw;
            onActivateListener?.onHeadRotation(rot.x,rot.y,rot.z,rot.w)
        } else {
            onActivateListener?.onBlendShapeValues(emptyArray<Float>().toFloatArray())
        }
    }

    fun createCameraTracker(context: Context) {
        val cameraTracker = CameraTracker(this, glContext)
        cameraTracker.trackerDelegate = this::onTracker
        cameraTracker.blendshapeNames.whenDone { names ->
            runOnUiThread {
                val headPoseNames = faceRotationToSliders(Quaternion.identity).keys
                blendshapesView?.blendshapeNames = (names + headPoseNames).sorted()
            }
        }
        this.cameraTracker = cameraTracker
    }

    fun pause() {
        cameraTracker?.stop()
    }

    fun resume() {
        cameraTracker?.restart()
    }

    fun destroy() {
        cameraTracker?.stop()
    }

    interface OnActivateListener {
        fun onActivate(activated: Boolean)
        fun onBlendShapeNames(names: List<String>)
        fun onBlendShapeValues(input: FloatArray?)
        fun onHeadRotation(x: Float, y: Float, z: Float, w: Float)
    }
}