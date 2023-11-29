package com.example.mealintju.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface mealInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg mealInfos: mealInfo)

    @Delete
    suspend fun delete(user: mealInfo)

    @Update
    suspend fun update(vararg mealInfos: mealInfo)

    @Query("SELECT * FROM mealInfo")
    suspend fun getAll(): List<mealInfo>
}