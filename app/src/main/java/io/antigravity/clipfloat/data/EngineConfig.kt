package io.antigravity.clipfloat.data

import io.antigravity.clipfloat.design.theme.ThemePreset

data class EngineConfig(
    val slotCount: Int = 4,
    val theme: ThemePreset = ThemePreset.CYBER_CHARCOAL,
    val hudOpacity: Float = 0.88f,
    val autoTrim: Boolean = true,
    val stripTrackers: Boolean = true,
    val hapticsEnabled: Boolean = true
)
