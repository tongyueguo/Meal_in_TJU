package com.example.mealintju.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["year", "month","day","mealNumber"])
data class mealInfo(
    @ColumnInfo(name = "year")
    var year: Int = 0,
    @ColumnInfo(name = "month")
    val month: Int = 0,
    @ColumnInfo(name = "day")
    val day: Int = 0,
    @ColumnInfo(name = "mealNumber")
    val mealNumber: Int = 0 ,
    @ColumnInfo(name = "canteenNumber")
    val canteenNumber: Int = 0,
    @ColumnInfo(name = "windowText")
    val windowText: String? = null ,
    @ColumnInfo(name = "result")
    val result: Int? = 0 ,
)