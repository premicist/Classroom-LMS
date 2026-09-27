package com.example.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class Spreadsheet(
    val spreadsheetId: String,
    val sheets: List<Sheet>? = null
)

@JsonClass(generateAdapter = true)
data class Sheet(
    val properties: SheetProperties
)

@JsonClass(generateAdapter = true)
data class SheetProperties(
    val sheetId: Int? = null,
    val title: String
)

@JsonClass(generateAdapter = true)
data class ValueRange(
    val range: String,
    val majorDimension: String? = null,
    val values: List<List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class BatchUpdateSpreadsheetRequest(
    val requests: List<Request>
)

@JsonClass(generateAdapter = true)
data class Request(
    val addSheet: AddSheetRequest? = null
)

@JsonClass(generateAdapter = true)
data class AddSheetRequest(
    val properties: SheetProperties
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
        @Path(value = "range", encoded = true) range: String,
        @Header("Authorization") authHeader: String
    ): ValueRange

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}?valueInputOption=USER_ENTERED")
    suspend fun updateSheetValues(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path(value = "range", encoded = true) range: String,
        @Header("Authorization") authHeader: String,
        @Body body: ValueRange
    ): Any

    @POST("v4/spreadsheets/{spreadsheetId}:batchUpdate")
    suspend fun batchUpdate(
        @Path("spreadsheetId") spreadsheetId: String,
        @Header("Authorization") authHeader: String,
        @Body body: BatchUpdateSpreadsheetRequest
    ): Any
}
