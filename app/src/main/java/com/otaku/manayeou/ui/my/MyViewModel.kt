package com.otaku.manayeou.ui.my

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

data class MyUiState(
    val bookmarked: List<Series> = emptyList(),
    val recent: List<Series> = emptyList()
)

// kmana10 연동 없이 앱 로컬 DB(북마크·읽기기록)만으로 구성되는 화면
class MyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MangaRepository(app)

    private val _state = MutableStateFlow(MyUiState())
    val state: StateFlow<MyUiState> = _state.asStateFlow()

    init {
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
}
