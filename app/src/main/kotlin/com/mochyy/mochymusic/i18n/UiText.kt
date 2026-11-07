package com.mochyy.mochymusic.i18n

import android.content.Context
import androidx.annotation.StringRes

/** Localized text for sheet actions and callbacks outside composition.
 * AppLanguage already updates the application resources when the selection changes.
 */
internal object UiText {
    private lateinit var application: Context
    fun initialize(context: Context) { application = context.applicationContext }
    fun quantity(@androidx.annotation.PluralsRes id: Int, count: Int, vararg args: Any): String = formatLocalizedText(application.resources.getQuantityString(id, count), application.resources.configuration.locales[0], *args)
    fun get(@StringRes id: Int, vararg args: Any): String = formatLocalizedText(application.getString(id), application.resources.configuration.locales[0], *args)
}
