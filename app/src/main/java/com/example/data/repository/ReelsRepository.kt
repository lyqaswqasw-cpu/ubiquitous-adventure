package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.DeepLinkPayload
import com.example.data.model.QuarantinedReelItem
import com.example.data.model.ReelCommentItem
import com.example.data.model.ReelItem
import com.example.data.model.ReelModerationDecision
import com.example.data.model.ReelModerationStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class ReelPendingReviewException(message: String) : IllegalStateException(message)

/**
 * Repository for FOUL REELS & Smart Deep Linking / Link Copying.
 * Connected strictly to the new Firebase Realtime Database:
 * https://foul-b7671-default-rtdb.firebaseio.com (Project ID: foul-b7671)
 * and the FOUL Cloudflare Worker / Backend:
 * https://foul.lydrwty8-12f.workers.dev
 */
class ReelsRepository {

    companion object {
        const val FIREBASE_DB_URL = "https://foul-b7671-default-rtdb.firebaseio.com"
        const val SECONDARY_FIREBASE_DB_URL = "https://foul-aeed9-default-rtdb.firebaseio.com"
        const val SERVER_URL = "https://foul.lydrwty8-12f.workers.dev"
        const val DEEP_LINK_SCHEME = "foultv"
        const val OFFICIAL_CHANNEL_URL = "https://t.me/jdj_q"
        const val REELS_CHANNEL_URL = "https://t.me/kdndjj98383838jdjdj"
        const val DEFAULT_REELS_CHAT = "kdndjj98383838jdjdj"
        private const val TELEGRAM_BOT_TOKEN = "9228657664:AAG3ATkyVoshvHxn2-Xsv2duoormdm8Blsk"

        fun buildSmartShareUrl(
            type: String,
            id: String,
            title: String = "",
            streamUrl: String = "",
            chatId: String = "",
            messageId: String = ""
        ): String {
            val cleanType = type.trim().lowercase().ifBlank { "reel" }
            val params = mutableListOf<String>()
            if (id.isNotBlank()) params.add("id=${encode(id)}")
            if (chatId.isNotBlank()) params.add("chat_id=${encode(chatId.removePrefix("@"))}")
            if (messageId.isNotBlank()) params.add("message_id=${encode(messageId)}")
            if (streamUrl.isNotBlank()) params.add("stream_url=${encode(streamUrl)}")
            if (title.isNotBlank()) params.add("title=${encode(title)}")
            val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
            return "$SERVER_URL/open/$cleanType$query"
        }

        fun buildCustomDeepLink(
            type: String,
            id: String,
            title: String = "",
            streamUrl: String = "",
            chatId: String = "",
            messageId: String = ""
        ): String {
            val cleanType = type.trim().lowercase().ifBlank { "reel" }
            val params = mutableListOf<String>()
            if (id.isNotBlank()) params.add("id=${encode(id)}")
            if (chatId.isNotBlank()) params.add("chat_id=${encode(chatId.removePrefix("@"))}")
            if (messageId.isNotBlank()) params.add("message_id=${encode(messageId)}")
            if (streamUrl.isNotBlank()) params.add("stream_url=${encode(streamUrl)}")
            if (title.isNotBlank()) params.add("title=${encode(title)}")
            val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
            return "$DEEP_LINK_SCHEME://open/$cleanType$query"
        }

        fun parseDeepLinkUri(uri: Uri?): DeepLinkPayload? {
            if (uri == null) return null
            val scheme = (uri.scheme ?: "").lowercase()
            val host = (uri.host ?: "").lowercase()
            val pathSegments = uri.pathSegments ?: emptyList()

            // Case 1: foultv://open/{type}?id=... or foultv://{type}/{id}
            if (scheme == DEEP_LINK_SCHEME) {
                val type = when {
                    host == "open" && pathSegments.isNotEmpty() -> pathSegments[0].lowercase()
                    host in listOf("reel", "reels", "channel", "movie", "series", "story") ->
                        if (host == "reels" || host == "story") "reel" else host
                    pathSegments.isNotEmpty() -> pathSegments[0].lowercase()
                    else -> "reel"
                }
                val idFromPath = if (host == "open" && pathSegments.size >= 2) {
                    pathSegments[1]
                } else if (host != "open" && pathSegments.isNotEmpty()) {
                    pathSegments[0]
                } else ""
                val id = uri.getQueryParameter("id").orEmpty().ifBlank { idFromPath }
                val chatId = uri.getQueryParameter("chat_id").orEmpty()
                val messageId = uri.getQueryParameter("message_id").orEmpty()
                val streamUrl = uri.getQueryParameter("stream_url").orEmpty()
                val title = uri.getQueryParameter("title").orEmpty()
                val resolvedId = if (id.isBlank() && chatId.isNotBlank() && messageId.isNotBlank()) {
                    "${chatId.removePrefix("@")}_$messageId"
                } else id
                return DeepLinkPayload(
                    type = if (type == "reels" || type == "story") "reel" else type,
                    id = resolvedId,
                    chatId = chatId,
                    messageId = messageId,
                    streamUrl = streamUrl,
                    title = title
                )
            }

            // Case 2: https://foul.lydrwty8-12f.workers.dev/open/{type} or /share/{chat_id}/{message_id} or /stream_msg/{chat_id}/{message_id}
            if (scheme == "http" || scheme == "https") {
                if (pathSegments.isNotEmpty()) {
                    val first = pathSegments[0].lowercase()
                    if (first == "open" && pathSegments.size >= 2) {
                        val rawType = pathSegments[1].lowercase()
                        val type = if (rawType == "reels" || rawType == "story") "reel" else rawType
                        val pathId = if (pathSegments.size >= 3) pathSegments[2] else ""
                        val id = uri.getQueryParameter("id").orEmpty().ifBlank { pathId }
                        val chatId = uri.getQueryParameter("chat_id").orEmpty()
                        val messageId = uri.getQueryParameter("message_id").orEmpty()
                        val streamUrl = uri.getQueryParameter("stream_url").orEmpty()
                        val title = uri.getQueryParameter("title").orEmpty()
                        val resolvedId = if (id.isBlank() && chatId.isNotBlank() && messageId.isNotBlank()) {
                            "${chatId.removePrefix("@")}_$messageId"
                        } else id
                        return DeepLinkPayload(
                            type = type,
                            id = resolvedId,
                            chatId = chatId,
                            messageId = messageId,
                            streamUrl = streamUrl,
                            title = title
                        )
                    } else if (first == "share" && pathSegments.size >= 3) {
                        val chatId = pathSegments[1].removePrefix("@")
                        val messageId = pathSegments[2]
                        val id = "${chatId}_$messageId"
                        return DeepLinkPayload(
                            type = "reel",
                            id = id,
                            chatId = chatId,
                            messageId = messageId,
                            streamUrl = "$SERVER_URL/stream_msg/$chatId/$messageId",
                            title = "FOUL REELS"
                        )
                    } else if (first == "stream_msg" && pathSegments.size >= 3) {
                        val chatId = pathSegments[1].removePrefix("@")
                        val messageId = pathSegments[2]
                        val id = "${chatId}_$messageId"
                        return DeepLinkPayload(
                            type = "reel",
                            id = id,
                            chatId = chatId,
                            messageId = messageId,
                            streamUrl = "$SERVER_URL/stream_msg/$chatId/$messageId",
                            title = "FOUL REELS"
                        )
                    }
                }
            }
            return null
        }

        private fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8")
    }

