package com.arqcdemo.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arqcdemo.app.input.WheelEvent
import com.arqcdemo.app.input.WheelInput
import com.arqcdemo.app.transport.BusMessage
import com.arqcdemo.app.transport.StateBus
import com.arqcdemo.app.transport.TransportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class Scene(val statusText: String) {
    data object Welcome :   Scene("Ready")
    data object Scanning :  Scene("Scanning…")
    data object VerdictA :  Scene("Part A — PASS")
    data object VerdictB :  Scene("Part B — REWORK")
    data object VerdictC :  Scene("Part C — SCRAP")
    data object Complete :  Scene("Demo complete")
}

data class Counts(val a: Int = 0, val b: Int = 0, val c: Int = 0) {
    val total get() = a + b + c
}

/**
 * Each scene declares an ordered list of focusable actions; the wearer's
 * scroll-wheel moves [focusedIndex] across them and clicking activates the
 * focused one.
 */
data class UiState(
    val scene: Scene = Scene.Welcome,
    val counts: Counts = Counts(),
    val elapsedMs: Long = 0L,
    val transport: TransportState = TransportState.Initializing,
    val roomPin: String = "",
    val focusedIndex: Int = 0,
)

/**
 * Single source of truth for the QC state machine.
 *
 *   welcome → scanning → verdict → scanning → … → complete → welcome
 *
 * Transitions arrive from:
 *   - the state bus (operator's laptop controller tapping A/B/C/SCAN/End/Reset)
 *   - the on-device QR analyzer (camera spots A/B/C)
 *   - the Argo scroll wheel (in-headset Accept/Reject/EndSession on Verdict)
 */
