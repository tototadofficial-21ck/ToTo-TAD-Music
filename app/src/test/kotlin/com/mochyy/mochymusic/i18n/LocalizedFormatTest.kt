package com.mochyy.mochymusic.i18n

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalizedFormatTest {
    @Test fun `recap literal percentage does not run through printf`() {
        assertEquals("trên tổng 100%", formatLocalizedText("trên tổng 100%", Locale.forLanguageTag("vi")))
        assertEquals("out of 100%", formatLocalizedText("out of 100%", Locale.ENGLISH))
    }
    @Test fun `positional arguments and escaped percent still format`() {
        assertEquals("Tháng 11: 100%", formatLocalizedText("Tháng %1\$d: 100%%", Locale.forLanguageTag("vi"), 11))
    }
}
