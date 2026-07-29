package com.otaku.manayeou.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otaku.manayeou.data.model.Series
import com.otaku.manayeou.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListUiState(
    val title: String = "",
    val items: List<Series> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val selectionMode: Boolean = false
)

class ListViewModel(
    app: Application,
    private val category: String
) : AndroidViewModel(app) {

    private val repo = MangaRepository(app)

    // 북마크/최근 본 만화는 로컬 DB 기반이라 꾹 눌러 일괄 삭제 가능; 최신/인기는 원격 목록이라 삭제 불가
    val isDeletable = category == "bookmarked" || category == "recent"

    private val _state = MutableStateFlow(ListUiState(title = categoryTitle(category)))
    val state: StateFlow<ListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun startSelection(seriesId: String) {
        if (!isDeletable) return
        _state.update { it.copy(selectionMode = true, selectedIds = setOf(seriesId)) }
    }

    fun toggleSelection(seriesId: String) {
        _state.update { s ->
            val next = if (seriesId in s.selectedIds) s.selectedIds - seriesId else s.selectedIds + seriesId
            s.copy(selectedIds = next, selectionMode = next.isNotEmpty())
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _state.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                when (category) {
                    "bookmarked" -> repo.removeBookmark(id)
                    "recent" -> repo.removeHistory(id)
                }
            }
            _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
        }
    }

    fun retry() {
        _state.update { it.copy(isLoading = true, error = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                when (category) {
                    "latest" -> {
                        val items = repo.fetchLatest()
                        _state.update { it.copy(items = items, isLoading = false) }
                    }
                    "popular" -> {
                        val items = repo.fetchPopular()
                        _state.update { it.copy(items = items, isLoading = false) }
                    }
                    "bookmarked" -> {
                        repo.getBookmarks().collect { items ->
                            _state.update { it.copy(items = items, isLoading = false) }
                        }
                    }
                    "recent" -> {
                        repo.getRecentHistory(100).collect { items ->
                            _state.update { it.copy(items = items, isLoading = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "불러오기 실패: ${e.message}") }
            }
        }
    }

    private fun categoryTitle(cat: String) = when (cat) {
        "latest" -> "최신 만화"
        "popular" -> "인기 만화"
        "bookmarked" -> "북마크"
        "recent" -> "최근 본 만화"
        else -> ""
    }
}
