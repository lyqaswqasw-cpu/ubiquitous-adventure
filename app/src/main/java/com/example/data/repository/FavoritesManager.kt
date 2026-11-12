package com.example.data.repository

import android.content.Context
import com.example.data.model.FavoriteMediaSnapshot
import com.example.data.model.LibraryMediaItem
import com.example.data.model.LiveChannel
import com.example.data.model.XtreamAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FavoritesManager {
    private const val PREFS_NAME = "foul_tv_favorites_prefs_v1"
    private const val KEY_FAV_CHANNEL_IDS = "fav_channel_ids_json"
    private const val KEY_FAV_MEDIA_SNAPSHOTS = "fav_media_snapshots_json"
    private const val FIREBASE_DB_URL = "https://foul-b7671-default-rtdb.firebaseio.com"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    private val _favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChannelIds: StateFlow<Set<String>> = _favoriteChannelIds.asStateFlow()

    private val _favoriteMediaMap = MutableStateFlow<Map<String, FavoriteMediaSnapshot>>(emptyMap())
    val favoriteMediaMap: StateFlow<Map<String, FavoriteMediaSnapshot>> = _favoriteMediaMap.asStateFlow()
    val favoriteMediaIds: StateFlow<Map<String, FavoriteMediaSnapshot>> = _favoriteMediaMap.asStateFlow()

    private val _isFavoritesScreenOpen = MutableStateFlow(false)
    val isFavoritesScreenOpen: StateFlow<Boolean> = _isFavoritesScreenOpen.asStateFlow()

    fun openFavoritesScreen() {
        _isFavoritesScreenOpen.value = true
    }

    fun closeFavoritesScreen() {
        _isFavoritesScreenOpen.value = false
    }

    fun ensureInitialized(context: Context) {
        initialize(context)
    }

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        loadFromPrefs()
    }

    fun isChannelFavorite(channelId: String): Boolean {
        return _favoriteChannelIds.value.contains(channelId)
    }

    fun isMediaFavorite(mediaId: String): Boolean {
        return _favoriteMediaMap.value.containsKey(mediaId)
    }

    fun toggleChannelFavorite(context: Context, channel: LiveChannel) {
        initialize(context)
        val current = _favoriteChannelIds.value.toMutableSet()
        if (current.contains(channel.id)) {
            current.remove(channel.id)
        } else {
            current.add(channel.id)
        }
        _favoriteChannelIds.value = current
        saveToPrefs()
        syncToFirebaseIfLoggedIn()
    }

    fun removeChannelFavorite(context: Context, channelId: String) {
        initialize(context)
        val current = _favoriteChannelIds.value.toMutableSet()
        if (current.remove(channelId)) {
            _favoriteChannelIds.value = current
            saveToPrefs()
            syncToFirebaseIfLoggedIn()
        }
    }

    fun toggleMediaFavorite(context: Context, item: LibraryMediaItem) {
        initialize(context)
        val current = _favoriteMediaMap.value.toMutableMap()
        if (current.containsKey(item.id)) {
            current.remove(item.id)
        } else {
            current[item.id] = FavoriteMediaSnapshot(
                id = item.id,
                name = item.name,
                posterUrl = item.posterUrl,
                rating = item.rating,
                releaseYear = item.releaseYear,
                isSeries = item.isSeries,
                containerExtension = item.containerExtension,
                categoryId = item.categoryId,
                accountHost = item.account.host,
                accountUsername = item.account.username,
                accountPassword = item.account.password,
                accountName = item.account.name,
                addedAt = System.currentTimeMillis()
            )
        }
        _favoriteMediaMap.value = current
        saveToPrefs()
        syncToFirebaseIfLoggedIn()
    }

    fun removeMediaFavorite(context: Context, mediaId: String) {
        initialize(context)
        val current = _favoriteMediaMap.value.toMutableMap()
        if (current.remove(mediaId) != null) {
            _favoriteMediaMap.value = current
            saveToPrefs()
            syncToFirebaseIfLoggedIn()
        }
    }

    fun snapshotToLibraryMediaItem(snapshot: FavoriteMediaSnapshot): LibraryMediaItem {
        return LibraryMediaItem(
            id = snapshot.id,
            name = snapshot.name,
            lowerName = snapshot.name.lowercase(),
            posterUrl = snapshot.posterUrl,
            rating = snapshot.rating,
            releaseYear = snapshot.releaseYear,
            isSeries = snapshot.isSeries,
            containerExtension = snapshot.containerExtension,
            categoryId = snapshot.categoryId,
            plotPreview = "",
            account = XtreamAccount(
                id = "fav_acc_${snapshot.id}",
                name = snapshot.accountName.ifBlank { "FOUL VOD" },
                host = snapshot.accountHost,
                username = snapshot.accountUsername,
                password = snapshot.accountPassword
            )
        )
    }

    private fun loadFromPrefs() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        runCatching {
            val chJson = prefs.getString(KEY_FAV_CHANNEL_IDS, "[]") ?: "[]"
            val arr = JSONArray(chJson)
            val set = LinkedHashSet<String>()
            for (i in 0 until arr.length()) {
                val id = arr.optString(i).trim()
                if (id.isNotBlank()) set.add(id)
            }
            _favoriteChannelIds.value = set
        }
        runCatching {
            val mediaJson = prefs.getString(KEY_FAV_MEDIA_SNAPSHOTS, "[]") ?: "[]"
            val arr = JSONArray(mediaJson)
            val map = LinkedHashMap<String, FavoriteMediaSnapshot>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                if (id.isBlank()) continue
                map[id] = FavoriteMediaSnapshot(
                    id = id,
                    name = obj.optString("name"),
                    posterUrl = obj.optString("posterUrl"),
                    rating = obj.optString("rating"),
                    releaseYear = obj.optString("releaseYear"),
                    isSeries = obj.optBoolean("isSeries", false),
                    containerExtension = obj.optString("containerExtension", "mp4"),
                    categoryId = obj.optString("categoryId"),
                    accountHost = obj.optString("accountHost"),
                    accountUsername = obj.optString("accountUsername"),
                    accountPassword = obj.optString("accountPassword"),
                    accountName = obj.optString("accountName"),
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                )
            }
            _favoriteMediaMap.value = map
        }
    }

    private fun saveToPrefs() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val chArr = JSONArray()
        _favoriteChannelIds.value.forEach { chArr.put(it) }

        val mediaArr = JSONArray()
        _favoriteMediaMap.value.values.forEach { snap ->
            mediaArr.put(
                JSONObject().apply {
                    put("id", snap.id)
                    put("name", snap.name)
                    put("posterUrl", snap.posterUrl)
                    put("rating", snap.rating)
                    put("releaseYear", snap.releaseYear)
                    put("isSeries", snap.isSeries)
                    put("containerExtension", snap.containerExtension)
                    put("categoryId", snap.categoryId)
                    put("accountHost", snap.accountHost)
                    put("accountUsername", snap.accountUsername)
                    put("accountPassword", snap.accountPassword)
                    put("accountName", snap.accountName)
                    put("addedAt", snap.addedAt)
                }
            )
        }
        prefs.edit()
            .putString(KEY_FAV_CHANNEL_IDS, chArr.toString())
            .putString(KEY_FAV_MEDIA_SNAPSHOTS, mediaArr.toString())
            .apply()
    }

    private fun syncToFirebaseIfLoggedIn() {
        val uid = SocialFirebaseRepository.globalAuthenticatedUserFlow.value?.uid?.takeIf { it.isNotBlank() } ?: return
        val channelsList = _favoriteChannelIds.value.toList()
        val mediaList = _favoriteMediaMap.value.values.toList()
        scope.launch {
            runCatching {
                val payload = JSONObject().apply {
                    val chArr = JSONArray()
                    channelsList.forEach { chArr.put(it) }
                    put("channel_ids", chArr)

                    val mArr = JSONArray()
                    mediaList.forEach { snap ->
                        mArr.put(
                            JSONObject().apply {
                                put("id", snap.id)
                                put("name", snap.name)
                                put("posterUrl", snap.posterUrl)
                                put("rating", snap.rating)
                                put("releaseYear", snap.releaseYear)
                                put("isSeries", snap.isSeries)
                                put("containerExtension", snap.containerExtension)
                                put("categoryId", snap.categoryId)
                                put("accountHost", snap.accountHost)
                                put("accountUsername", snap.accountUsername)
                                put("accountPassword", snap.accountPassword)
                                put("accountName", snap.accountName)
                                put("addedAt", snap.addedAt)
                            }
                        )
                    }
                    put("media_items", mArr)
                    put("updated_at", System.currentTimeMillis())
                }
                val conn = (URL("$FIREBASE_DB_URL/user_favorites/$uid.json").openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            }
        }
    }
}
