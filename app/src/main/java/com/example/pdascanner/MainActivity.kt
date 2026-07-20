package com.example.pdascanner

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdascanner.cameraManager.CameraManager
import com.example.pdascanner.databinding.ActivityMainBinding
import com.example.pdascanner.permissionmanager.PermissionManager
import com.example.pdascanner.sesionmanager.SesionManager
import com.example.pdascanner.ui.viewmodel.InventoryViewModel
import com.example.pdascanner.ui.viewmodel.InventoryViewModel.ScanState
import com.example.pdascanner.ui.viewmodel.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraManager: CameraManager? = null
    private val inventoryViewModel: InventoryViewModel by viewModels()
    private var lastQr: String? = null

    // Flag para saber si la cámara ya se configuró
    private var isCameraInitialized = false

    @Volatile private var isProcessing: Boolean = false
    @Volatile private var isScanPaused: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = SesionManager.getUserId(this)
        if (userId == null || userId == -1) {
            irALogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        cameraManager = CameraManager(this, binding.previewView)
        setupObservers()

        binding.btnCapturar.setOnClickListener { ejecutarCaptura() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubirFotos.setOnClickListener { sincronizarFotos() }
        binding.txtResultado.setOnClickListener { reestablecerEscaner() }

        // Iniciamos el flujo aquí
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        // No llamamos a checkPermissions aquí para evitar bucles de reinicio
        // Si necesitas re-validar permisos, hazlo solo si es necesario
    }

    override fun onPause() {
        super.onPause()
        cameraManager?.unbind()
        isCameraInitialized = false // Marcamos que la cámara está "cerrada" al pausar
    }

    private fun checkPermissions() {
        val pm = PermissionManager(this)
        if (pm.allPermissionsGranted()) {
            startFlow()
        } else {
            pm.requestPermissions(10)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && PermissionManager(this).allPermissionsGranted()) {
            startFlow()
        }
    }

    private fun startFlow() {
        if (isCameraInitialized) return // Evita múltiples inicializaciones

        cameraManager?.setupCamera(this) { qr ->
            // Si está pausado (ya encontró un código) o procesando, no hacemos nada
            if (isScanPaused || isProcessing) return@setupCamera

            val limpio = qr.trim().uppercase()
            runOnUiThread {
                inventoryViewModel.aceptarCodigoEscaneado(limpio)
            }
        }
        isCameraInitialized = true
    }

    private fun ejecutarCaptura() {
        val qrParaCapturar = lastQr
        if (isProcessing || qrParaCapturar == null) return

        isProcessing = true
        actualizarUI("PROCESANDO FOTO...", Color.parseColor("#F57C00"), true)

        cameraManager?.takePhoto(qrParaCapturar, { name, uri ->
            inventoryViewModel.procesarCaptura(name, qrParaCapturar, uri)

            runOnUiThread {
                isProcessing = false
                // Feedback visual: confirmamos que se guardó
                // Ya no reseteamos el escáner automáticamente
                actualizarUI("FOTO GUARDADA. DOC: $qrParaCapturar", Color.parseColor("#2E7D32"), false)
            }
        }, { err ->
            runOnUiThread {
                isProcessing = false
                actualizarUI("ERROR. PULSE AQUÍ PARA RE-ESCANEAR", Color.RED, false)
            }
        })
    }

    private fun reestablecerEscaner() {
        lastQr = null
        isProcessing = false
        isScanPaused = false // Esto permite que el callback de cámara vuelva a pasar el IF

        inventoryViewModel.reiniciarEscaneo()
        actualizarUI("ESCANEE UN CÓDIGO", Color.DKGRAY, true)
    }

    private fun actualizarUI(msg: String, color: Int, processing: Boolean) {
        binding.txtResultado.text = msg
        binding.txtResultado.setBackgroundColor(color)
        binding.btnCapturar.isEnabled = !processing
    }

    private fun setupObservers() {
        inventoryViewModel.estadoEscaneo.observe(this) { estado ->
            if (estado is ScanState.Valido) {
                isScanPaused = true // Bloqueamos la entrada de nuevos códigos
                lastQr = estado.codigo
                actualizarUI("CÓDIGO: ${estado.codigo}", Color.parseColor("#1565C0"), false)
            } else if (estado is ScanState.Error) {
                actualizarUI(estado.mensaje, Color.RED, false)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_CAMERA) {
            ejecutarCaptura()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun sincronizarFotos() {
        Toast.makeText(this, "Sincronizando fotos...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            inventoryViewModel.sincronizarFotos(this@MainActivity)
        }
    }
}