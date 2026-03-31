package com.example.pdascanner.localdatabase.appdatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pdascanner.localdatabase.AlbaranDao
import com.example.pdascanner.localdatabase.FotoDao
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.Albaran

// 1. Definimos las entidades y la versión
@Database(entities = [Albaran::class, Foto::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    // 2. Conectamos los DAOs
    abstract fun albaranDao(): AlbaranDao
    abstract fun fotoDao(): FotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 3. Patrón Singleton para que solo haya una instancia de la BD
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pda_scanner_db" // Nombre del archivo de la base de datos
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}