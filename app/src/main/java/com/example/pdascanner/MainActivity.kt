package com.example.pdascanner

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.pdascanner.cameraManager.CameraManager
import com.example.pdascanner.databinding.ActivityMainBinding
import com.example.pdascanner.permissionmanager.PermissionManager
import com.example.pdascanner.ui.viewmodel.InventoryViewModel
import com.example.pdascanner.ui.viewmodel.InventoryViewModel.ScanState

class MainActivity : AppCompatActivity() {

    // Usamos ViewBinding para eliminar los findViewById
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager

    // Kotlin permite inicializar el ViewModel en una línea
    private val inventoryViewModel: InventoryViewModel by viewModels()

    private var lastQr: String? = null
    private var isProcessing: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        cameraManager = CameraManager(this, binding.previewView)

        setupObservers()
        checkPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        inventoryViewModel.estadoEscaneo.observe(this) { estado ->
            when (estado) {
                is ScanState.Buscando -> actualizarUI("Buscando...", Color.BLUE, true)
                is ScanState.Valido -> {
                    lastQr = estado.codigo
                    actualizarUI("LISTO: ${estado.codigo}", Color.parseColor("#4CAF50"), false)
                    vibrar(100)
                }
                is ScanState.Error -> {
                    lastQr = null
                    actualizarUI(estado.mensaje, Color.RED, false)
                }
                is ScanState.Guardado -> {
                    actualizarUI("GUARDADO: ${estado.nombre}", Color.GREEN, false)
                    vibrar(50)
                }
                else -> {}
            }
        }
    }

    private fun actualizarUI(msg: String, color: Int, processing: Boolean) {
        binding.txtResultado.apply {
            text = msg
            setBackgroundColor(color)
        }
        isProcessing = processing
    }

    private fun checkPermissions() {
        val pm = PermissionManager(this)
        if (pm.allPermissionsGranted()) startFlow() else pm.requestPermissions(10)
    }

    private fun startFlow() {
        cameraManager.setupCamera(this) { qr ->
            if (lastQr != qr.trim().uppercase()) inventoryViewModel.procesarCodigo(qr)
        }
    }

    private fun ejecutarCaptura() {
        val qr = lastQr ?: return
        if (isProcessing) return

        actualizarUI("PROCESANDO...", Color.YELLOW, true)
        cameraManager.takePhoto(qr, { name, uri ->
            inventoryViewModel.procesarCaptura(name, qr, uri)
            inventoryViewModel.estadoEscaneo.postValue(ScanState.Guardado(name))
        }, { err ->
            inventoryViewModel.estadoEscaneo.postValue(ScanState.Error(err))
        })
    }

    // Extraemos la lógica de detección de gatillos para que sea más legible
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val esGatilloPushed = when (keyCode) {
            131, 285, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_CAMERA -> true
            else -> false
        }

        if (esGatilloPushed && lastQr != null) {
            ejecutarCaptura()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrar(ms: Long) {
        val v = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        val effect = android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
        v.vibrate(effect)
    }
}