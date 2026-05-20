package com.arqcdemo.app.input

import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Argo scroll-wheel + click input, surfaced as Android KeyEvents.
 *
 * On the Digilens Argo the temple-mounted scroll wheel emits standard
 * d-pad events: UP / DOWN for rotation, CENTER (and sometimes ENTER) for
 * click. Activities forward their KeyEvents into [dispatch] and the rest
 * of the app consumes the resulting [WheelEvent]s via [events].
 *
 * ViewModels observe the flow and translate scroll into focus-index
 * changes; click activates the focused on-screen action.
 */
enum class WheelEvent { UP, DOWN, CLICK }

object WheelInput {
    private const val TAG = "ARQC.Wheel"

    private val _events = MutableSharedFlow<WheelEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<WheelEvent> = _events.asSharedFlow()

    /** Returns true if this event was consumed by the wheel-input pipeline. */
    fun dispatch(ev: KeyEvent): Boolean {
        if (ev.action != KeyEvent.ACTION_DOWN) return false
        val mapped = when (ev.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_VOLUME_UP -> WheelEvent.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> WheelEvent.DOWN
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> WheelEvent.CLICK
            else -> null
        } ?: return false
        Log.i(TAG, "$mapped (key=${ev.keyCode})")
        _events.tryEmit(mapped)
        return true
    }
}
