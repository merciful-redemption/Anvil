package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Anvil v5.0 Normative Brand Tokens (Part 2.3)
val AnvilInk = Color(0xFF14181F)
val AnvilSurfaceDark = Color(0xFF1C222D)
val AnvilSurface2Dark = Color(0xFF252D3B)
val AnvilIndigo = Color(0xFF4C5BD4)
val AnvilIndigoLight = Color(0xFF6B7AE8)
val AnvilEmber = Color(0xFFFF9F43)
val AnvilMint = Color(0xFF2DD4A7)
val AnvilCoral = Color(0xFFF0524F)
val AnvilCoralDeep = Color(0xFFD63A37)
val AnvilSky = Color(0xFF4DA8FF)
val AnvilViolet = Color(0xFF8B6CF0)
val AnvilGold = Color(0xFFF5C542)
val AnvilMist = Color(0xFFF4F6FA)
val AnvilSlate = Color(0xFF5B6575)
val AnvilTextLight = Color(0xFFF4F6FA)
val AnvilTextMutedDark = Color(0xFFA3B1C6)

data class AnvilThemePalette(
    val id: String,
    val name: String,
    val category: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val success: Color = AnvilMint,
    val error: Color = AnvilCoral,
    val reward: Color = AnvilGold,
    val contrastBadge: String = "AA"
)

object AnvilThemeCatalog {
    val themes: List<AnvilThemePalette> = listOf(
        AnvilThemePalette(
            id = "anvil-dark",
            name = "Anvil Ink (Default)",
            category = "Signature Anvil",
            isDark = true,
            background = AnvilInk,
            surface = AnvilSurfaceDark,
            surfaceVariant = AnvilSurface2Dark,
            primary = AnvilIndigo,
            onPrimary = Color.White,
            secondary = AnvilEmber,
            tertiary = AnvilViolet,
            onBackground = AnvilTextLight,
            onSurfaceVariant = AnvilTextMutedDark,
            contrastBadge = "AA+"
        ),
        AnvilThemePalette(
            id = "anvil-light",
            name = "Anvil Mist",
            category = "Core",
            isDark = false,
            background = AnvilMist,
            surface = Color.White,
            surfaceVariant = Color(0xFFE6EAF2),
            primary = Color(0xFF3947B8),
            onPrimary = Color.White,
            secondary = Color(0xFFD97706),
            tertiary = Color(0xFF6D28D9),
            onBackground = AnvilInk,
            onSurfaceVariant = AnvilSlate,
            error = AnvilCoralDeep,
            contrastBadge = "AA+"
        ),
        AnvilThemePalette(
            id = "high-contrast-dark",
            name = "High Contrast Dark",
            category = "Accessible (AAA)",
            isDark = true,
            background = Color(0xFF000000),
            surface = Color(0xFF0D1117),
            surfaceVariant = Color(0xFF161B22),
            primary = Color(0xFF82AAFF),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFFFFCA28),
            tertiary = Color(0xFF69F0AE),
            onBackground = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFE6EDF3),
            contrastBadge = "AAA 7:1"
        ),
        AnvilThemePalette(
            id = "high-contrast-light",
            name = "High Contrast Light",
            category = "Accessible (AAA)",
            isDark = false,
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFFE2E8F0),
            primary = Color(0xFF1E293B),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF9A3412),
            tertiary = Color(0xFF1E40AF),
            onBackground = Color(0xFF000000),
            onSurfaceVariant = Color(0xFF1E293B),
            contrastBadge = "AAA 7:1"
        ),
        AnvilThemePalette(
            id = "forge",
            name = "Forge Ember",
            category = "Signature Anvil",
            isDark = true,
            background = Color(0xFF181312),
            surface = Color(0xFF241C1A),
            surfaceVariant = Color(0xFF332724),
            primary = AnvilEmber,
            onPrimary = Color(0xFF181312),
            secondary = AnvilIndigoLight,
            tertiary = AnvilGold,
            onBackground = Color(0xFFFAF3F0),
            onSurfaceVariant = Color(0xFFD4C2BC)
        ),
        AnvilThemePalette(
            id = "terminal",
            name = "Terminal Phosphor",
            category = "Signature Anvil",
            isDark = true,
            background = Color(0xFF0A0F0D),
            surface = Color(0xFF111A16),
            surfaceVariant = Color(0xFF1A2821),
            primary = AnvilMint,
            onPrimary = Color(0xFF06120E),
            secondary = AnvilSky,
            tertiary = AnvilEmber,
            onBackground = Color(0xFFE2F9EE),
            onSurfaceVariant = Color(0xFF8EC5AD)
        ),
        AnvilThemePalette(
            id = "tokyo-night",
            name = "Tokyo Night",
            category = "Community Classics",
            isDark = true,
            background = Color(0xFF1A1B26),
            surface = Color(0xFF24283B),
            surfaceVariant = Color(0xFF2F3549),
            primary = Color(0xFF7AA2F7),
            onPrimary = Color(0xFF15161E),
            secondary = Color(0xFFFF9E64),
            tertiary = Color(0xFFBB9AF7),
            onBackground = Color(0xFFC0CAF5),
            onSurfaceVariant = Color(0xFF9AA5CE)
        ),
        AnvilThemePalette(
            id = "catppuccin-mocha",
            name = "Catppuccin Mocha",
            category = "Community Classics",
            isDark = true,
            background = Color(0xFF1E1E2E),
            surface = Color(0xFF181825),
            surfaceVariant = Color(0xFF313244),
            primary = Color(0xFFCBA6F7),
            onPrimary = Color(0xFF11111B),
            secondary = Color(0xFFFAB387),
            tertiary = Color(0xFFA6E3A1),
            onBackground = Color(0xFFCDD6F4),
            onSurfaceVariant = Color(0xFFBAC2DE)
        ),
        AnvilThemePalette(
            id = "nord",
            name = "Nord Polar",
            category = "Community Classics",
            isDark = true,
            background = Color(0xFF2E3440),
            surface = Color(0xFF3B4252),
            surfaceVariant = Color(0xFF434C5E),
            primary = Color(0xFF88C0D0),
            onPrimary = Color(0xFF2E3440),
            secondary = Color(0xFFEBCB8B),
            tertiary = Color(0xFFB48EAD),
            onBackground = Color(0xFFECEFF4),
            onSurfaceVariant = Color(0xFFD8DEE9)
        ),
        AnvilThemePalette(
            id = "blueprint",
            name = "Architect Blueprint",
            category = "Core",
            isDark = true,
            background = Color(0xFF0D2137),
            surface = Color(0xFF132D4A),
            surfaceVariant = Color(0xFF1C3D63),
            primary = Color(0xFF64B5F6),
            onPrimary = Color(0xFF071524),
            secondary = AnvilGold,
            tertiary = AnvilMint,
            onBackground = Color(0xFFE3F2FD),
            onSurfaceVariant = Color(0xFF90CAF9)
        )
    )

    fun byId(id: String): AnvilThemePalette =
        themes.firstOrNull { it.id == id } ?: themes.first()
}
