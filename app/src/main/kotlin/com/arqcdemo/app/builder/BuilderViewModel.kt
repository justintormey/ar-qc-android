package com.arqcdemo.app.builder

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

/**
 * Builder scene state machine.
 *
 *   Welcome → Instructions → Scanning → VerdictPass | VerdictFail → …
 *   - After PASS, "Next part" → Instructions for the NEXT component.
 *   - After FAIL, "Rework"    → Instructions for the SAME component.
 *   - End session             → Complete → Reset → Welcome.
 *
 * Verdicts arrive from either:
 *   - the on-device QR analyzer (single QR per face → AP/AF/BP/BF/CP/CF)
 *   - the operator's controller (BuilderVerdict bus message)
 * Both feed the same `goVerdict(part, isPass)` action.
 */
sealed class BuilderScene(val statusText: String) {
    data object Welcome :       BuilderScene("Ready")
    data class Instructions(val part: Char) : BuilderScene("Assembly — read steps, then SCAN")
    data object Scanning :      BuilderScene("Scanning…")
    data class VerdictPass(val part: Char) : BuilderScene("Part — PASS")
    data class VerdictFail(val part: Char) : BuilderScene("Part — FAIL")
    data object Complete :      BuilderScene("Session complete")
}

data class BuilderCounts(val pass: Int = 0, val fail: Int = 0) {
    val attempts get() = pass + fail
}

data class BuilderUiState(
    val scene: BuilderScene = BuilderScene.Welcome,
    val counts: BuilderCounts = BuilderCounts(),
    val elapsedMs: Long = 0L,
    val transport: TransportState = TransportState.Initializing,
    val roomPin: String = "",
    val focusedIndex: Int = 0,
    val currentPart: Char = 'A',
    /** Parts that have already passed this session — their PASS QRs are ignored
     *  on subsequent scans so the verdict doesn't keep re-firing while the
     *  passed pieces remain in view on the workbench. FAIL QRs still fire
     *  (relevant for rework loops). */
    val passedParts: Set<Char> = emptySet(),
)

class BuilderViewModel : ViewModel() {

    private val _ui = MutableStateFlow(BuilderUiState())
    val ui: StateFlow<BuilderUiState> = _ui.asStateFlow()

    private var bus: StateBus? = null
    private var firstScanAt: Long = 0L
    private var started = false

