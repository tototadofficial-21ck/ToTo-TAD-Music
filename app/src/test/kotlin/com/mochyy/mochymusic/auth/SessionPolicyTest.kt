package com.mochyy.mochymusic.auth

import org.junit.Assert.*
import org.junit.Test

class SessionPolicyTest {
    @Test fun `transient failures retain only a previous grant`() {
        assertTrue(retainAuthorization(true, false, false))
        assertFalse(retainAuthorization(false, false, false))
    }
    @Test fun `confirmed revocation overrides cached and successful states`() {
        for (cached in listOf(false, true)) for (success in listOf(false, true)) {
            assertFalse(retainAuthorization(cached, success, true))
        }
    }
    @Test fun `server success grants access without a cached receipt`() {
        assertTrue(retainAuthorization(false, true, false))
        assertTrue(retainAuthorization(true, true, false))
    }
}
