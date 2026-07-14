package com.example.composeplayground.ui.screen.music

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import com.example.composeplayground.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface PreviewPlaybackState {
    data object Idle : PreviewPlaybackState
    data object Preparing : PreviewPlaybackState
    data object Playing : PreviewPlaybackState
    data object Paused : PreviewPlaybackState
    data class Error(val message: String) : PreviewPlaybackState
}

/**
 * 曲目詳細頁 ViewModel。不呼叫 Repository — 完整 [Track] 已由 NavKey 攜帶，
 * 避免對已在搜尋結果中取得的單筆資料再打一次 API。
 *
 * 自行管理 [MediaPlayer] 生命週期，串流播放 30 秒試聽片段。
 */
class MusicDetailViewModel(
    val track: Track,
) : ViewModel() {

    val uiState: StateFlow<PreviewPlaybackState>
        field = MutableStateFlow<PreviewPlaybackState>(PreviewPlaybackState.Idle)

    private var mediaPlayer: MediaPlayer? = null

    fun togglePlayback() {
        val previewUrl = track.previewUrl
        if (previewUrl.isNullOrBlank()) {
            uiState.value = PreviewPlaybackState.Error("No preview available")
            return
        }
        when (uiState.value) {
            PreviewPlaybackState.Playing -> {
                mediaPlayer?.pause()
                uiState.value = PreviewPlaybackState.Paused
            }
            PreviewPlaybackState.Paused -> {
                mediaPlayer?.start()
                uiState.value = PreviewPlaybackState.Playing
            }
            else -> startPlayback(previewUrl)
        }
    }

    private fun startPlayback(previewUrl: String) {
        releasePlayer()
        uiState.value = PreviewPlaybackState.Preparing
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener {
                it.start()
                uiState.value = PreviewPlaybackState.Playing
            }
            setOnCompletionListener {
                uiState.value = PreviewPlaybackState.Idle
            }
            setOnErrorListener { _, _, _ ->
                uiState.value = PreviewPlaybackState.Error("Playback failed")
                true
            }
            try {
                setDataSource(previewUrl)
                prepareAsync()
            } catch (e: Exception) {
                uiState.value = PreviewPlaybackState.Error(e.localizedMessage ?: "Playback failed")
            }
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        releasePlayer()
    }
}
