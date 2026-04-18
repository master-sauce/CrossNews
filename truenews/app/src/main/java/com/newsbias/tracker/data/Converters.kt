package com.newsbias.tracker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun toStringList(json: String): List<String> =
        gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter
    fun fromCrossMatchList(list: List<CrossMatch>): String = gson.toJson(list)

    @TypeConverter
    fun toCrossMatchList(json: String): List<CrossMatch> =
        gson.fromJson(json, object : TypeToken<List<CrossMatch>>() {}.type) ?: emptyList()
}