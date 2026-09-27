package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM income_sources ORDER BY createdAt DESC")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources")
    suspend fun getAllIncomeSourcesOnce(): List<IncomeSourceEntity>

    @Query("SELECT * FROM income_sources WHERE status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    fun getIncomeSourceById(id: Long): Flow<IncomeSourceEntity?>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    suspend fun getIncomeSourceByIdOnce(id: Long): IncomeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(entity: IncomeSourceEntity): Long

    @Update
    suspend fun updateIncomeSource(entity: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(entity: IncomeSourceEntity)

    @Query("UPDATE income_sources SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE income_sources SET effectiveAmountChangeDate = :effectiveDate, newAmount = :newAmount WHERE id = :id")
    suspend fun updateEffectiveAmount(id: Long, effectiveDate: String, newAmount: Double)
}
