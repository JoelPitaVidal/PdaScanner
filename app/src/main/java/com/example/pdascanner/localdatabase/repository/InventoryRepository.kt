package com.example.pdascanner.localdatabase.repository

import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.AlbaranDao
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.FotoDao
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val albaranDao: AlbaranDao, private val fotoDao: FotoDao) {

    suspend fun insertarAlbaran(albaran: Albaran): Long {
        return albaranDao.insert(albaran) // Ya no debería estar en rojo
    }

    suspend fun obtenerAlbaranPorTransporte(at: String): Albaran? {
        return albaranDao.getAlbaranPorTransporte(at)
    }

    suspend fun guardarFoto(foto: Foto) {
        fotoDao.insert(foto)
    }

    fun obtenerFotosDeAlbaran(albaranId: Long): Flow<List<Foto>> {
        return fotoDao.getFotosByAlbaran(albaranId)
    }
}