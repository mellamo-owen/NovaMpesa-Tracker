package com.novanest.mpesatracker
import android.content.Context
import android.graphics.Color
import android.view.View

object ThemeManager {
    fun getThemeColor(context: Context): Int {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeIndex = prefs.getInt("color_theme", 0)
        
        return when (themeIndex) {
            0 -> Color.parseColor("#F3E8FF") // Light Lavender
            1 -> Color.parseColor("#E0F2FE") // Light Sky Blue
            2 -> Color.parseColor("#FCE7F3") // Light Pink
            else -> Color.parseColor("#F3E8FF")
        }
    }

    fun applyTheme(context: Context, rootView: View) {
        val color = getThemeColor(context)
        rootView.setBackgroundColor(color)
        (context as? android.app.Activity)?.window?.decorView?.setBackgroundColor(color)
    }
}
