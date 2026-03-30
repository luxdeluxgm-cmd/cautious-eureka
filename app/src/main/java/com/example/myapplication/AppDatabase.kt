package com.example.myapplication

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. Definicja Tabeli (Zastąpi starego JournalEntry z GameManager)
@Entity(tableName = "journal_entries")
data class RoomJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: Int,
    val date: String,
    val time: String = "",
    val imageUri: String? = null,
    val imageBase64: String? = null
)

// 2. DAO - Służy do komunikacji z bazą (zapisz/usuń/pobierz)
@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY id DESC")
    fun getAllEntries(): List<RoomJournalEntry>

    @Insert
    fun insert(entry: RoomJournalEntry)

    @Delete
    fun delete(entry: RoomJournalEntry)

    // Szybkie usuwanie całego dziennika w razie czego
    @Query("DELETE FROM journal_entries")
    fun deleteAll()
}

// 3. Właściwy silnik bazy danych
@Database(entities = [RoomJournalEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rpg_database"
                )
                    .allowMainThreadQueries() // Tymczasowo dla prostszej migracji
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}