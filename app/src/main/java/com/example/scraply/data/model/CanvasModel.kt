package com.example.scraply.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class CanvasElementType { STAMP, TAPE, STICKER, PAPER_CUT, POLAROID, TEXT }

@Serializable
data class CanvasElement(
    val id: String,
    val type: CanvasElementType,
    val assetKey: String = "",
    val stampId: String? = null,
    val text: String = "",
    val font: String = "serif",
    val color: Long = 0xFF1F1B18L,
    val x: Float = 0.5f,
    val y: Float = 0.35f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
)

@Serializable
data class CanvasState(
    val elements: List<CanvasElement> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(raw: String): CanvasState = try {
            json.decodeFromString(serializer(), raw)
        } catch (_: Throwable) {
            CanvasState()
        }

        fun toJson(state: CanvasState): String = json.encodeToString(serializer(), state)
    }
}
