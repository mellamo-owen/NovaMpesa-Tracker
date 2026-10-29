package com.novanest.mpesatracker
import android.content.Context
import android.graphics.Color
import android.view.View

object ThemeManager {
    fun applyTheme(context: Context, rootView: View) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeIndex = prefs.getInt("color_theme", 0)
        
        val bgColor = when (themeIndex) {
            0 -> Color.parseColor("#E2E8F0") // Royal (Soft Slate Navy/Grey)
            1 -> Color.parseColor("#90CAF9") // Ocean (Rich Sky Blue)
            2 -> Color.parseColor("#F48FB1") // Pink (Beautiful Blush Pink)
            else -> Color.parseColor("#E2E8F0")
        }
        rootView.setBackgroundColor(bgColor)
    }
}
