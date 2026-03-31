package com.example.pdascanner.cameraManager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.pdascanner.barcodeanalyzer.BarcodeAnalyzer
import java.text.SimpleDateFormat
import java.util.Locale

class CameraManager(
    private val context: Context,
    private val viewFinder: PreviewView
) {
    private val cameraController = LifecycleCameraController(context)

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        onBarcodeDetected: (String) -> Unit
    ) {
        // 1. Forzar TextureView para que la rotación manual de 180f sea efectiva
        viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER

        // 2. Configuración de cámara trasera y casos de uso
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.setEnabledUseCases(
            CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
        )

        // 3. Analizador de códigos de barras (ML Kit)
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            BarcodeAnalyzer { barcode ->
                onBarcodeDetected(barcode)
            }
        )

        // 4. Vincular el controlador a la vista
        viewFinder.controller = cameraController

        // 5. Aplicar la rotación manual que soluciona el problema del hardware invertido
        viewFinder.post {
            viewFinder.scaleX = 1f
            viewFinder.scaleY = 1f
            viewFinder.rotation = 180f
            Log.d("CameraManager", "Rotación de 180° aplicada correctamente.")
        }

        try {
            cameraController.bindToLifecycle(lifecycleOwner)
        } catch (e: Exception) {
            Log.e("CameraManager", "Error al vincular Lifecycle: ${e.message}")
        }
    }

    /**
     * Captura la foto y devuelve tanto el nombre como la URI para futuros envíos a API
     */
    fun takePhoto(
        qrContent: String,
        onSaved: (String, Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        val safeName = qrContent
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifEmpty { "SCAN" }

        val datePart = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
        val fileName = "${safeName}_$datePart"

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

        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Devolvemos el nombre y la URI (necesaria para la API)
                    onSaved(fileName, output.savedUri)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Error al capturar: ${exc.message}")
                    onError("Error en captura: ${exc.message}")
                }
            }
        )
    }
}