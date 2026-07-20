package com.example.pdascanner.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdascanner.api.RetrofitClient
import com.example.pdascanner.api.response.AlbaranResponse
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.appdatabase.AppDatabase
import com.example.pdascanner.localdatabase.repository.InventoryRepository
import com.example.pdascanner.sesionmanager.SesionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.getInstance(getApplication())
    private val repository: InventoryRepository = AppDatabase.getDatabase(application).let {
        InventoryRepository(it.albaranDao(), it.fotoDao())
    }

    val fotosPendientes: LiveData<Int> = repository.fotosPendientes
    val datosAlbaranActual = MutableLiveData<AlbaranResponse?>()
    val resultadosBusqueda = MutableLiveData<List<Albaran>>()

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
        return mbLibres > 100
    }

    fun buscarAlbaranesLocales(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val textoLimpio = "%${query.trim()}%"
                val lista = repository.buscarAlbaranes(textoLimpio)
                withContext(Dispatchers.Main) {
                    resultadosBusqueda.value = lista
                }
            } catch (e: Exception) {
                Log.e("SEARCH_ERROR", "Error: ${e.message}")
            }
        }
    }

    fun procesarCaptura(nombreFichero: String, qr: String, uri: Uri?) {
        viewModelScope.launch {
            try {
                if (!tieneEspacioSuficiente()) {
                    estadoEscaneo.postValue(ScanState.Error("MEMORIA LLENA"))
                    return@launch
                }

                val albaranExistente = repository.obtenerAlbaranUniversal(qr)
                if (albaranExistente == null) {
                    // CAMBIO: Se guardaba "DESCONOCIDO", ahora se guarda vacío para no ensuciar la UI
                    repository.insertarAlbaran(Albaran(codigoCliente = "", codigoTransporte = qr, fecha = System.currentTimeMillis()))
                }

                val foto = Foto(
                    nombreFichero = nombreFichero,
                    qrCodigo = qr,
                    fecha = System.currentTimeMillis(),
                    uri = uri?.toString() ?: "",
                    subida = false
                )

                val fotoId = repository.guardarFoto(foto)
                val totalFotos = repository.getConteoFotos(qr)
                estadoEscaneo.postValue(ScanState.Guardado(nombreFichero, totalFotos, qr))
                encolarFotoParaSubida(fotoId, foto)

            } catch (e: Exception) {
                estadoEscaneo.postValue(ScanState.Error("Error: ${e.message}"))
            }
        }
    }

    private fun encolarFotoParaSubida(fotoId: Long, foto: Foto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val archivo = uriToFile(Uri.parse(foto.uri), foto.nombreFichero)
                if (archivo != null && archivo.exists()) {
                    val subido = subirFotoServidor(archivo, foto.qrCodigo)
                    if (subido) {
                        repository.marcarFotoComoSubida(fotoId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ENCOLAR_ERROR", "Error: ${e.message}")
            }
        }
    }

    private suspend fun subirFotoServidor(archivo: File, qr: String): Boolean {
        return try {

            val md5 = calcularMD5(archivo)
            Log.d("UPLOAD_CHECK", "MD5 del archivo a subir: $md5")

            val requestFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", archivo.name, requestFile)

            val userId = SesionManager.getUserId(getApplication())

            val response = apiService.subirFotoAlbaran(
                codigoAlbaran = qr,
                usuarioId = userId,
                file = body
            )

            response.isSuccessful && (response.body()?.success == true)

        } catch (e: Exception) {
            Log.e("UPLOAD_ERROR", "Excepción al subir foto: ${e.message}")
            false
        }
    }

    fun aceptarCodigoEscaneado(codigo: String) {
        estadoEscaneo.postValue(ScanState.Valido(codigo.trim().uppercase()))
    }

    private fun uriToFile(uri: Uri, nombreFichero: String): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val file = File(context.cacheDir, nombreFichero)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val outputStream = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()
                file
            } else null
        } catch (e: Exception) { null }
    }

    fun sincronizarFotos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.procesarColaDeSubida(context, apiService)
        }
    }

    fun reiniciarEscaneo() {
        estadoEscaneo.value = ScanState.Idle
    }

    private fun calcularMD5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192) // Leer en bloques de 8KB

        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        // Convertir los bytes del hash a formato hexadecimal
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

}