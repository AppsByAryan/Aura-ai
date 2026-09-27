package com.example.aura.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RememberedPermissionDao {
    @Query("SELECT * FROM remembered_permissions ORDER BY grantedAtTimestamp DESC")
    fun getAllPermissions(): Flow<List<RememberedPermissionEntity>>

    @Query("SELECT COUNT(*) > 0 FROM remembered_permissions WHERE permissionKey = :key")
    suspend fun isPermissionGranted(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun grantPermission(permission: RememberedPermissionEntity)

    @Query("DELETE FROM remembered_permissions WHERE permissionKey = :key")
    suspend fun revokePermission(key: String)

    @Query("DELETE FROM remembered_permissions")
    suspend fun clearAll()
}
