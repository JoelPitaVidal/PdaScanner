package com.example.pdascanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pdascanner.barcodeanalyzer.BarcodeAnalyzer

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var txtResultado: TextView
    private lateinit var cameraController: LifecycleCameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.previewView)
        txtResultado = findViewById(R.id.txtResultado)

        if (allPermissionsGranted()) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    private fun setupCamera() {
        cameraController = LifecycleCameraController(baseContext)

        // Usamos nuestra nueva clase separada
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            BarcodeAnalyzer { resultado ->
                txtResultado.text = "Contenido: $resultado"
            }
        )

        cameraController.bindToLifecycle(this)
        viewFinder.controller = cameraController

        // Aplicamos el arreglo de la cámara invertida
        viewFinder.post {
            viewFinder.scaleX = -1f
            viewFinder.scaleY = -1f
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && allPermissionsGranted()) {
            setupCamera()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }
}