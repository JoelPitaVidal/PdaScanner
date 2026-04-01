package com.example.pdascanner.localdatabase.repository

import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.AlbaranDao
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.FotoDao
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val albaranDao: AlbaranDao,
    private val fotoDao: FotoDao
) {

    // --- LÓGICA DE ALBARANES ---

    suspend fun insertarAlbaran(albaran: Albaran): Long {
        return albaranDao.insert(albaran)
    }

    suspend fun obtenerAlbaranPorTransporte(at: String): Albaran? {
        return albaranDao.getAlbaranPorTransporte(at)
    }

    // --- LÓGICA DE FOTOS (Sincronización) ---

    suspend fun guardarFoto(foto: Foto): Long {
        // Ahora fotoDao.insert(foto) devuelve el Long correctamente
        return fotoDao.insert(foto)
    }

    suspend fun marcarFotoComoSubida(fotoId: Int) {
        fotoDao.marcarComoSubida(fotoId)
    }

    suspend fun obtenerFotosPendientes(): List<Foto> {
        return fotoDao.obtenerPendientesDeSubida()
    }

    fun obtenerFotosDeAlbaran(albaranId: Long): Flow<List<Foto>> {
        // Asegúrate de que el DAO tenga esta función con este nombre exacto
        return fotoDao.getFotosByAlbaran(albaranId)
    }

    suspend fun getConteoFotos(qr: String): Int {
        return fotoDao.contarFotosPorQr(qr)
    }

    suspend fun obtenerFotosNoSubidas(): List<Foto> {
        return fotoDao.obtenerPendientesDeSubida()
    }
}