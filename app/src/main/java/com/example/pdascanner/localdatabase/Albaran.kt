package com.example.pdascanner.localdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albaranes")
data class Albaran(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigoCliente: String,    // El "AC"
    val codigoTransporte: String, // El "AT" <--- Asegúrate de que se llame así
    val fecha: Long = System.currentTimeMillis()
)