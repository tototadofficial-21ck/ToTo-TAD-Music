package com.mochyy.mochymusic.i18n

import java.util.Locale

/** Literal resource text is not a printf template unless arguments were supplied. */
internal fun formatLocalizedText(text: String, locale: Locale, vararg args: Any): String =
    if (args.isEmpty()) text else String.format(locale, text, *args)
