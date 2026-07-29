package com.otaku.manayeou.ui.search

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

data class SearchUiState(
    val query: String = "",
    val results: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MangaRepository(app)

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun search() {
        val keyword = _state.value.query.trim()
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, hasSearched = true) }
            try {
                val results = repo.search(keyword)
                _state.update {
                    it.copy(
                        results = results,
                        isLoading = false,
                        error = if (results.isEmpty()) "검색 결과가 없습니다." else null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "검색 실패: ${e.message}") }
            }
        }
    }
}
