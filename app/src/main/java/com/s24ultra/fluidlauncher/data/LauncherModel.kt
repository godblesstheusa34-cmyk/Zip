package com.s24ultra.fluidlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.text.Collator

object LauncherModel {
    fun load(context: Context, iconSize: Int): List<AppItem> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                runCatching {
                    AppItem(
                        info.loadLabel(pm).toString(),
                        ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                        info.loadIcon(pm).toBitmap(iconSize),
                    )
                }.getOrNull()
            }
            .distinctBy { it.component }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val old = bounds
        setBounds(0, 0, size, size)
        draw(Canvas(result))
        bounds = old
        return result
    }
}
