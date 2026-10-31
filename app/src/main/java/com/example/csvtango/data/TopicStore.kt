package com.example.csvtango.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** 주제·폴더 목록을 기기에 저장한다. (웹 버전의 localStorage 자리) */
class TopicStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<Topic> {
        val raw = prefs.getString(KEY_TOPICS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toTopic() }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun save(topics: List<Topic>) {
        val array = JSONArray()
        topics.forEach { array.put(it.toJson()) }
        prefs.edit { putString(KEY_TOPICS, array.toString()) }
    }

    fun loadFolders(): List<Folder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toFolder() }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun saveFolders(folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { array.put(it.toJson()) }
        prefs.edit { putString(KEY_FOLDERS, array.toString()) }
    }

    private fun Folder.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toFolder() = Folder(
        id = optString("id"),
        name = optString("name"),
        createdAt = optLong("createdAt"),
    )

    private fun Topic.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        if (folderId != null) put("folderId", folderId)
        put("cards", JSONArray().apply { cards.forEach { put(it.toJson()) } })
    }

    private fun Card.toJson() = JSONObject().apply {
        put("jp", jp)
        put("kana", kana)
        put("ko", ko)
        put("exJp", exJp)
        put("exKana", exKana)
        put("exKo", exKo)
        put("bookmarked", bookmarked)
    }

    private fun JSONObject.toTopic(): Topic {
        val cardsJson = optJSONArray("cards") ?: JSONArray()
        return Topic(
            id = optString("id"),
            title = optString("title"),
            createdAt = optLong("createdAt"),
            folderId = optString("folderId").ifEmpty { null },
            cards = (0 until cardsJson.length()).mapNotNull { cardsJson.optJSONObject(it)?.toCard() },
        )
    }

    private fun JSONObject.toCard() = Card(
        jp = optString("jp"),
        kana = optString("kana"),
        ko = optString("ko"),
        exJp = optString("exJp"),
        exKana = optString("exKana"),
        exKo = optString("exKo"),
        bookmarked = optBoolean("bookmarked"),
    )

    private companion object {
        const val PREFS = "jp-flashcards-v1"
        const val KEY_TOPICS = "topics"
        const val KEY_FOLDERS = "folders"
    }
}
