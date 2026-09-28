package com.zimapp.zim.ui.settings.lock

import androidx.compose.geometry.Offset
import androidx.compose.geometry.Size
import androidx.compose.graphics.Path
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// UI state for the passcode pad + pattern canvas (ported from EasyNotes
// LockScreenViewModel; persistence handled by callers via AppSettings).
class LockViewModel : ViewModel() {
    val selectedCellsIndexList = mutableStateListOf<Int?>()
    val selectedCellCenterList = mutableStateListOf<Offset>()
    var canvasSize by mutableStateOf(Size.Zero)
    var currentTouchOffset by mutableStateOf<Offset?>(null)
    var lastCellCenter by mutableStateOf<Offset?>(null)
    var path by mutableStateOf(Path())

    private val _pinCode: MutableState<List<Int>> = mutableStateOf(emptyList())
    val pinCode: State<List<Int>> = _pinCode

    private val _isPinIncorrect = mutableStateOf(false)
    val isPinIncorrect: State<Boolean> = _isPinIncorrect

    private val _animateError = mutableStateOf(false)
    val animateError: State<Boolean> = _animateError

    fun updatePath(cellCenter: Offset) {
        lastCellCenter?.let { path.lineTo(it.x, it.y) }
        lastCellCenter = cellCenter
        path.lineTo(cellCenter.x, cellCenter.y)
    }

    fun clearPattern() {
        selectedCellsIndexList.clear()
        selectedCellCenterList.clear()
        path = Path()
        currentTouchOffset = null
        lastCellCenter = null
    }

    // expected == null → setup mode (returns the new pin); else verify mode.
    fun addNumber(number: Int, expected: String?, onSetup: (String) -> Unit, onUnlock: () -> Unit) {
        viewModelScope.launch {
            if (_pinCode.value.size < 6) {
                _pinCode.value += number
                if (_pinCode.value.size == 6) {
                    val pin = _pinCode.value.joinToString("")
                    if (expected == null) {
                        onSetup(pin)
                    } else if (pin == expected) {
                        onUnlock()
                    } else {
                        _isPinIncorrect.value = true
                        _animateError.value = true
                        delay(500)
                        _animateError.value = false
                        onReset()
                    }
                }
            }
        }
    }

    fun removeNumber() {
        viewModelScope.launch {
            if (_pinCode.value.isNotEmpty()) _pinCode.value = _pinCode.value.dropLast(1)
        }
    }

    fun onReset() {
        _pinCode.value = emptyList()
        _isPinIncorrect.value = false
        _animateError.value = false
    }
}
