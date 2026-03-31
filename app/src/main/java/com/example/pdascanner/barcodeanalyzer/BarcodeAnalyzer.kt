package com.example.pdascanner.barcodeanalyzer

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_CODE_128, // Añadido por si tu PDA usa códigos de barras estándar
            Barcode.FORMAT_EAN_13
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    // Variable para evitar escaneos duplicados infinitos del mismo código
    private var lastDetected: String? = null

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            // Pasamos la rotación que nos da CameraX directamente a ML Kit
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.firstOrNull()
                        val code = barcode?.rawValue ?: barcode?.displayValue

                        if (code != null && code != lastDetected) {
                            lastDetected = code
                            Log.d("BarcodeAnalyzer", "Código detectado: $code")

                            // IMPORTANTE: Si la app se cierra aquí, es por el error de Room.
                            // Asegúrate de haber actualizado la versión de la base de datos.
                            onBarcodeDetected(code)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeAnalyzer", "Fallo al procesar imagen: ${e.message}")
                }
                .addOnCompleteListener {
                    // Cierra siempre el proxy para liberar el buffer y recibir el siguiente frame
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    // Método opcional para resetear el último detectado si necesitas escanear lo mismo dos veces
    fun resetLastDetected() {
        lastDetected = null
    }
}