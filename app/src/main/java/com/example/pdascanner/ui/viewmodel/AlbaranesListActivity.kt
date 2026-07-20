package com.example.pdascanner.ui.viewmodel

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdascanner.adapter.AlbaranAdapter
import com.example.pdascanner.databinding.ActivityAlbaranesListBinding

class AlbaranesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbaranesListBinding
    private val viewModel: AlbaranesListViewModel by viewModels()
    private lateinit var adapter: AlbaranAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbaranesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Forzar orientación vertical
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Configurar RecyclerView
        setupRecyclerView()

        // Observar cambios en los albaranes
        observarAlbaranes()

        // Configurar búsqueda
        setupSearchView()

        // Botón atrás
        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    /**
     * Configura el RecyclerView con su adapter
     */
    private fun setupRecyclerView() {
        adapter = AlbaranAdapter(emptyList())
        binding.recyclerAlbaranes.apply {
            layoutManager = LinearLayoutManager(this@AlbaranesListActivity)
            adapter = this@AlbaranesListActivity.adapter
        }
    }

    /**
     * Observa los cambios en los datos de albaranes
     */
    private fun observarAlbaranes() {
        // Observar albaranes filtrados (son los que se muestran)
        viewModel.albaranesFiltrados.observe(this) { albaranes ->
            adapter.actualizarLista(albaranes)
            
            // Mostrar mensaje "vacío" si no hay resultados
            if (albaranes.isEmpty()) {
                binding.recyclerAlbaranes.visibility = android.view.View.GONE
                binding.txtVacio.visibility = android.view.View.VISIBLE
            } else {
                binding.recyclerAlbaranes.visibility = android.view.View.VISIBLE
                binding.txtVacio.visibility = android.view.View.GONE
            }
        }

        // Observar albaranes totales (para inicializar)
        viewModel.albaranesConFotos.observe(this) { albaranes ->
            // Primera carga, mostrar todos
            if (albaranes.isNotEmpty() && binding.txtVacio.visibility == android.view.View.VISIBLE) {
                adapter.actualizarLista(albaranes)
                binding.recyclerAlbaranes.visibility = android.view.View.VISIBLE
                binding.txtVacio.visibility = android.view.View.GONE
            }
        }
    }

    /**
     * Configura el SearchView para filtrar albaranes en tiempo real
     */
    private fun setupSearchView() {
        binding.searchViewAlbaranes.setOnQueryTextListener(
            object : android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.filtrar(newText ?: "")
                    return true
                }
            }
        )
    }
}
