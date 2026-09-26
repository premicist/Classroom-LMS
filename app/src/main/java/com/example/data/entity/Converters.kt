package com.example.data.entity

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val adapter = moshi.adapter<Map<String, String>>(mapType)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        if (value == null) return "{}"
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value == null) return emptyMap()
        return try {
            adapter.fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}