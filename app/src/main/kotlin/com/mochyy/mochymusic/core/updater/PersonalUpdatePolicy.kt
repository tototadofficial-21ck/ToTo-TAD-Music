package com.mochyy.mochymusic.core.updater

import java.net.URI

/** Feed and redirect boundaries are kept independently testable without Android. */
internal object PersonalUpdatePolicy {
    fun allowedUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.rawUserInfo == null && uri.port in listOf(-1, 443) &&
            uri.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
    }.getOrDefault(false)

    fun newerTag(tag: String, installed: Long): Boolean {
        require(tag.matches(Regex("v[1-9][0-9]{0,9}")))
        val version = tag.drop(1).toLong()
        require(version <= Int.MAX_VALUE)
        return version > installed
    }
}
