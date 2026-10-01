package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCapDao {

    @Query("SELECT * FROM budget_caps ORDER BY category ASC")
    fun getAllBudgetCaps(): Flow<List<BudgetCapEntity>>

    @Query("SELECT * FROM budget_caps ORDER BY category ASC")
    suspend fun getAllBudgetCapsOnce(): List<BudgetCapEntity>

    @Query("SELECT * FROM budget_caps WHERE category = :category LIMIT 1")
    suspend fun getBudgetCapForCategory(category: String): BudgetCapEntity?

    @Query("SELECT * FROM budget_caps WHERE id = :id LIMIT 1")
    suspend fun getBudgetCapById(id: Long): BudgetCapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetCap(budgetCap: BudgetCapEntity): Long

    @Update
    suspend fun updateBudgetCap(budgetCap: BudgetCapEntity)

    @Query("DELETE FROM budget_caps WHERE id = :id")
    suspend fun deleteBudgetCapById(id: Long)

    @Query("DELETE FROM budget_caps WHERE category = :category")
    suspend fun deleteBudgetCapByCategory(category: String)
}
