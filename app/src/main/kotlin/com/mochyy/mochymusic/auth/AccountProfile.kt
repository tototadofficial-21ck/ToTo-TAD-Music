package com.mochyy.mochymusic.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.constants.*
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.time.OffsetDateTime
import javax.net.ssl.HttpsURLConnection

/** Only profile identity is sent; music data and service credentials never enter this client. */
internal object AccountProfile {
    private val ownerKey = stringPreferencesKey("supabaseProfileOwner")
    suspend fun isCachedFor(context: Context, user: String): Boolean = context.dataStore.data.first()[ownerKey] == user
    class Failure : Exception() // Never surface HTTP bodies, signed URLs or tokens.

    private fun request(path: String, token: String, method: String = "GET", bytes: ByteArray? = null,
                        type: String = "application/json"): ByteArray {
        val c = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpsURLConnection
        try {
            c.connectTimeout = 15000; c.readTimeout = 15000
            c.instanceFollowRedirects = false; c.useCaches = false; c.requestMethod = method
            c.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            c.setRequestProperty("Authorization", "Bearer $token")
            if (bytes != null) {
                c.doOutput = true; c.setRequestProperty("Content-Type", type)
                c.setRequestProperty("x-upsert", "true")
                c.outputStream.use { it.write(bytes) }
            }
            if (c.responseCode !in 200..299) throw Failure()
            return c.inputStream.use { input ->
                val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    if (out.size() + n > 2 * 1024 * 1024) throw Failure()
                    out.write(buffer, 0, n)
                }; out.toByteArray()
            }
        } finally { c.disconnect() }
    }

    /** Called while auth owns its mutex, before navigation can show a different user's profile. */
    suspend fun load(context: Context, user: String, token: String) = withContext(Dispatchers.IO) {
        val row = JSONArray(String(request("/rest/v1/profiles?user_id=eq.$user&select=display_name,avatar_path,created_at,profile_completed", token), Charsets.UTF_8))
        if (row.length() != 1) throw Failure()
        val profile = row.getJSONObject(0)
        val avatar = profile.optString("avatar_path")
        val photo = if (avatar == "$user/avatar.jpg") {
            val bytes = request("/storage/v1/object/authenticated/toto-avatars/$user/avatar.jpg", token)
            File(context.noBackupFilesDir, "toto-avatar-$user-${System.nanoTime()}.jpg").apply { writeBytes(bytes) }.absolutePath
        } else ""
        // Keep pre-account profile available for an explicit user choice, never auto-upload it.
        val previous = context.dataStore.data.first()
        val legacy = File(context.noBackupFilesDir, "toto-legacy-profile.json")
        if (previous[ownerKey] == null && !previous[UsernameKey].isNullOrBlank() && !legacy.exists()) {
            legacy.writeText(JSONObject().put("name", previous[UsernameKey]).put("photo", previous[ProfilePicUriKey].orEmpty()).toString())
        }
        context.dataStore.edit {
            it[ownerKey] = user
            it[UsernameKey] = profile.getString("display_name")
            it[ProfilePicUriKey] = photo
            it[SetupCompletedKey] = profile.getBoolean("profile_completed")
            it[ProfileJoinedEpochDayKey] = OffsetDateTime.parse(profile.getString("created_at")).toLocalDate().toEpochDay()
            it[ProfileCardNumberKey] = java.util.UUID.fromString(user).leastSignificantBits.toULong().toString().takeLast(11).padStart(11, '0')
        }
        if (profile.getBoolean("profile_completed")) com.mochyy.mochymusic.markOnboardedOnThisInstall(context)
        // Fresh paths invalidate image caches; only remove our previous downloaded avatar.
        previous[ProfilePicUriKey]?.let { old ->
            val file = File(old)
            if (file.parentFile == context.noBackupFilesDir && file.name.startsWith("toto-avatar-") && old != photo) file.delete()
        }
    }

    suspend fun legacy(context: Context): Pair<String, Uri?>? = withContext(Dispatchers.IO) {
        runCatching {
            val row = JSONObject(File(context.noBackupFilesDir, "toto-legacy-profile.json").readText())
            row.getString("name") to row.optString("photo").takeIf { it.isNotBlank() }?.let {
                if (it.startsWith("content:")) Uri.parse(it) else Uri.fromFile(File(it))
            }
        }.getOrNull()
    }

    suspend fun save(context: Context, user: String, token: String, name: String, photo: Uri?) = withContext(Dispatchers.IO) {
        require(name.isNotBlank() && name.length <= 32)
        val body = JSONObject().put("display_name", name).put("profile_completed", true)
        if (photo != null) {
            // Decode sampled first, strip metadata and cap the encoded upload to 2 MiB.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(photo)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw Failure()
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val bitmap = context.contentResolver.openInputStream(photo)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: throw Failure()
            val bytes = try { ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, this) }.toByteArray() }
                finally { bitmap.recycle() }
            if (bytes.size > 2097152) throw Failure()
            request("/storage/v1/object/toto-avatars/$user/avatar.jpg", token, "POST", bytes, "image/jpeg")
            body.put("avatar_path", "$user/avatar.jpg")
        }
        request("/rest/v1/profiles?user_id=eq.$user", token, "PATCH", body.toString().toByteArray(Charsets.UTF_8))
        load(context, user, token)
    }
}
