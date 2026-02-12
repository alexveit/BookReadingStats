package com.bookstats.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.domain.model.CategoryStatistics
import com.bookstats.domain.model.DailyChartData
import com.bookstats.domain.model.ReadingStatistics
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Statistics screen.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {
    
    private val _statistics = MutableStateFlow(ReadingStatistics())
    val statistics: StateFlow<ReadingStatistics> = _statistics.asStateFlow()
    
    private val _chartData = MutableStateFlow<List<DailyChartData>>(emptyList())
    val chartData: StateFlow<List<DailyChartData>> = _chartData.asStateFlow()

    private val _categoryStatistics = MutableStateFlow<List<CategoryStatistics>>(emptyList())
    val categoryStatistics: StateFlow<List<CategoryStatistics>> = _categoryStatistics.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeStatistics()
        observeChartData()
        observeCategoryStatistics()
    }
    
    private fun observeStatistics() {
        viewModelScope.launch {
            repository.getReadingStatistics().collect { stats ->
                _statistics.value = stats
                _isLoading.value = false
            }
        }
    }
    
    private fun observeChartData() {
        viewModelScope.launch {
            repository.getDailyChartData().collect { data ->
                _chartData.value = data
            }
        }
    }

    private fun observeCategoryStatistics() {
        viewModelScope.launch {
            repository.getCategoryStatistics().collect { stats ->
                _categoryStatistics.value = stats
            }
        }
    }

    suspend fun exportData(): String {
        return repository.exportToJson()
    }
    
    suspend fun importData(json: String): Result<Int> {
        return repository.importFromJson(json)
    }
}
