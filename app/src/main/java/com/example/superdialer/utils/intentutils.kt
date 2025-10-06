package com.example.superdialer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openWhatsApp(context: Context, number: String) {
    val cleanNumber = number.replace("+", "").replace(" ", "")
    val uri = Uri.parse("https://wa.me/$cleanNumber")

    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.whatsapp")

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}
