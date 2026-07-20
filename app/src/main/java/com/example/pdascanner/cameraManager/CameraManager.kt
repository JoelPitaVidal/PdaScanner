package com.example.pdascanner.cameraManager

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.pdascanner.barcodeanalyzer.BarcodeAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
class CameraManager(private val context: Context, private val viewFinder: PreviewView) {

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentOnBarcodeDetected: ((String) -> Unit)? = null

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun setupCamera(lifecycleOwner: LifecycleOwner, onBarcodeDetected: (String) -> Unit) {
        this.currentLifecycleOwner = lifecycleOwner
        this.currentOnBarcodeDetected = onBarcodeDetected

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // 1. Configurar vista previa con AUTOENFOQUE CONTINUO (Vital para ver el QR nítido)
            val previewBuilder = Preview.Builder()

            val camera2PreviewExtender = Camera2Interop.Extender(previewBuilder)
            camera2PreviewExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            preview = previewBuilder.build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // 2. Configurar analizador con AUTOENFOQUE CONTINUO
            val analysisBuilder = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

            val camera2AnalysisExtender = Camera2Interop.Extender(analysisBuilder)
            camera2AnalysisExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            imageAnalysis = analysisBuilder.build().also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeDetected))
            }

            // 3. Configurar captura de fotos
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            activarModoEscaneo()

        }, ContextCompat.getMainExecutor(context))
    }

    private fun activarModoEscaneo() {
        val provider = cameraProvider ?: return
        val lifecycle = currentLifecycleOwner ?: return
        val prev = preview ?: return
        val analysis = imageAnalysis ?: return

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycle,
                CameraSelector.DEFAULT_BACK_CAMERA,
                prev,
                analysis
            )
            Log.d("CameraManager", "PDA en modo ESCANEO con Autoenfoque Continuo")
        } catch (e: Exception) {
            Log.e("CameraManager", "Error al iniciar modo escaneo: ${e.message}")
        }
    }

    fun takePhoto(qrContent: String, onSaved: (String, Uri?) -> Unit, onError: (String) -> Unit) {
        val provider = cameraProvider ?: return
        val lifecycle = currentLifecycleOwner ?: return
        val prev = preview ?: return
        val capture = imageCapture ?: return

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycle,
                CameraSelector.DEFAULT_BACK_CAMERA,
                prev,
                capture
            )
        } catch (e: Exception) {
            onError("Error de hardware al preparar foto: ${e.message}")
            activarModoEscaneo()
            return
        }

        val fileName = "${qrContent}_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PdaScanner")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(fileName, output.savedUri)
                    activarModoEscaneo()
                }
                override fun onError(exc: ImageCaptureException) {
                    onError(exc.message ?: "Error desconocido")
                    activarModoEscaneo()
                }
            })
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}