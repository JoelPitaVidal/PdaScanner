package com.example.pdascanner.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FotoDao {

    // Al devolver Long, Room nos da el ID autogenerado tras la inserción
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foto: Foto): Long

    // CORREGIDO: En tu entidad 'subida' es un booleano (en SQLite se evalúa como 0 o 1)
    @Query("SELECT * FROM fotos WHERE subida = 0")
    suspend fun obtenerPendientesDeSubida(): List<Foto>

    // CORREGIDO: Actualiza el estado de subida usando el ID tipo Long
    @Query("UPDATE fotos SET subida = 1 WHERE id = :fotoId")
    suspend fun marcarComoSubida(fotoId: Long)

    // CORREGIDO: Cambiado 'albaranId' por 'qrCodigo', que es el nombre real en tu Foto.kt
    @Query("SELECT * FROM fotos WHERE qrCodigo = :albaranId")
    fun getFotosByAlbaran(albaranId: String): Flow<List<Foto>>

    // CORREGIDO: Cambiado 'qr_asociado' por 'qrCodigo', que es tu columna real
    @Query("SELECT COUNT(*) FROM fotos WHERE qrCodigo = :qr")
    suspend fun contarFotosPorQr(qr: String): Int

    // Añade esto dentro de tu interfaz FotoDao
    @Query("SELECT COUNT(*) FROM fotos WHERE subida = 0")
    fun observarFotosPendientes(): kotlinx.coroutines.flow.Flow<Int>

    @Query("UPDATE fotos SET subida = 1 WHERE id = :id")
    suspend fun marcarFotoComoSubida(id: Long)
}