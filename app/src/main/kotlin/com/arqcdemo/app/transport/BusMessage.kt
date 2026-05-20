package com.arqcdemo.app.transport

import org.json.JSONObject

/**
 * Message shapes shared across the v1 (HTML) and v2 (Android) clients.
 * Both pages on the Portal-signaled DataChannel speak this exact JSON
 * grammar; do not break compatibility.
 */
sealed class BusMessage {
    data object Ready : BusMessage()
    data object Scan : BusMessage()
    data class Verdict(val part: String) : BusMessage()         // "A" | "B" | "C"
    data object Complete : BusMessage()
    data object Reset : BusMessage()
    data class Scene(val name: String) : BusMessage()
    data class VerdictShown(val part: String) : BusMessage()
    data class Unknown(val raw: String) : BusMessage()
}

internal fun BusMessage.toJson(seq: Long): String {
    val o = JSONObject()
    when (this) {
        BusMessage.Ready -> o.put("kind", "ready")
        BusMessage.Scan -> o.put("kind", "scan")
        is BusMessage.Verdict -> { o.put("kind", "verdict"); o.put("part", part) }
        BusMessage.Complete -> o.put("kind", "complete")
        BusMessage.Reset -> o.put("kind", "reset")
        is BusMessage.Scene -> { o.put("kind", "scene"); o.put("name", name) }
        is BusMessage.VerdictShown -> { o.put("kind", "verdict-shown"); o.put("part", part) }
        is BusMessage.Unknown -> Unit
    }
    o.put("_seq", seq)
    return o.toString()
}

internal fun parseBusMessage(raw: String): BusMessage = try {
    val o = JSONObject(raw)
    when (o.optString("kind")) {
        "ready" -> BusMessage.Ready
        "scan" -> BusMessage.Scan
        "verdict" -> BusMessage.Verdict(o.optString("part"))
        "complete" -> BusMessage.Complete
        "reset" -> BusMessage.Reset
        "scene" -> BusMessage.Scene(o.optString("name"))
        "verdict-shown" -> BusMessage.VerdictShown(o.optString("part"))
        else -> BusMessage.Unknown(raw)
    }
} catch (t: Throwable) {
    BusMessage.Unknown(raw)
}
