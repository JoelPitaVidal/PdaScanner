package com.example.pdascanner.localdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albaranes")
data class Albaran(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigoCliente: String,    // Aquí va el "AC..."
    val codigoTransporte: String, // Aquí va el "AT..."
    val fecha: Long = System.currentTimeMillis()
)