    /**
     * Fetches reels from the FOUL Backend (/api/reels) and merges/syncs with the
     * Firebase Realtime Databases (https://foul-b7671-default-rtdb.firebaseio.com/reels.json
     * and https://foul-aeed9-default-rtdb.firebaseio.com/reels.json) so published Reels
     * from all users are always loaded from the server even after uninstalling/reinstalling the app.
     */
    suspend fun fetchReels(currentUserId: String): List<ReelItem> = withContext(Dispatchers.IO) {
        val mergedById = linkedMapOf<String, ReelItem>()

        // 1. Fetch directly from Firebase foul-b7671 (/reels.json) and secondary mirror foul-aeed9 (/reels.json)
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                val rawJson = httpGet("$dbUrl/reels.json")
                if (rawJson.isNotBlank() && rawJson != "null") {
                    val root = JSONObject(rawJson)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        val item = parseReelJson(key, obj, currentUserId)
                        if (item.streamUrl.isNotBlank() && !mergedById.containsKey(item.id)) {
                            mergedById[item.id] = item
                        }
                    }
                }
            }
        }

        // 2. Also fetch from SERVER_URL/api/reels to pick up any worker-cached items
        runCatching {
            val apiJson = httpGet("$SERVER_URL/api/reels")
            if (apiJson.isNotBlank()) {
                val root = JSONObject(apiJson)
                val arr: JSONArray? = root.optJSONArray("reels") ?: root.optJSONArray("data")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val key = obj.optString("id").ifBlank { "reel_$i" }
                        val item = parseReelJson(key, obj, currentUserId)
                        if (item.streamUrl.isNotBlank() && !mergedById.containsKey(item.id)) {
                            mergedById[item.id] = item
                        }
                    }
                }
            }
        }

        mergedById.values.sortedByDescending { it.createdAt }
    }

    /**
     * Fetches published Stories from Firebase Realtime Databases (/stories.json)
     * and the FOUL Backend (/api/stories).
     */
    suspend fun fetchStories(): List<com.example.data.model.StoryItem> = withContext(Dispatchers.IO) {
        val mergedById = linkedMapOf<String, com.example.data.model.StoryItem>()

        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                val rawJson = httpGet("$dbUrl/stories.json")
                if (rawJson.isNotBlank() && rawJson != "null") {
                    val root = JSONObject(rawJson)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        val story = parseStoryJson(key, obj)
                        if (story.mediaUrl.isNotBlank() && !mergedById.containsKey(story.id)) {
                            mergedById[story.id] = story
                        }
                    }
                }
            }
        }

        runCatching {
            val apiJson = httpGet("$SERVER_URL/api/stories")
            if (apiJson.isNotBlank()) {
                val root = JSONObject(apiJson)
                val arr: JSONArray? = root.optJSONArray("stories") ?: root.optJSONArray("data")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val key = obj.optString("id").ifBlank { "story_$i" }
                        val story = parseStoryJson(key, obj)
                        if (story.mediaUrl.isNotBlank() && !mergedById.containsKey(story.id)) {
                            mergedById[story.id] = story
                        }
                    }
                }
            }
        }

        mergedById.values.sortedWith(
            compareByDescending<com.example.data.model.StoryItem> { it.isPinnedFirst }
                .thenByDescending { it.createdAt }
        )
    }

    /**
     * Uploads an image or video Story selected by the Developer to Telegram Storage
     * and persists the Story metadata in Firebase Realtime Databases (/stories/{id}.json).
     */
    suspend fun uploadStoryMedia(
        context: Context,
        mediaUri: Uri,
        caption: String,
        userId: String,
        userName: String,
        userPersonalName: String = "",
        userHandle: String = "@foultv",
        userPhotoUrl: String = com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL,
        categoryKey: String = "foul_tv",
        isPinnedFirst: Boolean = false
    ): Result<com.example.data.model.StoryItem> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = context.contentResolver.getType(mediaUri)?.lowercase().orEmpty()
            val isImage = mimeType.startsWith("image/")
            val defaultExt = if (isImage) "jpg" else "mp4"
            val fileName = resolveFileName(context, mediaUri)
                ?: "foul_story_${System.currentTimeMillis()}.$defaultExt"
            val mediaBytes = context.contentResolver.openInputStream(mediaUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("تعذر قراءة ملف القصة المختار")

            val effectiveName = userPersonalName.trim().ifBlank { userName }
            val cleanHandle = if (userHandle.trim().startsWith("@")) {
                userHandle.trim()
            } else {
                "@${userHandle.trim().ifBlank { userName }}"
            }
            val effectivePhoto = userPhotoUrl.trim().ifBlank {
                com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL
            }

            val tgMethod = if (isImage) "sendPhoto" else "sendVideo"
            val tgField = if (isImage) "photo" else "video"
            val targetChat = DEFAULT_REELS_CHAT
            val fieldsMap = mutableMapOf(
                "chat_id" to "@$targetChat",
                "caption" to caption
            )
            if (!isImage) {
                fieldsMap["supports_streaming"] = "true"
            }

            val tgRespStr = uploadMultipartToBackend(
                endpoint = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/$tgMethod",
                fileFieldName = tgField,
                fileName = fileName,
                fileBytes = mediaBytes,
                fields = fieldsMap
            )
            val tgJson = JSONObject(tgRespStr)
            if (!tgJson.optBoolean("ok", false)) {
                throw IllegalStateException("تعذر رفع القصة إلى السحابة: ${tgJson.optString("description")}")
            }
            val resultObj = tgJson.getJSONObject("result")
            val messageId = resultObj.optLong("message_id", System.currentTimeMillis() / 1000L)
            val fileId = if (isImage) {
                val photos = resultObj.optJSONArray("photo")
                if (photos != null && photos.length() > 0) {
                    photos.optJSONObject(photos.length() - 1)?.optString("file_id").orEmpty()
                } else {
                    resultObj.optJSONObject("document")?.optString("file_id").orEmpty()
                }
            } else {
                val vid = resultObj.optJSONObject("video") ?: resultObj.optJSONObject("document")
                vid?.optString("file_id").orEmpty()
            }

            var directFileUrl = ""
            if (fileId.isNotBlank()) {
                runCatching {
                    val fileInfoStr = httpGet(
                        "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getFile?file_id=${encode(fileId)}"
                    )
                    val filePath = JSONObject(fileInfoStr)
                        .optJSONObject("result")
                        ?.optString("file_path")
                        .orEmpty()
                    if (filePath.isNotBlank()) {
                        directFileUrl = "https://api.telegram.org/file/bot$TELEGRAM_BOT_TOKEN/$filePath"
                    }
                }
            }

            val storyId = "story_${targetChat}_$messageId"
            val workerUrl = "$SERVER_URL/stream_msg/$targetChat/$messageId"
            val resolvedMediaUrl = directFileUrl.ifBlank { workerUrl }
            val nowMs = System.currentTimeMillis()

            val storyItem = com.example.data.model.StoryItem(
                id = storyId,
                categoryKey = categoryKey.ifBlank { "foul_tv" },
                userId = userId,
                userName = effectiveName,
                userHandle = cleanHandle,
                userPhotoUrl = effectivePhoto,
                isVerified = true,
                mediaType = if (isImage) "IMAGE" else "VIDEO",
                mediaUrl = resolvedMediaUrl,
                thumbnailUrl = if (isImage) resolvedMediaUrl else effectivePhoto,
                caption = caption,
                subtitle = cleanHandle,
                isPinnedFirst = isPinnedFirst,
                isFromDeveloper = true,
                createdAt = nowMs
            )

            saveStoryToFirebase(storyItem, targetChat, messageId, fileId, directFileUrl)
            storyItem
        }
    }

    suspend fun saveDirectDeveloperStory(
        storyItem: com.example.data.model.StoryItem
    ): Result<com.example.data.model.StoryItem> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanId = storyItem.id.ifBlank { "story_dev_${System.currentTimeMillis()}" }
            val updated = storyItem.copy(id = cleanId, isFromDeveloper = true)
            saveStoryToFirebase(updated, DEFAULT_REELS_CHAT, 0L, "", updated.mediaUrl)
            updated
        }
    }

    suspend fun deleteDeveloperStory(storyId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (storyId.isBlank()) return@runCatching false
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest("$dbUrl/stories/$storyId.json", "DELETE", "")
                }
            }
            true
        }
    }

    private fun saveStoryToFirebase(
        storyItem: com.example.data.model.StoryItem,
        targetChat: String,
        messageId: Long,
        fileId: String,
        directFileUrl: String
    ) {
        val payload = JSONObject().apply {
            put("id", storyItem.id)
            put("category_key", storyItem.categoryKey)
            put("chat_id", targetChat)
            put("message_id", messageId)
            put("file_id", fileId)
            put("user_id", storyItem.userId)
            put("user_name", storyItem.userName)
            put("user_handle", storyItem.userHandle)
            put("user_photo_url", storyItem.userPhotoUrl)
            put("is_verified", storyItem.isVerified)
            put("media_type", storyItem.mediaType)
            put("media_url", storyItem.mediaUrl)
            put("stream_url", storyItem.mediaUrl)
            put("direct_video_url", directFileUrl)
            put("thumbnail_url", storyItem.thumbnailUrl)
            put("caption", storyItem.caption)
            put("is_pinned_first", storyItem.isPinnedFirst)
            put("is_from_developer", storyItem.isFromDeveloper)
            put("created_at", storyItem.createdAt)
        }

        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                httpRequest("$dbUrl/stories/${storyItem.id}.json", "PUT", payload.toString())
            }
        }
    }

    suspend fun fetchHomeManagementConfig(): com.example.data.model.HomeManagementConfig = withContext(Dispatchers.IO) {
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                val raw = httpGet("$dbUrl/home_management_config.json")
                if (raw.isNotBlank() && raw != "null") {
                    val obj = JSONObject(raw)
                    val customIds = mutableListOf<String>()
                    val idsArr = obj.optJSONArray("custom_featured_channel_ids")
                    if (idsArr != null) {
                        for (i in 0 until idsArr.length()) {
                            val id = idsArr.optString(i).trim()
                            if (id.isNotBlank()) customIds.add(id)
                        }
                    }
                    val customSecs = mutableListOf<com.example.data.model.HomeCustomSectionItem>()
                    val secsArr = obj.optJSONArray("custom_sections")
                    if (secsArr != null) {
                        for (i in 0 until secsArr.length()) {
                            val sObj = secsArr.optJSONObject(i) ?: continue
                            val title = sObj.optString("title").trim()
                            if (title.isNotBlank()) {
                                customSecs.add(
                                    com.example.data.model.HomeCustomSectionItem(
                                        id = sObj.optString("id").ifBlank { "sec_$i" },
                                        title = title,
                                        subtitle = sObj.optString("subtitle"),
                                        imageUrl = sObj.optString("image_url"),
                                        actionType = sObj.optString("action_type", "CHANNEL"),
                                        targetValue = sObj.optString("target_value"),
                                        isPinnedFirst = sObj.optBoolean("is_pinned_first", false),
                                        createdAt = sObj.optLong("created_at", System.currentTimeMillis())
                                    )
                                )
                            }
                        }
                    }
                    return@withContext com.example.data.model.HomeManagementConfig(
                        showStoriesSection = obj.optBoolean("show_stories_section", true),
                        showFoulTvStory = obj.optBoolean("show_foul_tv_story", true),
                        showMatchesStory = obj.optBoolean("show_matches_story", true),
                        showMoviesStory = obj.optBoolean("show_movies_story", true),
                        showSeriesStory = obj.optBoolean("show_series_story", true),
                        showFootballStory = obj.optBoolean("show_football_story", true),
                        showReelsSection = obj.optBoolean("show_reels_section", true),
                        showSuggestedMatchesSection = obj.optBoolean("show_suggested_matches_section", true),
                        showSuggestedChannelsSection = obj.optBoolean("show_suggested_channels_section", true),
                        foulTvStoryFirst = obj.optBoolean("foul_tv_story_first", true),
                        customFeaturedChannelIds = customIds,
                        customSections = customSecs.sortedWith(
                            compareByDescending<com.example.data.model.HomeCustomSectionItem> { it.isPinnedFirst }
                                .thenByDescending { it.createdAt }
                        )
                    )
                }
            }
        }
        com.example.data.model.HomeManagementConfig()
    }

    suspend fun saveHomeManagementConfig(
        config: com.example.data.model.HomeManagementConfig
    ): Result<com.example.data.model.HomeManagementConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val idsArr = JSONArray()
            config.customFeaturedChannelIds.forEach { idsArr.put(it) }

            val secsArr = JSONArray()
            config.customSections.forEach { sec ->
                secsArr.put(
                    JSONObject().apply {
                        put("id", sec.id)
                        put("title", sec.title)
                        put("subtitle", sec.subtitle)
                        put("image_url", sec.imageUrl)
                        put("action_type", sec.actionType)
                        put("target_value", sec.targetValue)
                        put("is_pinned_first", sec.isPinnedFirst)
                        put("created_at", sec.createdAt)
                    }
                )
            }

            val payload = JSONObject().apply {
                put("show_stories_section", config.showStoriesSection)
                put("show_foul_tv_story", config.showFoulTvStory)
                put("show_matches_story", config.showMatchesStory)
                put("show_movies_story", config.showMoviesStory)
                put("show_series_story", config.showSeriesStory)
                put("show_football_story", config.showFootballStory)
                put("show_reels_section", config.showReelsSection)
                put("show_suggested_matches_section", config.showSuggestedMatchesSection)
                put("show_suggested_channels_section", config.showSuggestedChannelsSection)
                put("foul_tv_story_first", config.foulTvStoryFirst)
                put("custom_featured_channel_ids", idsArr)
                put("custom_sections", secsArr)
            }

            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest("$dbUrl/home_management_config.json", "PUT", payload.toString())
                }
            }
            config
        }
    }

    private fun parseStoryJson(key: String, obj: JSONObject): com.example.data.model.StoryItem {
        val chatId = obj.optString("chat_id", DEFAULT_REELS_CHAT).removePrefix("@").ifBlank { DEFAULT_REELS_CHAT }
        val messageId = obj.optLong("message_id", obj.optLong("msg_id", 0L))
        val id = obj.optString("id").ifBlank {
            if (messageId > 0L) "story_${chatId}_$messageId" else key
        }
        val directUrl = obj.optString("direct_video_url").orEmpty()
        val rawMedia = obj.optString("media_url")
            .ifBlank { obj.optString("stream_url") }
            .ifBlank { obj.optString("video_url") }
            .ifBlank {
                if (messageId > 0L) "$SERVER_URL/stream_msg/$chatId/$messageId" else ""
            }
        val mediaUrl = if (directUrl.startsWith("http")) directUrl else rawMedia
        val mediaType = obj.optString("media_type", "VIDEO").uppercase()
        val userName = obj.optString("user_name", "FOUL TV")
        val userHandle = obj.optString("user_handle", "@foultv")
        val userPhotoUrl = obj.optString("user_photo_url").ifBlank {
            com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL
        }
        val thumb = obj.optString("thumbnail_url").ifBlank {
            if (mediaType == "IMAGE") mediaUrl else userPhotoUrl
        }
        val categoryKey = obj.optString("category_key", "foul_tv")

        return com.example.data.model.StoryItem(
            id = id,
            categoryKey = categoryKey,
            userId = obj.optString("user_id", "foul_dev"),
            userName = userName,
            userHandle = userHandle,
            userPhotoUrl = userPhotoUrl,
            isVerified = obj.optBoolean("is_verified", true),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumb,
            caption = obj.optString("caption").ifBlank { obj.optString("overlay_text", "") },
            subtitle = userHandle,
            isPinnedFirst = obj.optBoolean("is_pinned_first", false),
            isFromDeveloper = obj.optBoolean("is_from_developer", true),
            createdAt = obj.optLong("created_at", obj.optLong("timestamp", System.currentTimeMillis()))
        )
    }

    /**
     * Uploads a video selected from the user's device after strictly enforcing:
     * 1. Real duration <= 30 seconds (`30_000 ms`).
     * 2. Multi-stage AI Content Moderation (Visual frames, scene jumps, audio/speech, caption).
     * 3. Temporary Quarantine Storage (`/quarantine_reels`) before promoting approved videos to `/reels`.
     */
    suspend fun uploadReelVideo(
        context: Context,
        videoUri: Uri,
        caption: String,
        overlayText: String,
        userId: String,
        userName: String,
        userPersonalName: String = "",
        userHandle: String = "@foultv",
        userPhotoUrl: String = com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL,
        onModerationStageChanged: (ReelModerationStage) -> Unit = {}
    ): Result<ReelItem> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Strict 30-second programmatic duration check
            val localMeta = ReelAiModerationEngine.inspectSelectedVideoMetadata(context, videoUri)
                .getOrElse {
                    throw IllegalStateException(it.message ?: "تعذر قراءة بيانات الفيديو المختار")
                }

            if (localMeta.durationMs <= 0L || localMeta.durationMs > ReelAiModerationEngine.MAX_REEL_DURATION_MS) {
                throw IllegalStateException(ReelAiModerationEngine.DURATION_EXCEEDED_MESSAGE_AR)
            }

            // 2. Run 4-Stage AI Content Moderation before any public upload
            val moderationResult = ReelAiModerationEngine.runFullVideoModeration(
                context = context,
                videoUri = videoUri,
                caption = caption,
                fileName = localMeta.fileName,
                durationMs = localMeta.durationMs,
                onStageChanged = onModerationStageChanged
            )

            if (moderationResult.decision == ReelModerationDecision.REJECTED) {
                throw IllegalStateException(moderationResult.reasonAr)
            }

            onModerationStageChanged(ReelModerationStage.UPLOADING_QUARANTINE)

            val fileName = resolveFileName(context, videoUri) ?: "foul_reel_${System.currentTimeMillis()}.mp4"
            val videoBytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("تعذر قراءة ملف الفيديو المختار")

            val effectivePersonalName = userPersonalName.trim().ifBlank { userName }
            val cleanHandle = if (userHandle.trim().startsWith("@")) {
                userHandle.trim()
            } else {
                "@${userHandle.trim().ifBlank { userName }}"
            }
            val effectivePhotoUrl = userPhotoUrl.trim().ifBlank {
                com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL
            }

            // Upload media stream to cloud storage first (stored in Quarantine metadata until approved)
            val tgRespStr = uploadMultipartToBackend(
                endpoint = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendVideo",
                fileFieldName = "video",
                fileName = fileName,
                fileBytes = videoBytes,
                fields = mapOf(
                    "chat_id" to "@$DEFAULT_REELS_CHAT",
                    "caption" to caption,
                    "supports_streaming" to "true"
                )
            )
            val tgJson = JSONObject(tgRespStr)
            if (!tgJson.optBoolean("ok", false)) {
                throw IllegalStateException("تعذر رفع الفيديو إلى التخزين السحابي: ${tgJson.optString("description")}")
            }
            val resultObj = tgJson.getJSONObject("result")
            val messageId = resultObj.optLong("message_id", System.currentTimeMillis() / 1000L)
            val videoObj = resultObj.optJSONObject("video") ?: resultObj.optJSONObject("document")
            val fileId = videoObj?.optString("file_id").orEmpty()

            var directFileUrl = ""
            if (fileId.isNotBlank()) {
                runCatching {
                    val fileInfoStr = httpGet(
                        "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getFile?file_id=${encode(fileId)}"
                    )
                    val filePath = JSONObject(fileInfoStr)
                        .optJSONObject("result")
                        ?.optString("file_path")
                        .orEmpty()
                    if (filePath.isNotBlank()) {
                        directFileUrl = "https://api.telegram.org/file/bot$TELEGRAM_BOT_TOKEN/$filePath"
                    }
                }
            }

            val reelId = "${DEFAULT_REELS_CHAT}_$messageId"
            val workerStreamUrl = "$SERVER_URL/stream_msg/$DEFAULT_REELS_CHAT/$messageId"
            val effectiveStreamUrl = directFileUrl.ifBlank { workerStreamUrl }
            val copyVideoUrl = "$SERVER_URL/stream_msg/$DEFAULT_REELS_CHAT/$messageId/video.mp4"
            val shareUrl = buildSmartShareUrl(
                type = "reel",
                id = reelId,
                title = caption.ifBlank { "FOUL REELS" },
                streamUrl = workerStreamUrl,
                chatId = DEFAULT_REELS_CHAT,
                messageId = messageId.toString()
            )
            val deepLink = buildCustomDeepLink(
                type = "reel",
                id = reelId,
                title = caption.ifBlank { "FOUL REELS" },
                streamUrl = workerStreamUrl,
                chatId = DEFAULT_REELS_CHAT,
                messageId = messageId.toString()
            )

            // 3. Always write to private Quarantine Storage (`/quarantine_reels`) first
            val quarantineItem = QuarantinedReelItem(
                id = reelId,
                chatId = DEFAULT_REELS_CHAT,
                messageId = messageId,
                fileId = fileId,
                caption = caption,
                userId = userId,
                userName = userName,
                userPersonalName = effectivePersonalName,
                userHandle = cleanHandle,
                userPhotoUrl = effectivePhotoUrl,
                streamUrl = effectiveStreamUrl,
                videoUrl = workerStreamUrl,
                durationSeconds = ((localMeta.durationMs + 500L) / 1000L).toInt().coerceAtLeast(1),
                moderationDecision = moderationResult.decision.name,
                moderationReason = moderationResult.reasonAr,
                detectedCategories = moderationResult.detectedCategories,
                createdAt = System.currentTimeMillis()
            )
            saveQuarantinedReelToFirebase(quarantineItem, directFileUrl)

            // 4. If NEEDS_REVIEW, keep strictly in Quarantine (do NOT publish to `/reels`)
            if (moderationResult.decision == ReelModerationDecision.NEEDS_REVIEW) {
                throw ReelPendingReviewException(ReelAiModerationEngine.NEEDS_REVIEW_MESSAGE_AR)
            }

            // 5. If APPROVED, promote from Quarantine to public `/reels` and delete temporary Quarantine entry
            val newReel = ReelItem(
                id = reelId,
                chatId = DEFAULT_REELS_CHAT,
                messageId = messageId,
                fileId = fileId,
                caption = caption,
                overlayText = overlayText.ifBlank { caption },
                userId = userId,
                userName = userName,
                userPersonalName = effectivePersonalName,
                userHandle = cleanHandle,
                userPhotoUrl = effectivePhotoUrl,
                streamUrl = effectiveStreamUrl,
                videoUrl = workerStreamUrl,
                copyVideoUrl = copyVideoUrl,
                shareUrl = shareUrl,
                deepLink = deepLink,
                channelUrl = OFFICIAL_CHANNEL_URL,
                reelsChannelUrl = REELS_CHANNEL_URL,
                likesCount = 0,
                viewsCount = 0,
                sharesCount = 0,
                commentsCount = 0,
                isLikedByMe = false,
                createdAt = System.currentTimeMillis()
            )
            saveReelToFirebase(newReel, directFileUrl)
            deleteQuarantinedReelFromFirebase(reelId)
            newReel
        }
    }

    /**
     * Fetches all videos currently held in Quarantine (`/quarantine_reels.json`) awaiting
     * Admin/Developer review in the Content Moderation Control Panel.
     */
    suspend fun fetchQuarantinedReels(): List<QuarantinedReelItem> = withContext(Dispatchers.IO) {
        val map = linkedMapOf<String, QuarantinedReelItem>()
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                val raw = httpGet("$dbUrl/quarantine_reels.json")
                if (raw.isNotBlank() && raw != "null") {
                    val root = JSONObject(raw)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val obj = root.optJSONObject(k) ?: continue
                        val id = obj.optString("id").ifBlank { k }
                        val cats = mutableListOf<String>()
                        val cArr = obj.optJSONArray("detected_categories")
                        if (cArr != null) {
                            for (i in 0 until cArr.length()) {
                                val c = cArr.optString(i).trim()
                                if (c.isNotBlank()) cats.add(c)
                            }
                        }
                        val item = QuarantinedReelItem(
                            id = id,
                            chatId = obj.optString("chat_id", DEFAULT_REELS_CHAT),
                            messageId = obj.optLong("message_id", 0L),
                            fileId = obj.optString("file_id", ""),
                            caption = obj.optString("caption", ""),
                            userId = obj.optString("user_id", ""),
                            userName = obj.optString("user_name", "مستخدم FOUL TV"),
                            userPersonalName = obj.optString("user_personal_name", ""),
                            userHandle = obj.optString("user_handle", "@foultv"),
                            userPhotoUrl = obj.optString("user_photo_url", com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL),
                            streamUrl = obj.optString("direct_video_url").ifBlank { obj.optString("stream_url", "") },
                            videoUrl = obj.optString("video_url", ""),
                            durationSeconds = obj.optInt("duration_seconds", 15),
                            moderationDecision = obj.optString("moderation_decision", "NEEDS_REVIEW"),
                            moderationReason = obj.optString("moderation_reason", ""),
                            detectedCategories = cats,
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                        if (!map.containsKey(id)) {
                            map[id] = item
                        }
                    }
                }
            }
        }
        map.values.sortedByDescending { it.createdAt }
    }

    /**
     * Approves a Quarantined Reel from the Developer Moderation Panel:
     * Promotes it to `/reels/{id}` and deletes it from `/quarantine_reels/{id}`.
     */
    suspend fun approveQuarantinedReel(item: QuarantinedReelItem): Result<ReelItem> = withContext(Dispatchers.IO) {
        runCatching {
            val workerStreamUrl = item.videoUrl.ifBlank {
                "$SERVER_URL/stream_msg/${item.chatId}/${item.messageId}"
            }
            val copyVideoUrl = "$SERVER_URL/stream_msg/${item.chatId}/${item.messageId}/video.mp4"
            val shareUrl = buildSmartShareUrl(
                type = "reel",
                id = item.id,
                title = item.caption.ifBlank { "FOUL REELS" },
                streamUrl = workerStreamUrl,
                chatId = item.chatId,
                messageId = item.messageId.toString()
            )
            val deepLink = buildCustomDeepLink(
                type = "reel",
                id = item.id,
                title = item.caption.ifBlank { "FOUL REELS" },
                streamUrl = workerStreamUrl,
                chatId = item.chatId,
                messageId = item.messageId.toString()
            )
            val approvedReel = ReelItem(
                id = item.id,
                chatId = item.chatId,
                messageId = item.messageId,
                fileId = item.fileId,
                caption = item.caption,
                overlayText = item.caption,
                userId = item.userId,
                userName = item.userName,
                userPersonalName = item.userPersonalName.ifBlank { item.userName },
                userHandle = item.userHandle,
                userPhotoUrl = item.userPhotoUrl,
                streamUrl = item.streamUrl.ifBlank { workerStreamUrl },
                videoUrl = workerStreamUrl,
                copyVideoUrl = copyVideoUrl,
                shareUrl = shareUrl,
                deepLink = deepLink,
                channelUrl = OFFICIAL_CHANNEL_URL,
                reelsChannelUrl = REELS_CHANNEL_URL,
                createdAt = System.currentTimeMillis()
            )
            saveReelToFirebase(approvedReel, item.streamUrl)
            deleteQuarantinedReelFromFirebase(item.id)
            approvedReel
        }
    }

    /**
     * Rejects and permanently deletes a Quarantined Reel from `/quarantine_reels/{id}`.
     */
    suspend fun rejectQuarantinedReel(quarantineId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            deleteQuarantinedReelFromFirebase(quarantineId)
            true
        }
    }

    private fun saveQuarantinedReelToFirebase(item: QuarantinedReelItem, directVideoUrl: String) {
        val catsArr = JSONArray()
        item.detectedCategories.forEach { catsArr.put(it) }
        val payload = JSONObject().apply {
            put("id", item.id)
            put("chat_id", item.chatId)
            put("message_id", item.messageId)
            put("file_id", item.fileId)
            put("caption", item.caption)
            put("user_id", item.userId)
            put("user_name", item.userName)
            put("user_personal_name", item.userPersonalName)
            put("user_handle", item.userHandle)
            put("user_photo_url", item.userPhotoUrl)
            put("stream_url", item.streamUrl)
            put("video_url", item.videoUrl)
            put("direct_video_url", directVideoUrl)
            put("duration_seconds", item.durationSeconds)
            put("moderation_decision", item.moderationDecision)
            put("moderation_reason", item.moderationReason)
            put("detected_categories", catsArr)
            put("created_at", item.createdAt)
        }
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                httpRequest("$dbUrl/quarantine_reels/${item.id}.json", "PUT", payload.toString())
            }
        }
    }

    private fun deleteQuarantinedReelFromFirebase(quarantineId: String) {
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                val conn = (URL("$dbUrl/quarantine_reels/$quarantineId.json").openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                try {
                    conn.responseCode
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    /**
     * Deletes a published Reel from Firebase Realtime Databases (only called by its publisher).
     */
    suspend fun deleteReel(reelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    val conn = (URL("$dbUrl/reels/$reelId.json").openConnection() as HttpURLConnection).apply {
                        requestMethod = "DELETE"
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    try {
                        conn.responseCode
                    } finally {
                        conn.disconnect()
                    }
                }
                runCatching {
                    val conn = (URL("$dbUrl/reel_comments/$reelId.json").openConnection() as HttpURLConnection).apply {
                        requestMethod = "DELETE"
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    try {
                        conn.responseCode
                    } finally {
                        conn.disconnect()
                    }
                }
            }
            Unit
        }
    }

    suspend fun toggleLike(reel: ReelItem, userId: String): Result<ReelItem> = withContext(Dispatchers.IO) {
        runCatching {
            val rawStr = httpGet("$FIREBASE_DB_URL/reels/${reel.id}.json")
            val obj = if (rawStr.isNotBlank() && rawStr != "null") JSONObject(rawStr) else JSONObject()
            val likedBy = obj.optJSONObject("liked_by") ?: JSONObject()

            val currentlyLiked = likedBy.optBoolean(userId, false)
            if (currentlyLiked) {
                likedBy.remove(userId)
            } else {
                likedBy.put(userId, true)
            }
            val newLikesCount = likedBy.length()
            val patch = JSONObject().apply {
                put("liked_by", likedBy)
                put("likes_count", newLikesCount)
            }
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest("$dbUrl/reels/${reel.id}.json", "PATCH", patch.toString())
                }
            }

            // Also notify backend asynchronously
            runCatching {
                val reqBody = JSONObject().apply { put("user_id", userId) }.toString()
                httpRequest("$SERVER_URL/api/reels/${reel.id}/like", "POST", reqBody)
            }

            reel.copy(
                likesCount = newLikesCount,
                isLikedByMe = !currentlyLiked
            )
        }
    }

    suspend fun incrementView(reel: ReelItem): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val newViews = reel.viewsCount + 1
            val patch = JSONObject().apply {
                put("views_count", newViews)
            }
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest("$dbUrl/reels/${reel.id}.json", "PATCH", patch.toString())
                }
            }
            runCatching {
                httpRequest("$SERVER_URL/api/reels/${reel.id}/view", "POST", "{}")
            }
            newViews
        }
    }

    suspend fun incrementShareCount(reel: ReelItem): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val newShares = reel.sharesCount + 1
            val patch = JSONObject().apply {
                put("shares_count", newShares)
            }
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest("$dbUrl/reels/${reel.id}.json", "PATCH", patch.toString())
                }
            }
            newShares
        }
    }

    suspend fun fetchComments(reelId: String): List<ReelCommentItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ReelCommentItem>()
        runCatching {
            val raw = httpGet("$FIREBASE_DB_URL/reel_comments/$reelId.json")
            if (raw.isNotBlank() && raw != "null") {
                val root = JSONObject(raw)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val obj = root.optJSONObject(k) ?: continue
                    list.add(
                        ReelCommentItem(
                            id = obj.optString("id").ifBlank { k },
                            reelId = reelId,
                            userId = obj.optString("user_id", "foul_user"),
                            userName = obj.optString("user_name", "مشاهد FOUL TV"),
                            text = obj.optString("text", ""),
                            createdAt = obj.optLong("created_at", 0L)
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.createdAt }
    }

    suspend fun addComment(
        reelId: String,
        userId: String,
        userName: String,
        text: String,
        currentCommentsCount: Int = 0
    ): Result<ReelCommentItem> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val commentId = "c_${now}_${UUID.randomUUID().toString().take(6)}"
            val item = ReelCommentItem(
                id = commentId,
                reelId = reelId,
                userId = userId,
                userName = userName,
                text = text.trim(),
                createdAt = now
            )
            val payload = JSONObject().apply {
                put("id", item.id)
                put("reel_id", item.reelId)
                put("user_id", item.userId)
                put("user_name", item.userName)
                put("text", item.text)
                put("created_at", item.createdAt)
            }
            val nextCount = (currentCommentsCount + 1).coerceAtLeast(1)
            val countPatch = JSONObject().apply {
                put("comments_count", nextCount)
            }
            for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
                runCatching {
                    httpRequest(
                        "$dbUrl/reel_comments/$reelId/$commentId.json",
                        "PUT",
                        payload.toString()
                    )
                    httpRequest(
                        "$dbUrl/reels/$reelId.json",
                        "PATCH",
                        countPatch.toString()
                    )
                }
            }
            item
        }
    }

    private fun saveReelToFirebase(reel: ReelItem, directVideoUrl: String = "") {
        val payload = JSONObject().apply {
            put("id", reel.id)
            put("chat_id", reel.chatId)
            put("message_id", reel.messageId)
            put("file_id", reel.fileId)
            put("caption", reel.caption)
            put("overlay_text", reel.overlayText)
            put("user_id", reel.userId)
            put("user_name", reel.userName)
            put("user_personal_name", reel.userPersonalName)
            put("user_handle", reel.userHandle)
            put("user_photo_url", reel.userPhotoUrl)
            put("thumbnail_url", reel.thumbnailUrl)
            put("stream_url", reel.streamUrl)
            put("video_url", reel.videoUrl)
            put("copy_video_url", reel.copyVideoUrl)
            put("share_url", reel.shareUrl)
            put("deep_link", reel.deepLink)
            put("channel_url", reel.channelUrl)
            put("reels_channel_url", reel.reelsChannelUrl)
            if (directVideoUrl.isNotBlank()) {
                put("direct_video_url", directVideoUrl)
            }
            put("likes_count", reel.likesCount)
            put("views_count", reel.viewsCount)
            put("shares_count", reel.sharesCount)
            put("comments_count", reel.commentsCount)
            put("created_at", reel.createdAt)
        }
        for (dbUrl in listOf(FIREBASE_DB_URL, SECONDARY_FIREBASE_DB_URL)) {
            runCatching {
                httpRequest("$dbUrl/reels/${reel.id}.json", "PUT", payload.toString())
            }
        }
    }

    private fun parseReelJson(key: String, obj: JSONObject, currentUserId: String): ReelItem {
        val chatId = obj.optString("chat_id", DEFAULT_REELS_CHAT).removePrefix("@").ifBlank { DEFAULT_REELS_CHAT }
        val messageId = obj.optLong("message_id", obj.optLong("msg_id", 0L))
        val id = obj.optString("id").ifBlank {
            if (messageId > 0L) "${chatId}_$messageId" else key
        }

        val defaultStreamUrl = if (messageId > 0L) {
            "$SERVER_URL/stream_msg/$chatId/$messageId"
        } else ""
        val directVideo = obj.optString("direct_video_url").orEmpty()
        val rawStream = obj.optString("stream_url")
            .ifBlank { obj.optString("video_url") }
            .ifBlank { defaultStreamUrl }
        val streamUrl = if (directVideo.startsWith("http")) directVideo else rawStream
        val videoUrl = rawStream.ifBlank { streamUrl }

        val copyVideoUrl = obj.optString("copy_video_url").ifBlank {
            if (messageId > 0L) "$SERVER_URL/stream_msg/$chatId/$messageId/video.mp4" else videoUrl
        }
        val caption = obj.optString("caption").ifBlank { obj.optString("overlay_text", "") }
        val overlayText = obj.optString("overlay_text").ifBlank { caption }

        val shareUrl = obj.optString("share_url").ifBlank {
            buildSmartShareUrl(
                type = "reel",
                id = id,
                title = caption.ifBlank { "FOUL REELS" },
                streamUrl = videoUrl,
                chatId = chatId,
                messageId = if (messageId > 0L) messageId.toString() else ""
            )
        }
        val deepLink = obj.optString("deep_link").ifBlank {
            buildCustomDeepLink(
                type = "reel",
                id = id,
                title = caption.ifBlank { "FOUL REELS" },
                streamUrl = videoUrl,
                chatId = chatId,
                messageId = if (messageId > 0L) messageId.toString() else ""
            )
        }

        val likedByObj = obj.optJSONObject("liked_by")
        val isLiked = likedByObj?.optBoolean(currentUserId, false) ?: false
        val likesCount = obj.optInt(
            "likes_count",
            obj.optInt("likes", likedByObj?.length() ?: 0)
        )
        val viewsCount = obj.optInt("views_count", obj.optInt("views", 0))
        val sharesCount = obj.optInt("shares_count", obj.optInt("shares", 0))
        val commentsCount = obj.optInt("comments_count", obj.optInt("comments", 0))
        val userName = obj.optString("user_name", "FOUL TV")
        val userPersonalName = obj.optString("user_personal_name").ifBlank { userName }
        val rawHandle = obj.optString("user_handle", "@foultv")
        val userHandle = if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"
        val userPhotoUrl = obj.optString("user_photo_url").ifBlank {
            com.example.data.model.SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL
        }
        val thumbnailUrl = obj.optString("thumbnail_url", "")

        return ReelItem(
            id = id,
            chatId = chatId,
            messageId = messageId,
            fileId = obj.optString("file_id", ""),
            caption = caption,
            overlayText = overlayText,
            userId = obj.optString("user_id", "foul_user"),
            userName = userName,
            userPersonalName = userPersonalName,
            userHandle = userHandle,
            userPhotoUrl = userPhotoUrl,
            thumbnailUrl = thumbnailUrl,
            streamUrl = streamUrl,
            videoUrl = videoUrl,
            copyVideoUrl = copyVideoUrl,
            shareUrl = shareUrl,
            deepLink = deepLink,
            channelUrl = obj.optString("channel_url", OFFICIAL_CHANNEL_URL).ifBlank { OFFICIAL_CHANNEL_URL },
            reelsChannelUrl = obj.optString("reels_channel_url", REELS_CHANNEL_URL).ifBlank { REELS_CHANNEL_URL },
            likesCount = likesCount,
            viewsCount = viewsCount,
            sharesCount = sharesCount,
            commentsCount = commentsCount,
            isLikedByMe = isLiked,
            createdAt = obj.optLong("created_at", obj.optLong("timestamp", System.currentTimeMillis()))
        )
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpRequest(urlStr: String, method: String, jsonBody: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            if (method == "PATCH") {
                requestMethod = "POST"
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                requestMethod = method
            }
            connectTimeout = 12000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun uploadMultipartToBackend(
        endpoint: String,
        fileFieldName: String,
        fileName: String,
        fileBytes: ByteArray,
        fields: Map<String, String>
    ): String {
        val boundary = "----FoulAndroidBoundary${UUID.randomUUID().toString().replace("-", "")}"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            doInput = true
            doOutput = true
            useCaches = false
            connectTimeout = 30000
            readTimeout = 90000
            requestMethod = "POST"
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        return try {
            DataOutputStream(conn.outputStream).use { dos ->
                for ((key, value) in fields) {
                    dos.writeBytes(twoHyphens + boundary + lineEnd)
                    dos.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd$lineEnd")
                    dos.write(value.toByteArray(Charsets.UTF_8))
                    dos.writeBytes(lineEnd)
                }

                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes(
                    "Content-Disposition: form-data; name=\"$fileFieldName\"; filename=\"$fileName\"$lineEnd"
                )
                val fileContentType = if (fileFieldName == "photo") "image/jpeg" else "video/mp4"
                dos.writeBytes("Content-Type: $fileContentType$lineEnd$lineEnd")
                dos.write(fileBytes)
                dos.writeBytes(lineEnd)
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                dos.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: $resp")
            }
            resp
        } finally {
            conn.disconnect()
        }
    }
}
