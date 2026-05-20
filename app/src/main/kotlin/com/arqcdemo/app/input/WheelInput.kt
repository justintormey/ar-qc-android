package com.arqcdemo.app.input

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Argo scroll-wheel + click input.
 *
 * What the wheel actually sends is hardware-specific. The Argo has been
 * observed to emit d-pad CENTER for click — but scroll rotation may come
 * through as: DPAD_UP/DOWN, VOLUME_UP/DOWN, PAGE_UP/DOWN, TAB, or via
 * MotionEvent.ACTION_SCROLL with AXIS_SCROLL or AXIS_VSCROLL. We map a
 * superset and log every unmapped event at INFO so we can see what's
 * coming through during diagnostic runs.
 *
 * Activities forward their KeyEvents into [dispatchKey] and their
 * MotionEvents into [dispatchMotion]. ViewModels observe [events] and
 * translate scroll into focus-index changes; click activates the focused
 * on-screen action.
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

    /** Returns true if this KeyEvent was consumed by the wheel-input pipeline. */
    fun dispatchKey(ev: KeyEvent): Boolean {
        if (ev.action != KeyEvent.ACTION_DOWN) return false

        val mapped = when (ev.keyCode) {
            // The Argo's rotary wheel surfaces as DPAD_LEFT/RIGHT (the device
            // is in landscape and the OEM mapped the rotary as horizontal).
            // Treat LEFT as "scroll up / focus up" and RIGHT as "down".
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND -> WheelEvent.UP

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> WheelEvent.DOWN

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A -> WheelEvent.CLICK

            else -> null
        }
        // Always log the keyCode so unknown keys are visible during diagnostic runs.
        Log.i(TAG, "keyCode=${ev.keyCode} name=${KeyEvent.keyCodeToString(ev.keyCode)} mapped=${mapped ?: "(unmapped)"} src=0x${Integer.toHexString(ev.source)}")
        if (mapped == null) return false
        _events.tryEmit(mapped)
        return true
    }

    /** Backwards-compat alias for code still calling dispatch(). */
    @Deprecated("Use dispatchKey", ReplaceWith("dispatchKey(ev)"))
    fun dispatch(ev: KeyEvent): Boolean = dispatchKey(ev)

    /** Returns true if this MotionEvent was consumed by the wheel-input pipeline. */
    fun dispatchMotion(ev: MotionEvent): Boolean {
        // Generic motion events for mouse/wheel/rotary scroll show up as
        // ACTION_SCROLL with non-zero AXIS_VSCROLL (or AXIS_SCROLL on some
        // newer devices). Negative = down, positive = up.
        if (ev.action != MotionEvent.ACTION_SCROLL) return false
        val vscroll = ev.getAxisValue(MotionEvent.AXIS_VSCROLL)
        val scroll = ev.getAxisValue(MotionEvent.AXIS_SCROLL)
        val hscroll = ev.getAxisValue(MotionEvent.AXIS_HSCROLL)
        Log.i(TAG, "motion vscroll=$vscroll scroll=$scroll hscroll=$hscroll src=0x${Integer.toHexString(ev.source)}")
        val delta = if (vscroll != 0f) vscroll else if (scroll != 0f) scroll else hscroll
        if (delta == 0f) return false
        val mapped = if (delta > 0f) WheelEvent.UP else WheelEvent.DOWN
        _events.tryEmit(mapped)
        return true
    }
}
