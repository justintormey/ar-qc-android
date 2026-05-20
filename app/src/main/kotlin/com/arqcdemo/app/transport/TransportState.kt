package com.arqcdemo.app.transport

/** Surfaces the connection lifecycle for the UI's connection pill. */
sealed class TransportState(val text: String) {
    data object Initializing : TransportState("Initializing")
    data object Connecting : TransportState("Connecting…")
    data object SignalingOpen : TransportState("Signaling open")
    data class Peers(val n: Int) : TransportState("peers:$n")
    data object Initiating : TransportState("Initiating")
    data object PeerConnecting : TransportState("Negotiating peer connection…")
    data object Connected : TransportState("Connected")
    data object PeerLeft : TransportState("Peer left")
    data class Error(val message: String) : TransportState("Error: $message")
}
