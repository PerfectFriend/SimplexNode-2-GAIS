package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivityLog
import com.example.data.model.SharedFile
import com.example.data.model.SimplexServer
import com.example.data.model.VaultItem
import com.example.data.security.DbEncryptionManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

// [REAL] База данных Room с шифрованием на лету через SQLCipher (AES-256 в режиме CBC с HMAC-SHA512).
// [PROD-READY] База данных зашифрована на диске, открывается через SupportFactory и ключ DbEncryptionManager (Этап 1).
// [SECURITY-CRITICAL] Локальное хранилище данных приложения, паролей и криптографических метаданных.
@Database(
    entities = [VaultItem::class, ActivityLog::class, SimplexServer::class, SharedFile::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun simplexDao(): SimplexDao
    abstract fun sharedFileDao(): SharedFileDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!

                val appContext = context.applicationContext

                // Load native SQLCipher C/C++ engine binaries
                SQLiteDatabase.loadLibs(appContext)

                // Derive 256-bit AES encryption key for SQLCipher page-level encryption
                val passphraseBytes = DbEncryptionManager.getDatabasePassphraseBytes(appContext)
                val factory = SupportFactory(passphraseBytes)

                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "simplex_privacy_vault.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
