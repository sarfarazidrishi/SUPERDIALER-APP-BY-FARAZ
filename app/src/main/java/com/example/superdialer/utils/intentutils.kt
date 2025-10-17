package com.example.superdialer.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openWhatsApp(context: Context, number: String) {
    val cleanNumber = number.replace("+", "").replace(" ", "")
    val uri = Uri.parse("https://wa.me/$cleanNumber")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.whatsapp") }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}

fun openSmsApp(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Can't open messaging app", Toast.LENGTH_SHORT).show()
    }
}

fun openVideoCall(context: Context, number: String) {
    // Try Google Dialer video activity first, fallback to tel: URI (dialer)
    try {
        // Intent that may trigger video call UI in some dialers (best-effort)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("tel:$number")
            // This attempt uses the Google Dialer package/class — may not exist on all devices.
            setClassName("com.google.android.dialer", "com.android.dialer.VideoCallActivity")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: open regular dialer with number (user can use video call if supported)
        try {
            val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(fallback)
        } catch (_: Exception) {
            Toast.makeText(context, "No dialer available", Toast.LENGTH_SHORT).show()
        }
    }
}
