package com.mochyy.mochymusic.ui.screens.profile

import android.content.Context
import android.net.Uri
import com.mochyy.mochymusic.auth.PersonalAuth

/** Save to the authenticated account first; mirror locally only after the server confirms it. */
object ProfileStore {
    suspend fun save(context: Context, name: String, photo: Uri?) {
        PersonalAuth.saveProfile(context.applicationContext, name, photo)
    }
}
