package com.otaku.manayeou.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.otaku.manayeou.data.local.SettingsRepository
import com.otaku.manayeou.data.local.ThemeMode
import com.otaku.manayeou.data.local.UrlMode
import com.otaku.manayeou.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val urlMode: UrlMode = UrlMode.AUTO,
    val manualBaseUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val snackbarMessage: String? = null
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val repo = MangaRepository(app)

    private val _state = MutableStateFlow(
        SettingsUiState(
            urlMode = settingsRepo.getUrlMode(),
            manualBaseUrl = settingsRepo.getManualBaseUrl(),
            themeMode = settingsRepo.getThemeMode()
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setUrlMode(mode: UrlMode) {
        settingsRepo.setUrlMode(mode)
        _state.update { it.copy(urlMode = mode) }
    }

    fun onManualUrlChange(url: String) {
        _state.update { it.copy(manualBaseUrl = url) }
    }

    fun saveManualUrl() {
        val url = _state.value.manualBaseUrl.trim()
        settingsRepo.setManualBaseUrl(url)
        _state.update { it.copy(manualBaseUrl = url, snackbarMessage = "URL을 저장했습니다") }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepo.setThemeMode(mode)
        _state.update { it.copy(themeMode = mode) }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearImageCache() {
        val loader = getApplication<Application>().imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        _state.update { it.copy(snackbarMessage = "이미지 캐시를 삭제했습니다") }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repo.exportBackupJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                _state.update { it.copy(snackbarMessage = "백업을 저장했습니다") }
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = "백업 실패: ${e.message}") }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                repo.importBackupJson(json)
                _state.update { it.copy(snackbarMessage = "백업을 불러왔습니다") }
            } catch (e: Exception) {
                _state.update { it.copy(snackbarMessage = "불러오기 실패: ${e.message}") }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllData()
            _state.update { it.copy(snackbarMessage = "데이터를 삭제했습니다") }
        }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }
}
