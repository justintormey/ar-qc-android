package com.arqcdemo.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class UiState(
    val scene: Scene = Scene.Welcome,
    val counts: Counts = Counts(),
    val elapsedMs: Long = 0L,
    val transport: TransportState = TransportState.Initializing,
    val roomPin: String = "",
)

/**
 * Single source of truth for the demo state machine. Mirrors the HTML
 * v1's src/demo.js exactly so the controller works unchanged:
 *
 *   welcome → scanning → verdictA/B/C → scanning → … → complete → welcome
 *
 * The state bus drives transitions both from the network controller
 * (laptop tapping A/B/C/SCAN/End Demo) AND from the on-device QR
 * detector (camera spots QR-A, QR-B, or QR-C). Both paths fire the same
 * verdict actions.
 */
class DemoViewModel : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var bus: StateBus? = null
    private var firstScanAt: Long = 0L
    private var started = false

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

    /** Called by both the network bus AND the on-device QR analyzer. */
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

    private fun goWelcome() {
        _ui.update {
            it.copy(scene = Scene.Welcome, counts = Counts(), elapsedMs = 0L)
        }
        firstScanAt = 0L
        bus?.send(BusMessage.Scene("welcome"))
    }
    private fun goScanning() {
        if (firstScanAt == 0L) firstScanAt = System.currentTimeMillis()
        _ui.update { it.copy(scene = Scene.Scanning) }
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
        _ui.update { it.copy(scene = scene, counts = newCounts) }
        bus?.send(BusMessage.Scene("verdict-${part.uppercase()}"))
        bus?.send(BusMessage.VerdictShown(part.uppercase()))
    }
    private fun goComplete() {
        val elapsed = if (firstScanAt == 0L) 0L else System.currentTimeMillis() - firstScanAt
        _ui.update { it.copy(scene = Scene.Complete, elapsedMs = elapsed) }
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
