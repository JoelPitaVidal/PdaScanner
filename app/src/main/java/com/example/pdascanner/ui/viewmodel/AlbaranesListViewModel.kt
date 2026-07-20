package com.example.pdascanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.appdatabase.AppDatabase
import com.example.pdascanner.localdatabase.repository.InventoryRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AlbaranConFotos(
    val albaran: Albaran,
    val totalFotos: Int
)

class AlbaranesListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository: InventoryRepository = InventoryRepository(
        db.albaranDao(),
        db.fotoDao()
    )

    // LiveData con todos los albaranes
    private val _albaranesConFotos = MutableLiveData<List<AlbaranConFotos>>()
    val albaranesConFotos: LiveData<List<AlbaranConFotos>> = _albaranesConFotos

    // LiveData para los albaranes filtrados
    private val _albaranesFiltrados = MutableLiveData<List<AlbaranConFotos>>()
    val albaranesFiltrados: LiveData<List<AlbaranConFotos>> = _albaranesFiltrados

    // Texto de búsqueda
    private val _textoBusqueda = MutableLiveData<String>("")
    val textoBusqueda: LiveData<String> = _textoBusqueda

    init {
        cargarAlbaranes()
    }

    private fun cargarAlbaranes() {
        viewModelScope.launch {
            try {
                // Colectar el Flow directamente
                repository.observarTodosAlbaranes().collect { albaranes ->
                    // Para cada albarán, obtener el conteo de fotos
                    val resultado = albaranes.map { albaran ->
                        val totalFotos = repository.getConteoFotos(albaran.codigoTransporte)
                        AlbaranConFotos(albaran, totalFotos)
                    }

                    _albaranesConFotos.postValue(resultado)

                    // Aplicar filtro actual
                    filtrar(_textoBusqueda.value ?: "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun filtrar(texto: String) {
        _textoBusqueda.value = texto

        val listaActual = _albaranesConFotos.value ?: emptyList()

        val filtrados = if (texto.isEmpty()) {
            listaActual
        } else {
            listaActual.filter { item ->
                item.albaran.codigoTransporte.contains(texto, ignoreCase = true) ||
                        item.albaran.codigoCliente.contains(texto, ignoreCase = true)
            }
        }

        _albaranesFiltrados.postValue(filtrados)
    }
}