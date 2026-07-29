package com.otaku.manayeou.ui.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otaku.manayeou.data.local.ReadingDirection
import com.otaku.manayeou.data.local.SettingsRepository
import com.otaku.manayeou.data.model.Chapter
import com.otaku.manayeou.data.model.MangaPage
import com.otaku.manayeou.data.model.ViewerMode
import com.otaku.manayeou.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ViewerUiState(
    val pages: List<MangaPage> = emptyList(),
    // -1: 아직 어떤 페이지도 기록되지 않음. 0으로 두면 첫 페이지(인덱스 0) 진입 시
    // "이전과 동일"로 취급돼 읽기 기록이 저장되지 않는 문제가 있었음
    val currentPage: Int = -1,
    val mode: ViewerMode = ViewerMode.WEBTOON,
    val readingDirection: ReadingDirection = ReadingDirection.LTR,
    val twoPageMode: Boolean = false,
    val firstPageSingle: Boolean = true,
    val showControls: Boolean = true,
    val showSettings: Boolean = false,
    val prevChapter: Chapter? = null,
    val nextChapter: Chapter? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ViewerViewModel(
    app: Application,
    private val chapterUrl: String,
    private val chapterTitle: String,
    private val seriesId: String,
    private val seriesUrl: String,
    private val seriesTitle: String,
    private val coverUrl: String
) : AndroidViewModel(app) {

    private val repo = MangaRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val _state = MutableStateFlow(
        ViewerUiState(
            mode = settingsRepo.getDefaultViewerMode(),
            readingDirection = settingsRepo.getReadingDirection(),
            twoPageMode = settingsRepo.getTwoPageMode(),
            firstPageSingle = settingsRepo.getFirstPageSingle()
        )
    )
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()

    init {
        loadPages()
        loadAdjacentChapters()
    }

    private fun loadPages() {
        viewModelScope.launch {
            try {
                val pages = repo.fetchPages(chapterUrl)
                _state.update {
                    it.copy(
                        pages = pages,
                        isLoading = false,
                        error = if (pages.isEmpty()) "페이지를 불러올 수 없습니다." else null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // 이전화/다음화 버튼을 위해 시리즈의 전체 챕터 목록에서 현재 챕터의 앞뒤를 찾음
    private fun loadAdjacentChapters() {
        viewModelScope.launch {
            try {
                val baseUrl = Regex("""^(https?://[^/]+)""").find(chapterUrl)?.groupValues?.get(1) ?: "https://kmana10.net"
                val chapters = repo.fetchChapters("$baseUrl/episode/$seriesId/1/1")
                val index = chapters.indexOfFirst { it.url == chapterUrl }
                if (index == -1) return@launch
                _state.update {
                    it.copy(
                        prevChapter = chapters.getOrNull(index + 1),
                        nextChapter = chapters.getOrNull(index - 1)
                    )
                }
            } catch (e: Exception) {
                // 이전/다음 이동은 부가 기능이라 실패해도 뷰어 자체는 계속 사용 가능해야 함
            }
        }
    }

    fun toggleControls() = _state.update { it.copy(showControls = !it.showControls) }

    fun toggleSettings() = _state.update { it.copy(showSettings = !it.showSettings) }

    fun setMode(mode: ViewerMode) {
        settingsRepo.setDefaultViewerMode(mode)
        _state.update { it.copy(mode = mode) }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        settingsRepo.setReadingDirection(direction)
        _state.update { it.copy(readingDirection = direction) }
    }

    fun setTwoPageMode(value: Boolean) {
        settingsRepo.setTwoPageMode(value)
        _state.update { it.copy(twoPageMode = value) }
    }

    fun setFirstPageSingle(value: Boolean) {
        settingsRepo.setFirstPageSingle(value)
        _state.update { it.copy(firstPageSingle = value) }
    }

    fun onPageChanged(page: Int) {
        val prev = _state.value.currentPage
        _state.update { it.copy(currentPage = page) }
        if (page == prev) return
        val totalPages = _state.value.pages.size
        if (totalPages == 0) return
        viewModelScope.launch {
            repo.recordHistory(
                chapterUrl = chapterUrl,
                chapterTitle = chapterTitle,
                seriesId = seriesId,
                seriesUrl = seriesUrl,
                seriesTitle = seriesTitle,
                coverUrl = coverUrl,
                lastPage = page,
                totalPages = totalPages
            )
        }
    }
}
