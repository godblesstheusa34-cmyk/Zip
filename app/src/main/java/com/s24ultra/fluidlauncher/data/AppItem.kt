package com.s24ultra.fluidlauncher.data

import android.content.ComponentName
import android.graphics.Bitmap

data class AppItem(
    val label: String,
    val component: ComponentName,
    val icon: Bitmap,
)
