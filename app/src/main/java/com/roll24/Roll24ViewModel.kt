package com.roll24

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roll24.film.FilmDevelopmentEngine
import com.roll24.film.FilmProfile
import com.roll24.film.FilmProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Roll24ViewModel : ViewModel() {
    
    private val filmEngine = FilmDevelopmentEngine()
    
    private val _selectedProfile = MutableStateFlow(FilmProfileRepository.getDefaultProfile())
    val selectedProfile: StateFlow<FilmProfile> = _selectedProfile.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()
    
    private val _showReview = MutableStateFlow(false)
    val showReview: StateFlow<Boolean> = _showReview.asStateFlow()
    
    fun selectProfile(profile: FilmProfile) {
        _selectedProfile.value = profile
    }
    
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            
            try {
                val processed = filmEngine.develop(bitmap, _selectedProfile.value)
                _processedBitmap.value = processed
                _showReview.value = true
            } catch (e: Exception) {
                // Handle error
                _processedBitmap.value = bitmap
                _showReview.value = true
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun dismissReview() {
        _showReview.value = false
        _processedBitmap.value = null
    }
}
