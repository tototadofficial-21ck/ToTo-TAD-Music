package com.mochyy.mochymusic.auth

/** Only a previous server grant survives a transient check failure; definitive revocation never does. */
internal fun retainAuthorization(previouslyGranted: Boolean, success: Boolean, invalidates: Boolean): Boolean =
    !invalidates && (success || previouslyGranted)
