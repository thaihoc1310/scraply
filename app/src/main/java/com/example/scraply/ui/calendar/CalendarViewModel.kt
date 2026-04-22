package com.example.scraply.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.repository.StampRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(
    private val stampRepository: StampRepository,
) : ViewModel() {
    private val _stamps = MutableStateFlow<List<Stamp>>(emptyList())
    val stamps: StateFlow<List<Stamp>> = _stamps.asStateFlow()

    init {
        viewModelScope.launch {
            stampRepository.observeAll().collect { _stamps.value = it }
        }
    }

    fun stampsOnDay(day: LocalDate): List<Stamp> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return _stamps.value.filter { it.createdAt in start..end }
    }

    fun stampsOfMonth(month: YearMonth): Map<LocalDate, List<Stamp>> {
        val zone = ZoneId.systemDefault()
        return _stamps.value.groupBy {
            LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.createdAt), zone)
        }.filterKeys { it.month == month.month && it.year == month.year }
    }
}
