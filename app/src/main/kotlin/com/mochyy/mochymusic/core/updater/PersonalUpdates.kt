package com.mochyy.mochymusic.core.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.auth.PersonalAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.coroutineContext

/** Independent feed; never enables the upstream Mochy installer. No credentials sent. */
object PersonalUpdates {
    const val REPO = "tototadofficial-21ck/ToTo-TAD-Music"
    private const val MAX_APK = 512L * 1024 * 1024
    data class Release(val name: String, val url: String, val size: Long, val sha256: String, val versionCode: Long)

    private fun connection(url: String): HttpsURLConnection {
        val parsed = URL(url)
        require(PersonalUpdatePolicy.allowedUrl(url))
        return (parsed.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "ToTo-TAD-Music")
        }
    }

    private fun signatures(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { hash(it.toByteArray()) }.toSet()
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val signatureFlags get() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }
    fun compatible(context: Context): Boolean = !BuildConfig.DEBUG && signatures(
        context.packageManager.getPackageInfo(context.packageName, signatureFlags)
    ) == setOf(BuildConfig.TOTO_RELEASE_CERT)

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        val conn = connection("https://api.github.com/repos/$REPO/releases/latest")
        try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode == 404) return@withContext null
            check(conn.responseCode == 200)
            val bytes = conn.inputStream.use { it.readBytesLimited(1024 * 1024) }
            val release = JSONObject(bytes.toString(Charsets.UTF_8))
            check(!release.getBoolean("draft") && !release.getBoolean("prerelease"))
            // Tags are v<versionCode>. Display version is carried by the APK and release name.
            if (!PersonalUpdatePolicy.newerTag(release.getString("tag_name"), BuildConfig.VERSION_CODE.toLong())) return@withContext null
            val assets = release.getJSONArray("assets")
            val asset = (0 until assets.length()).map { assets.getJSONObject(it) }
                .single { it.getString("name") == "ToTo-TAD-Music.apk" }
            val digest = asset.getString("digest").removePrefix("sha256:")
            check(digest.matches(Regex("[a-f0-9]{64}")))
            val url = asset.getString("browser_download_url")
            check(url.startsWith("https://github.com/$REPO/releases/download/"))
            Release(release.getString("name").take(120), url, asset.getLong("size"), digest, release.getString("tag_name").drop(1).toLong())
                .also { check(it.size in 1..MAX_APK) }
        } finally { conn.disconnect() }
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            check(out.size().toLong() + count <= limit)
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    suspend fun download(context: Context, release: Release): File = withContext(Dispatchers.IO) {
        check(PersonalAuth.allowed && compatible(context))
        val folder = File(context.cacheDir, "toto-updates").apply { mkdirs() }
        val partial = File(folder, "update.part")
        val apk = File(folder, "update.apk")
        apk.delete()
        var conn = connection(release.url)
        try {
            var redirects = 0
            while (conn.responseCode in listOf(301, 302, 303, 307, 308)) {
                check(++redirects <= 5)
                val next = URL(conn.url, conn.getHeaderField("Location")).toString()
                conn.disconnect()
                conn = connection(next)
            }
            check(conn.responseCode == 200)
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            conn.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        count += n
                        check(count <= release.size && count <= MAX_APK)
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                }
            }
            check(count == release.size && digest.digest().joinToString("") { "%02x".format(it) } == release.sha256)
            verify(context, partial, release.versionCode)
            check(partial.renameTo(apk))
            apk
        } finally { conn.disconnect(); partial.delete() }
    }

    private fun verify(context: Context, apk: File, expectedVersion: Long? = null) {
        check(PersonalAuth.allowed && compatible(context))
        val info = checkNotNull(context.packageManager.getPackageArchiveInfo(apk.path, signatureFlags))
        check(info.packageName == context.packageName)
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        check(version > BuildConfig.VERSION_CODE && (expectedVersion == null || version == expectedVersion))
        check(signatures(info) == setOf(BuildConfig.TOTO_RELEASE_CERT))
    }

    /** Returns false after opening Android permission settings; user taps Install again on return. */
    suspend fun install(context: Context, apk: File): Boolean {
        withContext(Dispatchers.IO) { verify(context, apk) }
        check(PersonalAuth.allowed)
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        return true
    }
}
