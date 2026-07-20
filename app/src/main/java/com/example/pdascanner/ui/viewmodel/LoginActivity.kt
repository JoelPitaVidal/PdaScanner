package com.example.pdascanner.ui.viewmodel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdascanner.R
import com.example.pdascanner.api.RetrofitClient
import com.example.pdascanner.api.response.UsuarioActivo
import com.example.pdascanner.sesionmanager.SesionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var spinnerUsuarios: Spinner
    private lateinit var btnConfirmar: Button
    private lateinit var progressBar: ProgressBar

    private var listaUsuarios: List<UsuarioActivo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        spinnerUsuarios = findViewById(R.id.spinnerUsuarios)
        btnConfirmar = findViewById(R.id.btnConfirmar)
        progressBar = findViewById(R.id.progressBar)

        cargarUsuariosDesdeApi()

        btnConfirmar.setOnClickListener {
            handleLogin()
        }
    }

    private fun cargarUsuariosDesdeApi() {
        progressBar.visibility = View.VISIBLE
        btnConfirmar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(this@LoginActivity).obtenerUsuariosActivos()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        listaUsuarios = response.body()!!

                        val nombres = listaUsuarios.map { it.nombre }

                        // APLICAMOS EL DISEÑO PERSONALIZADO AQUÍ
                        val adapter = ArrayAdapter(this@LoginActivity, R.layout.spinner_item_usuario, nombres)

                        // Aplicamos el mismo diseño al desplegable para mantener consistencia
                        adapter.setDropDownViewResource(R.layout.spinner_item_usuario)

                        spinnerUsuarios.adapter = adapter

                        Toast.makeText(this@LoginActivity, "Usuarios cargados", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error al cargar: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                }
            }
        }
    }

    private fun handleLogin() {
        val position = spinnerUsuarios.selectedItemPosition

        if (position in listaUsuarios.indices) {
            val seleccionado = listaUsuarios[position]

            SesionManager.saveUserSession(this, seleccionado.id, seleccionado.nombre)

            Toast.makeText(this, "Sesión iniciada: ${seleccionado.nombre}", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Por favor, selecciona un usuario", Toast.LENGTH_SHORT).show()
        }
    }
}