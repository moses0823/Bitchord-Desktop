package com.music.bitchord.desktop

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * Makes Linux input methods (Zhuyin/Bopomofo, Pinyin, Kana, Hangul...) reachable from AWT.
 *
 * Java's X11 toolkit talks to ibus and fcitx through XIM, and it only finds them through the
 * `XMODIFIERS` environment variable. A desktop session started from a launcher, a systemd user
 * service, or a Wayland session often has `GTK_IM_MODULE` set but not `XMODIFIERS`, and then the
 * window takes plain keys and never opens the input method: Chinese cannot be typed at all.
 *
 * AWT reads the variable when the toolkit loads, so this must run before the first window.
 * It never overrides a value the user has set.
 */
internal object DesktopInputMethod {
    private interface LibC : Library {
        fun setenv(name: String, value: String, overwrite: Int): Int
    }

    /** `@im=` value implied by the variables desktops usually do export, or null for none. */
    internal fun xModifiersFor(env: Map<String, String?>): String? {
        val hint = listOf("XMODIFIERS_IM", "GTK_IM_MODULE", "QT_IM_MODULE", "INPUT_METHOD", "SDL_IM_MODULE")
            .firstNotNullOfOrNull { env[it]?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }
        val name = when {
            hint == null -> null
            hint.startsWith("fcitx") -> "fcitx"
            hint.startsWith("ibus") -> "ibus"
            hint == "xim" || hint == "none" || hint == "simple" || hint == "cedilla" -> null
            else -> hint
        }
        return name?.let { "@im=$it" }
    }

    fun install() {
        if (!DesktopPlatform.isLinux) return
        val env = System.getenv()
        if (!env["XMODIFIERS"].isNullOrBlank()) return
        val value = xModifiersFor(env) ?: guessRunningFramework() ?: return
        runCatching {
            Native.load("c", LibC::class.java).setenv("XMODIFIERS", value, 0)
            DesktopTrackLog.log("input method: XMODIFIERS=$value")
        }
    }

    /** No hint in the environment: take the framework whose daemon is actually running. */
    private fun guessRunningFramework(): String? {
        val running = runCatching {
            java.io.File("/proc").listFiles().orEmpty()
                .filter { it.name.all(Char::isDigit) }
                .mapNotNull { runCatching { java.io.File(it, "comm").readText().trim() }.getOrNull() }
                .toSet()
        }.getOrDefault(emptySet())
        return when {
            running.any { it.startsWith("fcitx") } -> "@im=fcitx"
            running.any { it.startsWith("ibus-daemon") } -> "@im=ibus"
            else -> null
        }
    }
}