    init {
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

    /** QR analyzer entry point — emits the matching verdict. */
    fun onBuilderQrDetected(part: Char, isPass: Boolean) {
        handle(BusMessage.BuilderVerdict(part.toString(), if (isPass) "pass" else "fail"))
    }

    private fun handle(msg: BusMessage) {
        Log.i(TAG, "handle: $msg")
        when (msg) {
            is BusMessage.Scan -> onScanRequested()
            is BusMessage.BuilderVerdict -> goVerdict(msg.part, msg.result == "pass")
            is BusMessage.Complete -> goComplete()
            is BusMessage.Reset -> goWelcome()
            else -> Unit
        }
    }

    /** Operator's SCAN button advances depending on current scene. */
    private fun onScanRequested() {
        val s = _ui.value
        when (s.scene) {
            BuilderScene.Welcome -> goInstructions(s.currentPart)
            is BuilderScene.Instructions -> goScanning()
            is BuilderScene.VerdictPass -> {
                val next = nextPart(s.currentPart)
                if (next == null) goComplete() else goInstructions(next)
            }
            is BuilderScene.VerdictFail -> goInstructions(s.currentPart)
            else -> goScanning()
        }
    }

    // ─── Scroll-wheel handling ─────────────────────────────────────────
    private fun onWheel(ev: WheelEvent) {
        val count = focusableCount(_ui.value.scene)
        if (count == 0) return
        when (ev) {
            WheelEvent.UP -> _ui.update { it.copy(focusedIndex = (it.focusedIndex - 1 + count) % count) }
            WheelEvent.DOWN -> _ui.update { it.copy(focusedIndex = (it.focusedIndex + 1) % count) }
            WheelEvent.CLICK -> activateFocused()
        }
    }

    private fun focusableCount(scene: BuilderScene): Int = when (scene) {
        BuilderScene.Welcome -> 1                                 // Begin
        is BuilderScene.Instructions -> 1                         // Scan when ready
        BuilderScene.Scanning -> 0
        is BuilderScene.VerdictPass -> 2                          // Next part / End session
        is BuilderScene.VerdictFail -> 2                          // Rework / End session
        BuilderScene.Complete -> 1                                // Reset
    }

    private fun activateFocused() {
        val s = _ui.value
        when (s.scene) {
            BuilderScene.Welcome -> goInstructions(s.currentPart)
            is BuilderScene.Instructions -> goScanning()
            BuilderScene.Scanning -> Unit
            is BuilderScene.VerdictPass -> when (s.focusedIndex) {
                0 -> {
                    val next = nextPart(s.currentPart)
                    if (next == null) goComplete() else goInstructions(next)
                }
                1 -> goComplete()
            }
            is BuilderScene.VerdictFail -> when (s.focusedIndex) {
                0 -> goInstructions(s.currentPart)
                1 -> goComplete()
            }
            BuilderScene.Complete -> goWelcome()
        }
    }

    // ─── Scene transitions ─────────────────────────────────────────────
    private fun goWelcome() {
        _ui.update {
            it.copy(
                scene = BuilderScene.Welcome,
                counts = BuilderCounts(),
                elapsedMs = 0L,
                focusedIndex = 0,
                currentPart = 'A',
                passedParts = emptySet(),
            )
        }
        firstScanAt = 0L
        bus?.send(BusMessage.Scene("welcome"))
    }
    private fun goInstructions(part: Char) {
        _ui.update { it.copy(scene = BuilderScene.Instructions(part), focusedIndex = 0, currentPart = part) }
        bus?.send(BusMessage.Scene("instructions-${part}"))
    }
    private fun goScanning() {
        if (firstScanAt == 0L) firstScanAt = System.currentTimeMillis()
        _ui.update { it.copy(scene = BuilderScene.Scanning, focusedIndex = 0) }
        bus?.send(BusMessage.Scene("scanning"))
    }
    private fun goVerdict(partStr: String, isPass: Boolean) {
        val part = partStr.uppercase().firstOrNull() ?: return
        if (part !in "ABC") return
        if (isPass && part in _ui.value.passedParts) {
            Log.i(TAG, "ignoring PASS for $part — already passed this session")
            return
        }
        val newCounts = if (isPass) {
            _ui.value.counts.copy(pass = _ui.value.counts.pass + 1)
        } else {
            _ui.value.counts.copy(fail = _ui.value.counts.fail + 1)
        }
        val newPassed = if (isPass) _ui.value.passedParts + part else _ui.value.passedParts
        val scene = if (isPass) BuilderScene.VerdictPass(part) else BuilderScene.VerdictFail(part)
        _ui.update {
            it.copy(
                scene = scene,
                counts = newCounts,
                focusedIndex = 0,
                currentPart = part,
                passedParts = newPassed,
            )
        }
        bus?.send(BusMessage.Scene("verdict-${part}-${if (isPass) "pass" else "fail"}"))
        bus?.send(BusMessage.VerdictShown(part.toString(), if (isPass) "pass" else "fail"))
    }
    private fun goComplete() {
        val elapsed = if (firstScanAt == 0L) 0L else System.currentTimeMillis() - firstScanAt
        _ui.update { it.copy(scene = BuilderScene.Complete, elapsedMs = elapsed, focusedIndex = 0) }
        bus?.send(BusMessage.Scene("complete"))
    }

    private fun nextPart(p: Char): Char? = when (p) {
        'A' -> 'B'
        'B' -> 'C'
        else -> null
    }

    override fun onCleared() {
        bus?.disconnect()
        super.onCleared()
    }

    companion object {
        private const val TAG = "ARQC.Builder.VM"
    }
}
