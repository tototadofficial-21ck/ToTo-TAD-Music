package com.mochyy.mochymusic.auth

internal enum class AuthProblem { CREDENTIALS, LOCKED, EMAIL_UNCONFIRMED, CONFIGURATION, NETWORK }

/** Permission/schema failures must never tell a user that a valid password is wrong. */
internal fun classifyAuthProblem(permissionRequest: Boolean, httpStatus: Int, code: String): AuthProblem = when {
    code == "user_banned" -> AuthProblem.LOCKED
    code == "email_not_confirmed" -> AuthProblem.EMAIL_UNCONFIRMED
    code == "invalid_api_key" -> AuthProblem.CONFIGURATION
    permissionRequest && httpStatus in setOf(400, 401, 403, 404) -> AuthProblem.CONFIGURATION
    !permissionRequest && httpStatus in setOf(400, 401, 403) -> AuthProblem.CREDENTIALS
    else -> AuthProblem.NETWORK
}
