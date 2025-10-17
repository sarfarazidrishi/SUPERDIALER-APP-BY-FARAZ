package com.example.superdialer.ui.theme

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superdialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    // Cache keys
    private val PREFS_NAME = "dialer_cache_v2"
    private val KEY_CONTACTS = "cached_contacts"
    private val KEY_NOTE_COUNTS = "cached_note_counts"
    private val KEY_CALL_HISTORY = "cached_call_history"

    // Observe notes reactively (instant badge updates)
    fun observeNotes(db: NotesDatabase) {
        viewModelScope.launch {
            // if your NotesDao exposes getAllNotes(): Flow<List<NoteEntity>>
            try {
                db.notesDao().getAllNotes().collect { notes ->
                    val counts = notes.groupBy { it.phoneNumber }.mapValues { it.value.size }
                    _noteCounts.value = counts
                }
            } catch (_: Exception) {
                // swallow — keep app stable
            }
        }
    }

//    fun observeTags(db: NotesDatabase) {
//        viewModelScope.launch {
//            db.tagsDao().observeAllTags()
//                .distinctUntilChanged()
//                .collectLatest { tagsList ->
//                    _allTags.value = tagsList
//                }
//        }
//    }

    fun observeTags(db: NotesDatabase) {
        viewModelScope.launch {
            // 🌈 Collect live list of tag names for the top TagFilterRow
            db.tagsDao().observeAllTags()
                .distinctUntilChanged()
                .collectLatest { tagsList ->
                    _allTags.value = tagsList
                }
        }

        viewModelScope.launch {
            // 🌈 Collect live list of tag entities to build number -> tag map
            db.tagsDao().observeAllTagEntities()
                .distinctUntilChanged()
                .collectLatest { tagEntities ->
                    val tagMap = tagEntities.associate { it.phoneNumber to it.tag }
                    _tags.value = tagMap
                }
        }
    }


    fun updateTag(context: Context, number: String, newTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = NotesDatabase.getDatabase(context)
            db.tagsDao().updateTag(number, newTag)
            // ✅ Flows handle UI update automatically
        }
    }

    fun deleteTag(context: Context, number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = NotesDatabase.getDatabase(context)
            db.tagsDao().clearTagsForNumber(number)
            // ✅ No need to call refreshTags()
        }
    }


    // Load cached data quickly then refresh in background
    fun loadData(context: Context, db: NotesDatabase) {
        viewModelScope.launch {
            // read cached synchronously off main
            val cachedContacts = withContext(Dispatchers.IO) { readStringMapFromPrefs(context, KEY_CONTACTS) }
            val cachedNoteCounts = withContext(Dispatchers.IO) { readIntMapFromPrefs(context, KEY_NOTE_COUNTS) }
            val cachedHistory = withContext(Dispatchers.IO) { readCachedCallHistory(context) }

            if (cachedContacts.isNotEmpty()) _contactNames.value = cachedContacts
            if (cachedNoteCounts.isNotEmpty()) _noteCounts.value = cachedNoteCounts
            if (cachedHistory.isNotEmpty()) _callHistory.value = cachedHistory

            // refresh in background (IO) and then publish on Main
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val freshHistory = getCallHistory(context)
                    val freshContacts = loadContacts(context)
//                    val allTagsList = db.tagsDao().getAllTags()

                    // build tag map for numbers present in freshHistory
                    val tagMap = mutableMapOf<String, String?>()
                    val uniqueNumbers = freshHistory.map { it.number }.distinct()
                    for (n in uniqueNumbers) {
                        val tagsForNumber = db.tagsDao().getTagsForNumber(n)
                        tagMap[n] = tagsForNumber.firstOrNull()?.tag
                    }

                    // cache
                    writeCachedCallHistory(context, freshHistory)
                    writeStringMapToPrefs(context, KEY_CONTACTS, freshContacts)

                    withContext(Dispatchers.Main) {
                        _callHistory.value = freshHistory
                        _contactNames.value = freshContacts
//                        _allTags.value = allTagsList
                        _tags.value = tagMap
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
    }

    fun addTag(context: Context, number: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = NotesDatabase.getDatabase(context)
            val dao = db.tagsDao()

            dao.clearTagsForNumber(number)
            dao.insert(TagEntity(phoneNumber = number, tag = tag))

            // Now update per-number map for immediate UI sync
            val newMap = _tags.value.toMutableMap()
            newMap[number] = tag
            withContext(Dispatchers.Main) {
                _tags.value = newMap
            }
        }
    }



    fun addNote(context: Context, number: String, noteText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            NotesDatabase.getDatabase(context)
                .notesDao()
                .insert(NoteEntity(phoneNumber = number, note = noteText))
            // noteCounts will update automatically via observeNotes collector
        }
    }

     //Public helper to refresh tags map and tag list (safe to call from UI)
    fun refreshTags(   db: NotesDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val all = db.tagsDao().getAllTags()
                val numbers = _callHistory.value.map { it.number }.distinct()
                val map = mutableMapOf<String, String?>()
                for (n in numbers) {
                    val t = db.tagsDao().getTagsForNumber(n).firstOrNull()?.tag
                    map[n] = t
                }
                withContext(Dispatchers.Main) {
                    _allTags.value = all
                    _tags.value = map
                }
            } catch (_: Exception) { }
        }
    }


    // ---------- Cache Helpers ----------
    private fun readCachedCallHistory(context: Context): List<CallEntry> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CALL_HISTORY, null) ?: return emptyList()
            deserializeCallList(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeCachedCallHistory(context: Context, history: List<CallEntry>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CALL_HISTORY, serializeCallList(history)).apply()
        } catch (_: Exception) {
        }
    }

    private fun writeStringMapToPrefs(context: Context, key: String, map: Map<String, String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString(key, json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun readStringMapFromPrefs(context: Context, key: String): Map<String, String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(key, null) ?: return emptyMap()
            val json = JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = json.optString(k, "")
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeIntMapToPrefs(context: Context, key: String, map: Map<String, Int>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString(key, json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun readIntMapFromPrefs(context: Context, key: String): Map<String, Int> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(key, null) ?: return emptyMap()
            val json = JSONObject(raw)
            val out = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = json.optInt(k, 0)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // Keep serialisation string-based to avoid mismatches
    private fun serializeCallList(list: List<CallEntry>): String =
        list.joinToString("§") { "${it.number}|${it.type}|${it.date}|${it.duration}" }

    private fun deserializeCallList(raw: String): List<CallEntry> =
        raw.split("§").mapNotNull {
            val parts = it.split("|")
            if (parts.size < 4) null
            else CallEntry(
                number = parts[0],
                type = parts[1],
                date = parts[2],
                duration = parts[3]
            )
        }

    private fun loadContacts(context: Context): Map<String, String> {
        val map = mutableMapOf<String, String>()
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
                val num = it.getString(0)?.replace(" ", "")?.replace("-", "") ?: ""
                val name = it.getString(1) ?: ""
                if (num.isNotBlank()) map[num] = name
            }
        }
        return map
    }



}
