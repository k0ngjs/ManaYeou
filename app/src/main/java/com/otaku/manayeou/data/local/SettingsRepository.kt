package com.otaku.manayeou.data.local

import android.content.Context
import com.otaku.manayeou.data.model.ViewerMode
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREFS_NAME = "manayeou_settings"
private const val KEY_DEFAULT_VIEWER_MODE = "default_viewer_mode"
private const val KEY_URL_MODE = "url_mode"
private const val KEY_MANUAL_BASE_URL = "manual_base_url"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_READING_DIRECTION = "reading_direction"
private const val KEY_TWO_PAGE_MODE = "two_page_mode"
private const val KEY_FIRST_PAGE_SINGLE = "first_page_single"
private const val DEFAULT_MANUAL_URL = "https://kmana10.net"

enum class UrlMode { AUTO, MANUAL }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ReadingDirection { LTR, RTL }

// 앱 전역에서 테마를 반응형으로 전환하기 위한 공유 상태 (SharedPreferences는 자체적으로 옵저버블하지 않기 때문)
object ThemeState {
    val mode = MutableStateFlow(ThemeMode.SYSTEM)
}

// kmana10 연동과 무관한 앱 자체 로컬 설정 저장소 (URL 소스 설정은 예외 - 연동 대상 자체를 바꾸는 설정)
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultViewerMode(): ViewerMode =
        if (prefs.getString(KEY_DEFAULT_VIEWER_MODE, null) == ViewerMode.MANGA.name) {
            ViewerMode.MANGA
        } else {
            ViewerMode.WEBTOON
        }

    fun setDefaultViewerMode(mode: ViewerMode) {
        prefs.edit().putString(KEY_DEFAULT_VIEWER_MODE, mode.name).apply()
    }

    fun getUrlMode(): UrlMode =
        if (prefs.getString(KEY_URL_MODE, null) == UrlMode.MANUAL.name) UrlMode.MANUAL else UrlMode.AUTO

    fun setUrlMode(mode: UrlMode) {
        prefs.edit().putString(KEY_URL_MODE, mode.name).apply()
    }

    fun getManualBaseUrl(): String = prefs.getString(KEY_MANUAL_BASE_URL, DEFAULT_MANUAL_URL) ?: DEFAULT_MANUAL_URL

    fun setManualBaseUrl(url: String) {
        prefs.edit().putString(KEY_MANUAL_BASE_URL, url).apply()
    }

    fun getThemeMode(): ThemeMode =
        when (prefs.getString(KEY_THEME_MODE, null)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }.also { ThemeState.mode.value = it }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        ThemeState.mode.value = mode
    }

    fun getReadingDirection(): ReadingDirection =
        if (prefs.getString(KEY_READING_DIRECTION, null) == ReadingDirection.RTL.name) {
            ReadingDirection.RTL
        } else {
            ReadingDirection.LTR
        }

    fun setReadingDirection(direction: ReadingDirection) {
        prefs.edit().putString(KEY_READING_DIRECTION, direction.name).apply()
    }

    fun getTwoPageMode(): Boolean = prefs.getBoolean(KEY_TWO_PAGE_MODE, false)

    fun setTwoPageMode(value: Boolean) {
        prefs.edit().putBoolean(KEY_TWO_PAGE_MODE, value).apply()
    }

    // 두 페이지씩 볼 때 첫 스프레드를 표지 1장만 단독으로 보여줄지 여부 (짝이 밀리지 않게 하는 용도)
    fun getFirstPageSingle(): Boolean = prefs.getBoolean(KEY_FIRST_PAGE_SINGLE, true)

    fun setFirstPageSingle(value: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_PAGE_SINGLE, value).apply()
    }
}
