package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.PUT

@JsonClass(generateAdapter = true)
data class Spreadsheet(
    val spreadsheetId: String,
    val sheets: List<Sheet>
)

@JsonClass(generateAdapter = true)
data class Sheet(
    val properties: SheetProperties
)

@JsonClass(generateAdapter = true)
data class SheetProperties(
    val sheetId: Int,
    val title: String
)

@JsonClass(generateAdapter = true)
data class ValueRange(
    val range: String,
    val majorDimension: String?,
    val values: List<List<String>>?
)

interface GoogleSheetsApi {
    @GET("v4/spreadsheets/{spreadsheetId}")
    suspend fun getSpreadsheet(
        @Path("spreadsheetId") spreadsheetId: String,
        @Header("Authorization") authHeader: String
    ): Spreadsheet

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getSheetValues(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Header("Authorization") authHeader: String
    ): ValueRange

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}?valueInputOption=USER_ENTERED")
    suspend fun updateSheetValues(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Header("Authorization") authHeader: String,
        @Body body: ValueRange
    ): Any
}