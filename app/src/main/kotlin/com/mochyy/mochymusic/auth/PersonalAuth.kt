package com.mochyy.mochymusic.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** No music data or provider credentials enter this client. Never log request/response bodies. */
object PersonalAuth {
    @Volatile var navigationReady = false
    @Volatile var allowed = false
        private set
    var status by mutableStateOf(R.string.toto_checking)
        private set
    var busy by mutableStateOf(false)
        private set
    var revision by mutableStateOf(0)
        private set
    var warning by mutableStateOf<Int?>(null)
        private set
    private var lastCheck = 0L
    private val mutex = Mutex()
    private val storageLock = Any()
    private var generation = 0
    private var accessToken: String? = null
    var userId: String? = null
        private set
    private val logoutScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    // Runs on the main thread before any further playback command can be accepted.
    var stopPlayback: (() -> Unit)? = null

    private fun file(context: Context) = File(context.noBackupFilesDir, "toto-auth.enc")
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("toto-session-v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("toto-session-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun save(context: Context, refresh: String, attempt: Int, trustedUser: String? = null) = synchronized(storageLock) {
        // A logout and a late refresh response must never resurrect the disk session.
        if (attempt != generation) throw AuthFailure(R.string.toto_sign_in)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = JSONObject().put("refresh", refresh).put("project", BuildConfig.SUPABASE_URL.trimEnd('/'))
            .put("verified_user", trustedUser ?: JSONObject.NULL)
        val data = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
        val target = android.util.AtomicFile(file(context))
        val stream = target.startWrite()
        try { stream.write(cipher.iv + data); target.finishWrite(stream) }
        catch (e: Exception) { target.failWrite(stream); throw e }
    }
    private fun loadPayload(context: Context): String? {
        if (!file(context).exists()) return null
        val bytes = android.util.AtomicFile(file(context)).openRead().use { it.readBytes() }
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }
    private fun load(context: Context): String? = loadPayload(context)?.let { raw ->
        if (raw.startsWith("{")) {
            val payload = JSONObject(raw)
            if (payload.getString("project") != BuildConfig.SUPABASE_URL.trimEnd('/')) return@let null
            payload.getString("refresh")
        } else raw // Existing encrypted refresh-only sessions are verified once before receiving a receipt.
    }

    /** Local Keystore read only, before activities/services start. No network on the startup path. */
    fun restore(context: Context) {
        try {
            val raw = loadPayload(context) ?: return
            if (!raw.startsWith("{")) return
            val payload = JSONObject(raw)
            if (payload.getString("project") != BuildConfig.SUPABASE_URL.trimEnd('/')) return
            val user = payload.optString("verified_user").takeIf { it.isNotBlank() && it != "null" } ?: return
            java.util.UUID.fromString(user)
            if (payload.getString("refresh").isBlank()) return
            val matches = kotlinx.coroutines.runBlocking(Dispatchers.IO) { AccountProfile.isCachedFor(context, user) }
            if (!matches) return
            userId = user; allowed = true; revision++
            diagnostic("restore", "CACHED_GRANT")
        } catch (_: Exception) {
            // An unreadable receipt never grants access. Credentials remain out of logs.
            android.util.AtomicFile(file(context)).delete()
        }
    }

    fun dismissWarning() { warning = null }

    private fun diagnostic(stage: String, outcome: String, http: Int? = null) {
        // Only fixed stage/outcome labels and status numbers. No identity, URL, body or exception text.
        if (BuildConfig.DEBUG) android.util.Log.i("ToToAuth", "stage=$stage outcome=$outcome" + (http?.let { " http=$it" } ?: ""))
    }
    private class AuthFailure(val messageId: Int, val clear: Boolean = false): Exception()
    private fun request(path: String, body: JSONObject? = null, token: String? = null): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        if (!base.startsWith("https://") || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank())
            throw AuthFailure(R.string.toto_config)
        val conn = URL(base + path).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 15000; conn.readTimeout = 15000
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.requestMethod = "POST"; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val json = runCatching { JSONObject(error ?: "{}") }.getOrNull()
                val errorCode = if (json?.optString("message").equals("Invalid API key", ignoreCase = true)) "invalid_api_key"
                    else json?.optString("error_code")?.ifEmpty { json.optString("code") }.orEmpty()
                val permissionRequest = path.startsWith("/rest/v1/app_access")
                val problem = classifyAuthProblem(permissionRequest, code, errorCode)
                diagnostic(if (permissionRequest) "access_http" else "auth_http", problem.name, code)
                val message = when (problem) {
                    AuthProblem.CREDENTIALS -> R.string.toto_credentials
                    AuthProblem.LOCKED -> R.string.toto_auth_locked
                    AuthProblem.EMAIL_UNCONFIRMED -> R.string.toto_auth_unconfirmed
                    AuthProblem.CONFIGURATION -> R.string.toto_auth_database
                    AuthProblem.NETWORK -> R.string.toto_network
                }
                throw AuthFailure(message, problem == AuthProblem.CREDENTIALS || problem == AuthProblem.LOCKED || problem == AuthProblem.EMAIL_UNCONFIRMED)
            }

            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    suspend fun verify(context: Context, email: String? = null, password: String? = null, force: Boolean = false) = mutex.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && email == null && allowed && lastCheck != 0L && now - lastCheck < 60_000L) return@withLock
        val previouslyAllowed = allowed && email == null
        val previousUser = userId
        var invalidates = false
        val attempt = generation
        var verifiedUser: String? = null
        busy = true; status = R.string.toto_checking
        try {
        val result = withContext(Dispatchers.IO) {
            try {
                val body = if (email != null) JSONObject().put("email", email.trim()).put("password", password ?: "")
                    else JSONObject().put("refresh_token", load(context) ?: throw AuthFailure(R.string.toto_sign_in))
                val grant = if (email != null) "password" else "refresh_token"
                val session = JSONObject(request("/auth/v1/token?grant_type=$grant", body))
                // Persist rotation even if the subsequent permission request loses connectivity.
                save(context, session.getString("refresh_token"), attempt, if (previouslyAllowed) previousUser else null)
                val access = session.getString("access_token")
                val user = JSONObject(request("/auth/v1/user", token = access)).getString("id")
                val rows = JSONArray(request("/rest/v1/app_access?user_id=eq.$user&select=enabled", token = access))
                if (rows.length() != 1 || !rows.getJSONObject(0).optBoolean("enabled")) {
                    diagnostic("access", if (rows.length() == 0) "NO_VISIBLE_ROW" else "NOT_ENABLED")
                    throw AuthFailure(R.string.toto_auth_not_granted, true)
                }
                if (previouslyAllowed && previousUser != user) throw AuthFailure(R.string.toto_sign_in, true)
                AccountProfile.load(context, user, access)
                save(context, session.getString("refresh_token"), attempt, user)
                verifiedUser = user
                diagnostic("access", "GRANTED")
                Pair(access, null as Int?)
            } catch (e: AuthFailure) {
                invalidates = e.clear || e.messageId == R.string.toto_sign_in
                if (invalidates) android.util.AtomicFile(file(context)).delete()
                Pair(null, e.messageId)
            } catch (_: AccountProfile.Failure) { Pair(null, R.string.toto_profile_load_error) }
            catch (_: java.io.IOException) { diagnostic("verify", "CONNECTION_FAILURE"); Pair(null, R.string.toto_network) }
            catch (_: java.security.GeneralSecurityException) {
                invalidates = true
                diagnostic("verify", "SECURE_STORAGE_FAILURE")
                android.util.AtomicFile(file(context)).delete(); Pair(null, R.string.toto_auth_storage)
            } catch (_: Exception) { diagnostic("verify", "UNEXPECTED_RESPONSE"); Pair(null, R.string.toto_network) }
        }
        if (attempt == generation) {
            lastCheck = android.os.SystemClock.elapsedRealtime()
            val error = result.second
            allowed = retainAuthorization(previouslyAllowed, error == null, invalidates)
            if (error == null) {
                accessToken = result.first
                userId = verifiedUser
                warning = null
            } else if (allowed) {
                warning = error
            } else {
                accessToken = null; userId = null; navigationReady = false
                warning = if (previouslyAllowed) error else null
                stopPlayback?.invoke()
            }
            status = error ?: R.string.toto_sign_in
            busy = false; revision++
        } else {
            withContext(Dispatchers.IO) { android.util.AtomicFile(file(context)).delete() }
        }
        } finally { busy = false }
    }
    suspend fun saveProfile(context: Context, name: String, photo: android.net.Uri?) = mutex.withLock {
        val user = userId ?: throw AccountProfile.Failure()
        val token = accessToken ?: throw AccountProfile.Failure()
        if (!allowed) throw AccountProfile.Failure()
        val attempt = generation
        AccountProfile.save(context, user, token, name, photo)
        if (attempt != generation) throw AccountProfile.Failure()
    }

    fun logout(context: Context) {
        warning = null; lastCheck = 0L
        val previous = accessToken
        accessToken = null
        userId = null
        navigationReady = false; allowed = false; busy = false
        stopPlayback?.invoke()
        synchronized(storageLock) {
            generation++
            android.util.AtomicFile(file(context)).delete()
        }
        if (previous != null) logoutScope.launch {
            // Local sign-out is immediate even without network. No token is persisted here.
            runCatching { request("/auth/v1/logout?scope=local", JSONObject(), previous) }
        }
        status = R.string.toto_sign_in; revision++
    }
}
