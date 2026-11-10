package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityLog
import com.example.data.model.SharedFile
import com.example.data.model.SimplexServer
import com.example.data.model.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: Int)

    @Query("DELETE FROM vault_items")
    suspend fun clearAllVault()
}

@Dao
interface SimplexDao {
    @Query("SELECT * FROM simplex_servers ORDER BY id ASC")
    fun getAllServers(): Flow<List<SimplexServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: SimplexServer)

    @Update
    suspend fun updateServer(server: SimplexServer)

    @Query("DELETE FROM simplex_servers WHERE id = :id")
    suspend fun deleteServer(id: Int)
}

@Dao
interface SharedFileDao {
    @Query("SELECT * FROM shared_files ORDER BY timestamp DESC")
    fun getAllSharedFiles(): Flow<List<SharedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedFile(file: SharedFile)

    @Query("DELETE FROM shared_files WHERE id = :id")
    suspend fun deleteSharedFile(id: Int)

    @Query("SELECT * FROM shared_files WHERE isPinned = 0 ORDER BY lastAccessTimestamp ASC")
    suspend fun getUnpinnedFilesOrderByOldestAccess(): List<SharedFile>

    @Query("UPDATE shared_files SET lastAccessTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastAccessTime(id: Int, timestamp: Long)

    @Query("UPDATE shared_files SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 60")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
