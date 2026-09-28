package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object CommunicationUtils {

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaGenericIntent(context, message)
        }
    }

    fun sendSmsMessage(context: Context, phoneNumber: String, message: String) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        try {
            val uri = Uri.parse("smsto:$cleanPhone")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaGenericIntent(context, message)
        }
    }

    fun shareViaGenericIntent(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, "Send Message to Parent via"))
    }
}
