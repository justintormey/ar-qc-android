package com.arqcdemo.app.transport

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Native WebRTC peer using io.github.webrtc-sdk's android port.
 *
 * Speaks the same handshake protocol as the HTML v1's
 * src/transport/webrtc-transport.js. The peer that joins SECOND becomes
 * the initiator (creates the DataChannel + sends the offer).
 *
 * The DataChannel is labelled "argo-qc" with ordered+reliable delivery —
 * identical to the v1 client so cross-version sessions work.
 */
class WebRtcClient(
    appContext: Context,
    private val signaling: PortalSignaling,
    private val onChannelOpen: () -> Unit,
    private val onChannelClose: () -> Unit,
    private val onChannelMessage: (String) -> Unit,
    private val onConnectionState: (PeerConnection.PeerConnectionState) -> Unit,
) {

    private val eglBase: EglBase by lazy { EglBase.create() }
    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private var isInitiator = false
    private var remoteSet = false

    /** Build the PeerConnection. Must be called BEFORE any handshake step. */
    fun initPeer(initiator: Boolean) {
        if (pc != null) return
        isInitiator = initiator

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val payload = JSONObject().put("type", "ice-candidate")
                    .put("candidate", JSONObject()
                        .put("candidate", candidate.sdp)
                        .put("sdpMid", candidate.sdpMid)
                        .put("sdpMLineIndex", candidate.sdpMLineIndex))
                signaling.send(payload)
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.i(TAG, "pc state -> $newState")
                onConnectionState(newState)
            }
            override fun onAddStream(p0: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
            override fun onDataChannel(channel: DataChannel) {
                Log.i(TAG, "ondatachannel: ${channel.label()}")
                attachDataChannel(channel)
            }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out org.webrtc.MediaStream>?) {}
        }) ?: error("createPeerConnection returned null")

        if (initiator) {
            val init = DataChannel.Init().apply { ordered = true }
            val ch = pc!!.createDataChannel(CHANNEL_LABEL, init)
            attachDataChannel(ch)
        }
    }

    private fun attachDataChannel(channel: DataChannel) {
        dc = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {
                val s = channel.state()
                Log.i(TAG, "dc state -> $s")
                when (s) {
                    DataChannel.State.OPEN -> onChannelOpen()
                    DataChannel.State.CLOSED -> onChannelClose()
                    else -> Unit
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                val text = String(bytes, StandardCharsets.UTF_8)
                onChannelMessage(text)
            }
        })
    }

    fun createOfferAndSend() {
        val pc = pc ?: return
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(noopObserver, sdp)
                signaling.send(
                    JSONObject().put("type", "offer")
                        .put("sdp", JSONObject().put("type", sdp.type.canonicalForm()).put("sdp", sdp.description))
                )
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { Log.w(TAG, "createOffer fail: $p0") }
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun handleRemoteOffer(sdp: JSONObject) {
        val pc = pc ?: return
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdp.optString("sdp"))
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                remoteSet = true
                flushCandidates()
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        pc.setLocalDescription(noopObserver, answer)
                        signaling.send(
                            JSONObject().put("type", "answer")
                                .put("sdp", JSONObject().put("type", answer.type.canonicalForm()).put("sdp", answer.description))
                        )
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) { Log.w(TAG, "createAnswer fail: $p0") }
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) { Log.w(TAG, "setRemote(offer) fail: $p0") }
        }, desc)
    }

    fun handleRemoteAnswer(sdp: JSONObject) {
        val pc = pc ?: return
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp.optString("sdp"))
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                remoteSet = true
                flushCandidates()
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) { Log.w(TAG, "setRemote(answer) fail: $p0") }
        }, desc)
    }

    fun addRemoteCandidate(json: JSONObject) {
        val candidate = IceCandidate(
            json.optString("sdpMid"),
            json.optInt("sdpMLineIndex"),
            json.optString("candidate"),
        )
        if (remoteSet) {
            pc?.addIceCandidate(candidate)
        } else {
            pendingCandidates.add(candidate)
        }
    }

    private fun flushCandidates() {
        val pc = pc ?: return
        val it = pendingCandidates.iterator()
        while (it.hasNext()) {
            pc.addIceCandidate(it.next())
            it.remove()
        }
    }

    fun send(text: String) {
        val ch = dc ?: return
        if (ch.state() != DataChannel.State.OPEN) return
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        ch.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
    }

    fun close() {
        try { dc?.close() } catch (_: Throwable) {}
        try { pc?.close() } catch (_: Throwable) {}
        dc = null
        pc = null
        pendingCandidates.clear()
        remoteSet = false
    }

    private val noopObserver = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }

    companion object {
        private const val TAG = "ARQC.RTC"
        private const val CHANNEL_LABEL = "argo-qc"
    }
}
