package com.arqcdemo.app.transport

import org.json.JSONObject

/**
 * JSON message shapes shared by the HTML and Android clients. Both pages
 * on the Portal-signaled DataChannel speak this exact grammar; do not
 * break compatibility.
 *
 * QC vocabulary:    Ready, Scan, Verdict(part), Complete, Reset, Scene, VerdictShown
 * Builder vocabulary adds: BuilderVerdict(part, result) and Next
 *
 * Cross-app messages (Ready, Scan, Complete, Reset, Scene, Unknown) work
 * unchanged in both apps. QC ignores `builder-verdict`; Builder ignores
 * `verdict`.
 */
sealed class BusMessage {
    data object Ready : BusMessage()
    data object Scan : BusMessage()

    // QC
    data class Verdict(val part: String) : BusMessage()           // "A" | "B" | "C"

    // Builder
    data class BuilderVerdict(val part: String, val result: String) : BusMessage()  // result: "pass" | "fail"
    data object Next : BusMessage()                                // wearer pressed Next/Rework

    // Run-control (cross-app)
    data object Complete : BusMessage()
    data object Reset : BusMessage()

    // Telemetry (cross-app)
    data class Scene(val name: String) : BusMessage()
    data class VerdictShown(val part: String, val result: String? = null) : BusMessage()
    data class Unknown(val raw: String) : BusMessage()
}

internal fun BusMessage.toJson(seq: Long): String {
    val o = JSONObject()
    when (this) {
        BusMessage.Ready -> o.put("kind", "ready")
        BusMessage.Scan -> o.put("kind", "scan")
        BusMessage.Next -> o.put("kind", "next")
        is BusMessage.Verdict -> { o.put("kind", "verdict"); o.put("part", part) }
        is BusMessage.BuilderVerdict -> {
            o.put("kind", "builder-verdict")
            o.put("part", part)
            o.put("result", result)
        }
        BusMessage.Complete -> o.put("kind", "complete")
        BusMessage.Reset -> o.put("kind", "reset")
        is BusMessage.Scene -> { o.put("kind", "scene"); o.put("name", name) }
        is BusMessage.VerdictShown -> {
            o.put("kind", "verdict-shown")
            o.put("part", part)
            if (result != null) o.put("result", result)
        }
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
        "next" -> BusMessage.Next
        "verdict" -> BusMessage.Verdict(o.optString("part"))
        "builder-verdict" -> BusMessage.BuilderVerdict(
            part = o.optString("part"),
            result = o.optString("result", "pass"),
        )
        "complete" -> BusMessage.Complete
        "reset" -> BusMessage.Reset
        "scene" -> BusMessage.Scene(o.optString("name"))
        "verdict-shown" -> BusMessage.VerdictShown(
            part = o.optString("part"),
            result = o.optString("result").ifBlank { null },
        )
        else -> BusMessage.Unknown(raw)
    }
} catch (t: Throwable) {
    BusMessage.Unknown(raw)
}
