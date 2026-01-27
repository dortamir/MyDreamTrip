package com.example.mydreamtrip.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydreamtrip.data.remote.wiki.DestinationInfo
import com.example.mydreamtrip.data.remote.wiki.WikiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DestinationInfoState {
    data object Idle : DestinationInfoState()
    data object Loading : DestinationInfoState()
    data class Success(val info: DestinationInfo) : DestinationInfoState()
    data class Error(val message: String) : DestinationInfoState()
}

class AddViewModel : ViewModel() {

    private val repo = WikiRepository()

    private val _state = MutableStateFlow<DestinationInfoState>(DestinationInfoState.Idle)
    val state: StateFlow<DestinationInfoState> = _state

    fun fetchDestinationInfo(location: String) {
        val q = location.trim()
        if (q.isBlank()) {
            _state.value = DestinationInfoState.Error("Please enter a location")
            return
        }

        _state.value = DestinationInfoState.Loading

        viewModelScope.launch {
            try {
                val info = repo.fetchDestinationInfo(q)
                _state.value = DestinationInfoState.Success(info)
            } catch (e: Exception) {
                _state.value = DestinationInfoState.Error(e.message ?: "Failed to fetch destination info")
            }
        }
    }

    fun reset() {
        _state.value = DestinationInfoState.Idle
    }
}
