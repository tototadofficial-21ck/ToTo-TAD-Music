package com.mochyy.mochymusic.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthProblemTest {
    @Test fun `valid password followed by denied database access is a configuration error`() {
        listOf(400, 401, 403, 404).forEach { status ->
            assertEquals(AuthProblem.CONFIGURATION, classifyAuthProblem(true, status, "42501"))
        }
        assertEquals(AuthProblem.CONFIGURATION, classifyAuthProblem(true, 404, "PGRST205"))
    }
    @Test fun `account restrictions are distinct from incorrect credentials`() {
        assertEquals(AuthProblem.LOCKED, classifyAuthProblem(false, 403, "user_banned"))
        assertEquals(AuthProblem.EMAIL_UNCONFIRMED, classifyAuthProblem(false, 400, "email_not_confirmed"))
        assertEquals(AuthProblem.CREDENTIALS, classifyAuthProblem(false, 400, "invalid_credentials"))
        assertEquals(AuthProblem.CONFIGURATION, classifyAuthProblem(false, 401, "invalid_api_key"))
    }
    @Test fun `unavailable services do not blame the password`() {
        listOf(429, 500, 502, 503).forEach { status ->
            assertEquals(AuthProblem.NETWORK, classifyAuthProblem(false, status, ""))
            assertEquals(AuthProblem.NETWORK, classifyAuthProblem(true, status, ""))
        }
    }
}
