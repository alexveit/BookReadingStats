package com.bookstats.ui.screens.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.ChartType
import com.bookstats.domain.model.DailyChartData
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Chart screen.
 */
@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: BookRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chartData = MutableStateFlow<List<DailyChartData>>(emptyList())
    val chartData: StateFlow<List<DailyChartData>> = _chartData.asStateFlow()

    // Persisted across process death
    val chartType: StateFlow<ChartType> = savedStateHandle.getStateFlow(KEY_CHART_TYPE, ChartType.PAGES)

    // Persisted across process death (1 = daily, 7 = weekly)
    val aggregationDays: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_AGGREGATION_DAYS, 1)
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _book.value = book
            }
        }
        
        viewModelScope.launch {
            repository.getDailyChartDataForBook(bookId).collect { data ->
                _chartData.value = aggregateData(data, aggregationDays.value)
                _isLoading.value = false
            }
        }
    }
    
    fun setChartType(type: ChartType) {
        savedStateHandle[KEY_CHART_TYPE] = type
    }

    fun setAggregationDays(days: Int) {
        savedStateHandle[KEY_AGGREGATION_DAYS] = days
        viewModelScope.launch {
            repository.getDailyChartDataForBook(bookId).first().let { data ->
                _chartData.value = aggregateData(data, days)
            }
        }
    }

    companion object {
        private const val KEY_CHART_TYPE = "chartType"
        private const val KEY_AGGREGATION_DAYS = "aggregationDays"
    }
    
    private fun aggregateData(data: List<DailyChartData>, days: Int): List<DailyChartData> {
        if (days == 1 || data.isEmpty()) return data
        
        return data.chunked(days).map { chunk ->
            DailyChartData(
                date = chunk.first().date,
                pagesRead = chunk.sumOf { it.pagesRead },
                minutesRead = chunk.sumOf { it.minutesRead }
            )
        }
    }
}
