package com.bars.exchange.tracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bars.exchange.tracker.domain.model.Asset
import com.bars.exchange.tracker.domain.usecase.GetAvailableAssetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AssetUiState {
    data object Loading : AssetUiState()
    data class Success(val assets: List<Asset>) : AssetUiState()
    data class Error(val message: String) : AssetUiState()
    data object Empty : AssetUiState() // Added for when there are no assets but no error
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAvailableAssetsUseCase: GetAvailableAssetsUseCase
) : ViewModel() {

    private val _assetUiState = MutableStateFlow<AssetUiState>(AssetUiState.Loading)
    val assetUiState: StateFlow<AssetUiState> = _assetUiState.asStateFlow()

    init {
        fetchAssets()
    }

    fun fetchAssets() {
        viewModelScope.launch {
            _assetUiState.value = AssetUiState.Loading
            getAvailableAssetsUseCase()
                .catch { exception -> // Catch exceptions from the flow itself (e.g., network issues before Result is formed)
                    _assetUiState.value = AssetUiState.Error(exception.message ?: "Flow collection error")
                }
                .collect { result -> // Result from the repository
                    result.fold(
                        onSuccess = { assets ->
                            if (assets.isEmpty()) {
                                _assetUiState.value = AssetUiState.Empty
                            } else {
                                _assetUiState.value = AssetUiState.Success(assets)
                            }
                        },
                        onFailure = { exception ->
                            _assetUiState.value = AssetUiState.Error(exception.message ?: "Data retrieval failed")
                        }
                    )
                }
        }
    }
}
