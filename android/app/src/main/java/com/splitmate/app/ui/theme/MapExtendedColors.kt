package com.splitmate.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.splitmate.app.ui.SplitMateExpressivePalette

/**
 * v2.4.0 rc15: colour roles for the Loop map (MapLibre GL JS style + markers). Every role is
 * DERIVED from the active theme palette (Sunlit Buckwheat / Warm Espresso Night / Kyoto Matcha),
 * so the map follows the app's tokens and this file adds no new hex values.
 *
 * mapcn-like calm basemap: land is the canvas, roads are the card surface, parks are sage, water
 * is periwinkle, labels are muted; the route is the brand primary with a card-coloured casing.
 */
data class MapColorRoles(
    val land: Color,
    val residential: Color,
    val park: Color,
    val water: Color,
    val waterLabel: Color,
    val building: Color,
    val roadMinor: Color,
    val roadMajor: Color,
    val roadCasing: Color,
    val rail: Color,
    val boundary: Color,
    val label: Color,
    val labelHalo: Color,
    val route: Color,
    val routeCasing: Color,
    val stop: Color,
    val onStop: Color,
    val stopRing: Color,
    val stay: Color,
    val onStay: Color
)

object MapExtendedColors {
    fun of(p: SplitMateExpressivePalette): MapColorRoles {
        val dark = p.mode.isDark
        val land = p.surfaceContainerLow
        return MapColorRoles(
            land = land,
            residential = p.surfaceContainer,
            park = p.primaryContainer.copy(alpha = if (dark) 0.45f else 0.7f).compositeOver(land),
            water = p.tertiaryContainer.copy(alpha = if (dark) 0.6f else 0.85f).compositeOver(land),
            waterLabel = p.onTertiaryContainer,
            building = p.surfaceContainerHigh,
            roadMinor = if (dark) p.surfaceContainerHigh else p.surfaceContainerLowest,
            roadMajor = if (dark) p.surfaceContainerHighest else p.surfaceContainerLowest,
            roadCasing = p.outlineVariant,
            rail = p.outline,
            boundary = p.outline,
            label = p.onSurfaceVariant,
            labelHalo = land,
            route = p.primary,
            routeCasing = p.surfaceContainerLowest,
            stop = p.primary,
            onStop = p.onPrimary,
            stopRing = p.surfaceContainerLowest,
            stay = p.onSurface,
            onStay = p.surfaceContainerLowest
        )
    }
}
