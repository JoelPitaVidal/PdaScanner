package com.example.pdascanner.barcodeanalyzer

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Convertimos la imagen de CameraX al formato que entiende ML Kit
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            onBarcodeDetected(rawValue)
                            break // Nos quedamos con el primer código válido detectado
                        }
                    }
                }
                .addOnFailureListener {
                    // Aquí puedes registrar el error si lo deseas
                }
                .addOnCompleteListener {
                    // ¡ESTO ES LO MÁS IMPORTANTE!
                    // Se ejecuta siempre (tanto si detecta QR como si falla o da error)
                    // para liberar el buffer y permitir que entre el siguiente frame.
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}