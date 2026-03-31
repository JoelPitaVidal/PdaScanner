package com.example.pdascanner.localdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fotos")
data class Foto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L, // Cambiado a Long
    val albaranId: Long,       // Cambiado a Long para coincidir con Albaran
    val nombreFichero: String,
    val qrCodigo: String,
    val fecha: Long,
    val uri: String
)