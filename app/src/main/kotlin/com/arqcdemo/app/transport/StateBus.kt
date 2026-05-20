package com.arqcdemo.app.transport

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.webrtc.PeerConnection

/**
 * High-level facade: drives Portal signaling + WebRTC together and
 * exposes a simple send(BusMessage) / onMessage callback to the rest
 * of the app. Mirrors the HTML v1's src/transport/state-bus.js plus
 * the WebRTC transport beneath it.
 *
 * Behavior:
 *   - On connect(), opens the WS to Portal, joins the room.
 *   - When peer-count=2, kicks off the WebRTC handshake (the joining
 *     side becomes initiator and creates the DataChannel).
 *   - Once the DataChannel opens, send()s are wired through it.
 *   - Outgoing messages are queued before open and flushed after.
 *
 * Same room PIN here as on the laptop controller → same DataChannel.
 */
class StateBus(
    appContext: Context,
    private val roomPin: String,
    private val onMessage: (BusMessage) -> Unit,
) : PortalSignaling.Listener {

    private val signaling = PortalSignaling(roomPin, this)
    private val rtc = WebRtcClient(
        appContext,
        signaling,
        onChannelOpen = ::onDcOpen,
        onChannelClose = ::onDcClose,
        onChannelMessage = ::onDcMessage,
        onConnectionState = ::onPcState,
    )

    private val outgoing = ArrayDeque<String>()
    private var seq: Long = 0L
    private var dcOpen = false

    private val _state = MutableStateFlow<TransportState>(TransportState.Initializing)
    val transportState: StateFlow<TransportState> = _state.asStateFlow()

    fun connect() {
        if (!Regex("^\\d{6,8}$").matches(roomPin)) {
            _state.value = TransportState.Error("invalid room PIN")
            return
        }
        _state.value = TransportState.Connecting
        signaling.connect()
    }

    fun disconnect() {
        try { rtc.close() } catch (_: Throwable) {}
        try { signaling.close() } catch (_: Throwable) {}
        dcOpen = false
        outgoing.clear()
    }

    fun send(msg: BusMessage) {
        val text = msg.toJson(++seq)
        if (dcOpen) {
            rtc.send(text)
        } else {
            outgoing.addLast(text)
        }
    }

    // ─── Signaling listener ───────────────────────────────────────────
    override fun onSignalingOpen() {
        _state.value = TransportState.SignalingOpen
    }

    override fun onSignal(payload: JSONObject) {
        val type = payload.optString("type")
        Log.i(TAG, "signal <- $type")
        when (type) {
            "peer-count" -> {
                val n = payload.optInt("count", 0)
                _state.value = TransportState.Peers(n)
            }
            "should-initiate" -> {
                _state.value = TransportState.Initiating
                rtc.initPeer(initiator = true)
                rtc.createOfferAndSend()
            }
            "peer-joined" -> {
                rtc.initPeer(initiator = false)
            }
            "offer" -> {
                val sdp = payload.optJSONObject("sdp") ?: return
                if (_state.value !is TransportState.Connected) {
                    rtc.initPeer(initiator = false)
                }
                _state.value = TransportState.PeerConnecting
                rtc.handleRemoteOffer(sdp)
            }
            "answer" -> {
                val sdp = payload.optJSONObject("sdp") ?: return
                _state.value = TransportState.PeerConnecting
                rtc.handleRemoteAnswer(sdp)
            }
            "ice-candidate" -> {
                val c = payload.optJSONObject("candidate") ?: return
                rtc.addRemoteCandidate(c)
            }
            "peer-left" -> {
                _state.value = TransportState.PeerLeft
                dcOpen = false
                rtc.close()
            }
            else -> Unit
        }
    }

    override fun onSignalingClosed(reason: String) {
        if (_state.value !is TransportState.Connected) {
            _state.value = TransportState.Error("signaling closed: $reason")
        }
    }

    override fun onSignalingError(t: Throwable) {
        _state.value = TransportState.Error(t.message ?: "unknown")
    }

    // ─── DataChannel callbacks ────────────────────────────────────────
    private fun onDcOpen() {
        dcOpen = true
        _state.value = TransportState.Connected
        while (outgoing.isNotEmpty()) {
            rtc.send(outgoing.removeFirst())
        }
    }

    private fun onDcClose() {
        dcOpen = false
        if (_state.value !is TransportState.PeerLeft) {
            _state.value = TransportState.Error("datachannel closed")
        }
    }

    private fun onDcMessage(text: String) {
        try {
            val parsed = parseBusMessage(text)
            Log.i(TAG, "dc <- $parsed")
            onMessage(parsed)
        } catch (t: Throwable) {
            Log.w(TAG, "bad dc message: $text", t)
        }
    }

    private fun onPcState(state: PeerConnection.PeerConnectionState) {
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> { /* dc.open will fire */ }
            PeerConnection.PeerConnectionState.FAILED -> _state.value = TransportState.Error("peer connection failed")
            PeerConnection.PeerConnectionState.CLOSED -> _state.value = TransportState.Error("peer connection closed")
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "ARQC.Bus"
    }
}
