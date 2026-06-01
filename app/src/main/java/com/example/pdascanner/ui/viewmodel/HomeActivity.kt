package com.example.pdascanner.ui.viewmodel

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pdascanner.MainActivity
import com.example.pdascanner.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Forzamos orientación vertical para mantener simetría con el escáner
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Evento para abrir el escáner al pulsar el botón
        binding.btnAccederEscaner.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}