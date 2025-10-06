package com.example.superdialer.data

import android.content.Context
import android.provider.CallLog
import java.util.*

data class CallEntry(
    val number: String,
    val type: String,
    val date: String,
    val duration: String,
    val tag: String? = null,      // 🏷 Tag (Zepto, Amazon, etc.)
    val notes: MutableList<String> = mutableListOf() // 📝 Notes
)

fun getCallHistory(context: Context): List<CallEntry> {
    val callList = mutableListOf<CallEntry>()
    val resolver = context.contentResolver

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        CallLog.Calls.DATE + " DESC"
    )

    cursor?.use {
        val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
        val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
        val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

        while (it.moveToNext()) {
            val number = it.getString(numberIdx)?:"unknown"
            val date = Date(it.getLong(dateIdx)).toString()
            val duration = it.getString(durationIdx)+"s"

            val typeInt = it.getInt(typeIndex)
            val type = when (typeInt) {
                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                CallLog.Calls.MISSED_TYPE -> "Missed"
                else -> "Other"
            }

            callList.add(CallEntry(number, type, date, "$duration sec"))
        }
    }
    return callList
}
