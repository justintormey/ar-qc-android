package com.arqcdemo.app.transport

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Thin wrapper around the AWS API Gateway WebSocket relay used by
 * github.com/justintormey/portal. The HTML v1's
 * src/transport/webrtc-transport.js speaks the same protocol; do not
 * break compatibility.
 *
 * Outgoing wire format:    {"action":"sendmessage","data": <payload>}
 * Incoming wire format:    {"type": "...", ...fields}
 *
 * `payload` always carries a `type` field that the server's routing
 * Lambda inspects (peer-count, should-initiate, peer-joined, offer,
 * answer, ice-candidate, peer-left).
 */
class PortalSignaling(
    private val roomPin: String,
    private val listener: Listener,
    private val signalingUrl: String = DEFAULT_URL,
) {

    interface Listener {
        fun onSignalingOpen()
        fun onSignal(payload: JSONObject)
        fun onSignalingClosed(reason: String)
        fun onSignalingError(t: Throwable)
    }

    private val client = OkHttpClient.Builder().build()
    private var ws: WebSocket? = null

    fun connect() {
        val sep = if (signalingUrl.contains("?")) "&" else "?"
        val url = "$signalingUrl${sep}room=$roomPin"
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "ws open")
                listener.onSignalingOpen()
                // tiny delay before sending join (mirrors portal.js 300ms)
                Thread.sleep(300)
                send(JSONObject().put("type", "join"))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = JSONObject(text)
                    listener.onSignal(obj)
                } catch (t: Throwable) {
                    Log.w(TAG, "bad signaling JSON: $text", t)
                }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "ws closing code=$code reason=$reason")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "ws closed code=$code reason=$reason")
                listener.onSignalingClosed(reason)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "ws failure: ${t.message}")
                listener.onSignalingError(t)
            }
        })
    }

    fun send(payload: JSONObject) {
        val envelope = JSONObject()
            .put("action", "sendmessage")
            .put("data", payload)
        ws?.send(envelope.toString())
    }

    fun close() {
        try { ws?.close(1000, "client close") } catch (_: Throwable) {}
        ws = null
    }

    companion object {
        private const val TAG = "ARQC.Signal"
        const val DEFAULT_URL = "wss://tkdxsgj4md.execute-api.us-east-1.amazonaws.com/v1"
    }
}
