package io.antigravity.clipfloat.design.theme

enum class ThemePreset(
    val displayName: String,
    val dominantColor: Long,       // 60%
    val secondaryColor: Long,      // 30%
    val secondaryBorderColor: Long,
    val textColor: Long,
    val accentColor: Long          // 10%
) {
    CYBER_CHARCOAL(
        displayName = "Cyber Charcoal",
        dominantColor = 0xE00F1115,
        secondaryColor = 0xFF1E232B,
        secondaryBorderColor = 0xFF2A323D,
        textColor = 0xFFE2E8F0,
        accentColor = 0xFF00E5FF
    ),
    AMOLED_PITCH(
        displayName = "AMOLED Pitch",
        dominantColor = 0xF2000000,
        secondaryColor = 0xFF121212,
        secondaryBorderColor = 0xFF262626,
        textColor = 0xFFF5F5F5,
        accentColor = 0xFF10B981
    ),
    RETRO_TERMINAL(
        displayName = "Retro Terminal",
        dominantColor = 0xE60A0F0D,
        secondaryColor = 0xFF152219,
        secondaryBorderColor = 0xFF243B2C,
        textColor = 0xFFA3E635,
        accentColor = 0xFF22C55E
    ),
    MONOCHROME_PRO(
        displayName = "Monochrome Pro",
        dominantColor = 0xEB141414,
        secondaryColor = 0xFF242424,
        secondaryBorderColor = 0xFF383838,
        textColor = 0xFFFFFFFF,
        accentColor = 0xFFFFFFFF
    )
}
