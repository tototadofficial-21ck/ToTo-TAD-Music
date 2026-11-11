package com.mochyy.mochymusic.core.updater

import org.junit.Assert.*
import org.junit.Test

class PersonalUpdatePolicyTest {
    @Test fun blocksUntrustedDownloadRedirects() {
        listOf("http://github.com/a", "https://github.com.evil.example/a", "https://github.com@evil.example/a",
            "https://user:pass@github.com/a", "file:///tmp/a.apk", "https://github.com:8443/a", "not a url")
            .forEach { assertFalse(it, PersonalUpdatePolicy.allowedUrl(it)) }
        listOf("https://github.com/a", "https://release-assets.githubusercontent.com/a?sig=example",
            "https://api.github.com/repos/example/releases/latest").forEach { assertTrue(PersonalUpdatePolicy.allowedUrl(it)) }
    }
    @Test fun neverOffersSameOrOlderVersion() {
        assertFalse(PersonalUpdatePolicy.newerTag("v1000", 1000))
        assertFalse(PersonalUpdatePolicy.newerTag("v999", 1000))
        assertTrue(PersonalUpdatePolicy.newerTag("v1001", 1000))
    }
    @Test fun malformedTagsFailClosed() {
        listOf("1.0.1", "v1.0.1", "v-1", "v9999999999", "v1001/other", "v01001").forEach {
            assertTrue(runCatching { PersonalUpdatePolicy.newerTag(it, 1000) }.isFailure)
        }
    }
}
