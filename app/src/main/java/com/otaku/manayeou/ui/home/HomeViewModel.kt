package com.otaku.manayeou.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otaku.manayeou.data.model.Series
import com.otaku.manayeou.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val latest: List<Series> = emptyList(),
    val popular: List<Series> = emptyList(),
    val bookmarked: List<Series> = emptyList(),
    val recent: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MangaRepository(app)

    private val _state = MutableStateFlow(HomeUiState(isLoading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        observeLocalData()
        refresh()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            repo.getBookmarks().collect { bookmarks ->
                _state.update { it.copy(bookmarked = bookmarks) }
            }
        }
        viewModelScope.launch {
            repo.getRecentHistory().collect { recent ->
                _state.update { it.copy(recent = recent) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val latest = repo.fetchLatest()
                val popular = repo.fetchPopular()
                _state.update { it.copy(latest = latest, popular = popular, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "불러오기 실패: ${e.message}") }
            }
        }
    }
}
