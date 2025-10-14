package com.example.superdialer.ui.theme

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superdialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialerViewModel : ViewModel() {

    private val _callHistory = MutableStateFlow<List<CallEntry>>(emptyList())
    val callHistory = _callHistory.asStateFlow()

    private val _noteCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val noteCounts = _noteCounts.asStateFlow()

    private val _tags = MutableStateFlow<Map<String, String?>>(emptyMap())
    val tags = _tags.asStateFlow()

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags = _allTags.asStateFlow()

    private val _contactNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val contactNames = _contactNames.asStateFlow()

    fun loadData(context: Context, db: NotesDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            val callHistoryList = getCallHistory(context)
            val notesDao = db.notesDao()
            val tagsDao = db.tagsDao()

            val notesCountMap = callHistoryList.associate { it.number to notesDao.getNotesForNumber(it.number).size }
            val tagMap = callHistoryList.associate { it.number to tagsDao.getTagsForNumber(it.number).firstOrNull()?.tag }
            val allTagsList = tagsDao.getAllTags().filter { it.isNotBlank() }
            val contactMap = mutableMapOf<String, String>()

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(0)?.replace(" ", "")?.replace("-", "") ?: ""
                    val name = it.getString(1) ?: ""
                    if (number.isNotBlank()) contactMap[number] = name
                }
            }

            withContext(Dispatchers.Main) {
                _callHistory.value = callHistoryList
                _noteCounts.value = notesCountMap
                _tags.value = tagMap
                _allTags.value = allTagsList
                _contactNames.value = contactMap
            }
        }
    }

    fun addTag(context: Context, number: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = NotesDatabase.getDatabase(context)
            val dao = db.tagsDao()
            dao.clearTagsForNumber(number)
            dao.insert(TagEntity(phoneNumber = number, tag = tag))
            val updatedTags = dao.getAllTags()
            withContext(Dispatchers.Main) {
                _allTags.value = updatedTags
            }
        }
    }

    fun addNote(context: Context, number: String, noteText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = NotesDatabase.getDatabase(context)
            db.notesDao().insert(NoteEntity(phoneNumber = number, note = noteText))
            _noteCounts.value = _noteCounts.value.toMutableMap().apply {
                this[number] = (this[number] ?: 0) + 1
            }
        }
    }
}
