package io.antigravity.clipfloat.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SlotItem(
    val id: Int,
    val text: String,
    val isSensitive: Boolean = false
)

object ClipboardRepository {
    private const val MAX_CAPACITY = 8

    private val _slots = MutableStateFlow(
        List(MAX_CAPACITY) { index ->
            SlotItem(
                id = index,
                text = when (index) {
                    0 -> "Alpha Payload"
                    1 -> "Beta Payload"
                    2 -> "Gamma Payload"
                    3 -> "Delta Payload"
                    else -> ""
                }
            )
        }
    )
    val slotsStream: StateFlow<List<SlotItem>> = _slots.asStateFlow()

    fun updateSlot(index: Int, rawPayload: String, config: EngineConfig) {
        if (index !in 0 until MAX_CAPACITY) return
        val sanitized = sanitize(rawPayload, config)
        _slots.update { list ->
            list.toMutableList().apply {
                this[index] = this[index].copy(text = sanitized)
            }
        }
    }

    fun getSlotPayload(index: Int): String {
        return _slots.value.getOrNull(index)?.text.orEmpty()
    }

    private fun sanitize(input: String, config: EngineConfig): String {
        var res = input
        if (config.autoTrim) res = res.trim()
        if (config.stripTrackers) {
            res = res.replace(Regex("([?&])(utm_[^&]+|fbclid=[^&]+|gclid=[^&]+)"), "")
                .replace(Regex("\\?$"), "")
        }
        return res
    }

    fun wipeSensitiveMemory() {
        _slots.update { list ->
            list.map { it.copy(text = "") }
        }
    }
}
