package com.mochyy.mochymusic.ui.screens.search

import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.i18n.UiText
import java.util.Locale

/** Localize known UI category names without changing the source title or browse endpoint. */
internal fun localizedMoodTitle(title: String): String = when (title.trim().lowercase(Locale.ROOT)) {
    "gaming" -> UiText.get(R.string.toto_mood_gaming)
    "party" -> UiText.get(R.string.toto_mood_party)
    else -> title
}
