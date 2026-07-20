package com.example.pdascanner.ui.viewmodel

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pdascanner.MainActivity
import com.example.pdascanner.databinding.ActivityHomeBinding
import com.example.pdascanner.sesionmanager.SesionManager

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Verificación de sesión
        if (SesionManager.getUserId(this) == null) {
            irALogin()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 2. Manejo del botón "atrás"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                realizarCierreSesion()
            }
        })

        // 3. Listeners
        binding.btnAccederEscaner.setOnClickListener {
            isNavigating = true
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnVerAlbaranes.setOnClickListener {
            isNavigating = true
            startActivity(Intent(this, AlbaranesListActivity::class.java))
        }

        // NUEVO: Listener para cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            realizarCierreSesion()
        }
    }

    // Función unificada para cerrar sesión
    private fun realizarCierreSesion() {
        SesionManager.cerrarSesion(this)
        irALogin()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        // Solo cerramos sesión si el usuario sale de la app sin haber navegado internamente
        if (isFinishing && !isNavigating) {
            SesionManager.cerrarSesion(this)
        }
        super.onDestroy()
    }
}