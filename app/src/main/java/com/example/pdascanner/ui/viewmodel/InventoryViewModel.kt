package com.example.pdascanner.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdascanner.api.RetrofitClient
import com.example.pdascanner.api.response.AlbaranResponse // Asegúrate de tener este import
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.appdatabase.AppDatabase
import com.example.pdascanner.localdatabase.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository = AppDatabase.getDatabase(application).let {
        InventoryRepository(it.albaranDao(), it.fotoDao())
    }

    // Exponemos el LiveData del repositorio para que MainActivity pinte los bultos pendientes
    val fotosPendientes: LiveData<Int> = repository.fotosPendientes

    // Añadimos este LiveData que faltaba para la respuesta remota del albarán de la API
    val datosAlbaranActual = MutableLiveData<AlbaranResponse?>()

    sealed class ScanState {
        object Idle : ScanState()
        object Buscando : ScanState()
        data class Valido(val codigo: String) : ScanState()
        data class Error(val mensaje: String) : ScanState()
        data class Guardado(val nombre: String, val totalFotos: Int, val qr: String) : ScanState()
    }

    val estadoEscaneo = MutableLiveData<ScanState>(ScanState.Idle)

    private fun tieneEspacioSuficiente(): Boolean {
        val path = getApplication<Application>().filesDir
        val espacioLibreBytes = path.freeSpace
        val mbLibres = espacioLibreBytes / (1024 * 1024)

        Log.d("STORAGE", "Espacio disponible: $mbLibres MB")

        // Retornamos falso si queda menos de 100MB por seguridad
        return mbLibres > 100
    }

    fun procesarCaptura(nombreArchivo: String, qr: String, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. COMPROBACIÓN CRÍTICA DE ESPACIO
            if (!tieneEspacioSuficiente()) {
                estadoEscaneo.postValue(ScanState.Error("MEMORIA LLENA: Libera espacio en la PDA"))
                return@launch
            }

            try {
                // A. GUARDAR LOCALMENTE
                // CORREGIDO: Se quita 'albaranId' y se instancia según tu Foto.kt real
                val nuevaFoto = Foto(
                    nombreFichero = nombreArchivo,
                    qrCodigo = qr,
                    fecha = System.currentTimeMillis(),
                    uri = uri?.toString() ?: "",
                    subida = false
                )

                val idGenerado = repository.guardarFoto(nuevaFoto)
                val totalFotos = repository.getConteoFotos(qr)

                estadoEscaneo.postValue(ScanState.Guardado(nombreArchivo, totalFotos, qr))

                // B. INTENTAR SUBIDA AL SERVIDOR
                uri?.let {
                    val archivo = uriToFile(it)
                    if (archivo != null) {
                        val exito = subirFotoServidor(archivo, qr, nombreArchivo)
                        if (exito) {
                            // CORREGIDO: Pasamos idGenerado directamente (es Long de origen)
                            repository.marcarFotoComoSubida(idGenerado)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PROCESS", "Error: ${e.message}")
                estadoEscaneo.postValue(ScanState.Error("Error de escritura: ${e.message}"))
            }
        }
    }

    private suspend fun subirFotoServidor(archivo: File, qr: String, nombre: String): Boolean {
        return try {
            val requestFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", archivo.name, requestFile)
            val qrBody = qr.toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = RetrofitClient.instance.subirImagen(body, qrBody, nombreBody)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    fun procesarCodigo(codigo: String) {
        val limpio = codigo.trim().uppercase()
        estadoEscaneo.postValue(ScanState.Buscando)

        // Ejecuta la petición al servidor Python para verificar la existencia del albarán
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.consultarAlbaran(limpio)
                if (response.isSuccessful && response.body() != null) {
                    val albaranInfo = response.body()!!
                    withContext(Dispatchers.Main) {
                        datosAlbaranActual.value = albaranInfo
                        if (albaranInfo.success) {
                            estadoEscaneo.value = ScanState.Valido(limpio)
                        } else {
                            estadoEscaneo.value = ScanState.Error(albaranInfo.message ?: "No existe")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("API_DEB_ERR", "Código HTTP: ${response.code()} | Error crudo: ${response.errorBody()?.string()}")

                        datosAlbaranActual.value = AlbaranResponse(success = false, message = "Error de red", cliente = null, estado = null, fecha_creacion = null, total_bultos = null)
                        estadoEscaneo.value = ScanState.Error("Error de servidor")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    datosAlbaranActual.value = AlbaranResponse(success = false, message = e.message, cliente = null, estado = null, fecha_creacion = null, total_bultos = null)
                    estadoEscaneo.value = ScanState.Error("Sin conexión con el almacén")
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "upload_temp.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}