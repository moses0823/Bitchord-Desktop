package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopInputMethodTest {
    @Test fun ibusFromGtk() = assertEquals("@im=ibus", DesktopInputMethod.xModifiersFor(mapOf("GTK_IM_MODULE" to "ibus")))
    @Test fun fcitx5FromQt() = assertEquals("@im=fcitx", DesktopInputMethod.xModifiersFor(mapOf("QT_IM_MODULE" to "fcitx5")))
    @Test fun nothingSet() = assertNull(DesktopInputMethod.xModifiersFor(emptyMap()))
    @Test fun plainXimIsLeftAlone() = assertNull(DesktopInputMethod.xModifiersFor(mapOf("GTK_IM_MODULE" to "xim")))
}
