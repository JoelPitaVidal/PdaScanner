package com.example.pdascanner.localdatabase.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.AlbaranDao
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.FotoDao

class InventoryRepository(
    private val albaranDao: AlbaranDao,
    private val fotoDao: FotoDao
) {

    // CORREGIDO: Convertimos el Flow del DAO a LiveData usando la extensión '.asLiveData()'
    val fotosPendientes: LiveData<Int> = fotoDao.observarFotosPendientes().asLiveData()

    // --- LÓGICA DE ALBARANES ---
    suspend fun obtenerAlbaranUniversal(codigo: String): Albaran? {
        return albaranDao.buscarAlbaranUniversal(codigo)
    }

    suspend fun insertarAlbaran(albaran: Albaran): Long {
        return albaranDao.insert(albaran)
    }

    suspend fun obtenerAlbaranPorTransporte(at: String): Albaran? {
        return albaranDao.getAlbaranPorTransporte(at)
    }

    // --- LÓGICA DE FOTOS (Sincronización) ---

    suspend fun guardarFoto(foto: Foto): Long {
        return fotoDao.insert(foto)
    }

    // CORREGIDO: Cambiado de Int a Long para que coincida con la entidad Foto
    suspend fun marcarFotoComoSubida(fotoId: Long) {
        fotoDao.marcarComoSubida(fotoId)
    }

    suspend fun obtenerFotosPendientes(): List<Foto> {
        return fotoDao.obtenerPendientesDeSubida()
    }

    suspend fun getConteoFotos(qr: String): Int {
        return fotoDao.contarFotosPorQr(qr)
    }
}