class DemoViewModel : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var bus: StateBus? = null
    private var firstScanAt: Long = 0L
    private var started = false

    init {
        // Consume scroll-wheel input for the whole lifetime of this VM.
        viewModelScope.launch {
            WheelInput.events.collect { onWheel(it) }
        }
    }

    fun start(app: Application, roomPin: String) {
        if (started) return
        started = true
        _ui.update { it.copy(roomPin = roomPin) }
        val newBus = StateBus(app.applicationContext, roomPin) { msg -> handle(msg) }
        bus = newBus
        viewModelScope.launch {
            newBus.transportState.collect { state ->
                _ui.update { it.copy(transport = state) }
            }
        }
        newBus.connect()
        newBus.send(BusMessage.Ready)
    }

    fun changeRoomPin(context: Context, newPin: String) {
        if (newPin == _ui.value.roomPin) return
        bus?.disconnect()
        _ui.update { it.copy(roomPin = newPin, transport = TransportState.Initializing) }
        val newBus = StateBus(context.applicationContext, newPin) { msg -> handle(msg) }
        bus = newBus
        viewModelScope.launch {
            newBus.transportState.collect { state ->
                _ui.update { it.copy(transport = state) }
            }
        }
        newBus.connect()
        newBus.send(BusMessage.Ready)
    }

    /** QR analyzer dispatches into this; same code path as the operator's tap. */
    fun onQrDetected(part: Char) {
        when (part) {
            'A', 'a' -> handle(BusMessage.Verdict("A"))
            'B', 'b' -> handle(BusMessage.Verdict("B"))
            'C', 'c' -> handle(BusMessage.Verdict("C"))
        }
    }

    private fun handle(msg: BusMessage) {
        Log.i(TAG, "handle: $msg")
        when (msg) {
            is BusMessage.Scan -> goScanning()
            is BusMessage.Verdict -> goVerdict(msg.part)
            is BusMessage.Complete -> goComplete()
            is BusMessage.Reset -> goWelcome()
            else -> Unit
        }
    }

    // ─── Scroll-wheel handling ─────────────────────────────────────────
    private fun onWheel(ev: WheelEvent) {
        val actionCount = focusableActionCount(_ui.value.scene)
        if (actionCount == 0) return

        when (ev) {
            WheelEvent.UP -> _ui.update { it.copy(focusedIndex = (it.focusedIndex - 1 + actionCount) % actionCount) }
            WheelEvent.DOWN -> _ui.update { it.copy(focusedIndex = (it.focusedIndex + 1) % actionCount) }
            WheelEvent.CLICK -> activateFocused()
        }
    }

    /** Returns the count of focusable actions on the current scene. */
    private fun focusableActionCount(scene: Scene): Int = when (scene) {
        Scene.Welcome -> 1                                          // Begin
        Scene.Scanning -> 0
        Scene.VerdictA, Scene.VerdictB, Scene.VerdictC -> 3         // Accept / Reject / End Session
        Scene.Complete -> 1                                         // Reset
    }

    /** Fires whatever the focused button does for the current scene. */
    private fun activateFocused() {
        val s = _ui.value
        when (s.scene) {
            Scene.Welcome -> goScanning()
            Scene.Scanning -> Unit                                  // no UI buttons
            Scene.VerdictA, Scene.VerdictB, Scene.VerdictC -> when (s.focusedIndex) {
                0 -> { /* Accept */ goScanning() }
                1 -> { /* Reject */ rejectAndScan(s.scene) }
                2 -> { /* End session */ goComplete() }
            }
            Scene.Complete -> goWelcome()
        }
    }

    private fun rejectAndScan(scene: Scene) {
        // Decrement the counter the verdict had bumped, then return to scanning.
        val c = _ui.value.counts
        val rolled = when (scene) {
            Scene.VerdictA -> c.copy(a = (c.a - 1).coerceAtLeast(0))
            Scene.VerdictB -> c.copy(b = (c.b - 1).coerceAtLeast(0))
            Scene.VerdictC -> c.copy(c = (c.c - 1).coerceAtLeast(0))
            else -> c
        }
        _ui.update { it.copy(counts = rolled) }
        goScanning()
    }

    private fun goWelcome() {
        _ui.update {
            it.copy(scene = Scene.Welcome, counts = Counts(), elapsedMs = 0L, focusedIndex = 0)
        }
        firstScanAt = 0L
        bus?.send(BusMessage.Scene("welcome"))
    }
    private fun goScanning() {
        if (firstScanAt == 0L) firstScanAt = System.currentTimeMillis()
        _ui.update { it.copy(scene = Scene.Scanning, focusedIndex = 0) }
        bus?.send(BusMessage.Scene("scanning"))
    }
    private fun goVerdict(part: String) {
        val scene = when (part.uppercase()) {
            "A" -> Scene.VerdictA
            "B" -> Scene.VerdictB
            "C" -> Scene.VerdictC
            else -> return
        }
        val newCounts = when (scene) {
            Scene.VerdictA -> _ui.value.counts.copy(a = _ui.value.counts.a + 1)
            Scene.VerdictB -> _ui.value.counts.copy(b = _ui.value.counts.b + 1)
            Scene.VerdictC -> _ui.value.counts.copy(c = _ui.value.counts.c + 1)
            else -> _ui.value.counts
        }
        // Default focused-index = 0 (Accept).
        _ui.update { it.copy(scene = scene, counts = newCounts, focusedIndex = 0) }
        bus?.send(BusMessage.Scene("verdict-${part.uppercase()}"))
        bus?.send(BusMessage.VerdictShown(part.uppercase()))
    }
    private fun goComplete() {
        val elapsed = if (firstScanAt == 0L) 0L else System.currentTimeMillis() - firstScanAt
        _ui.update { it.copy(scene = Scene.Complete, elapsedMs = elapsed, focusedIndex = 0) }
        bus?.send(BusMessage.Scene("complete"))
    }

    override fun onCleared() {
        bus?.disconnect()
        super.onCleared()
    }

    companion object {
        private const val TAG = "ARQC.VM"
    }
}
