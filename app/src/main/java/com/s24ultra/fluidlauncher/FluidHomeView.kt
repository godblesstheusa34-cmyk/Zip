package com.s24ultra.fluidlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.accessibility.AccessibilityEvent
import com.s24ultra.fluidlauncher.config.TransitionConfig
import com.s24ultra.fluidlauncher.data.AppItem
import com.s24ultra.fluidlauncher.physics.SpringPhysics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class FluidHomeView(context: Context) : View(context) {
    var apps: List<AppItem> = emptyList()
        set(value) { field = value; invalidate() }
    val config = TransitionConfig()
    var onOpenTuning: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 0, 0, 0)
    }
    private val spring = SpringPhysics()
    private var page = 0
    private var downX = 0f
    private var downY = 0f
    private var touchX = 0f
    private var touchY = 0f
    private var lastY = 0f
    private var downAt = 0L
    private var dragging = false
    private var drawer = 0f
    private var drawerTarget = 0f
    private var drawerScroll = 0f
    private var lastFrame = 0L
    private var velocity: VelocityTracker? = null
    private val iconRects = ArrayList<Pair<RectF, AppItem>>()
    private val drawerButton = RectF()
    private val pageSize get() = 20
    private val pageCount get() = max(1, (apps.size + pageSize - 1) / pageSize)

    init {
        isFocusable = true
        contentDescription = "FluidLauncher home screen"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0f else (now - lastFrame) / 1_000_000_000f
        lastFrame = now
        var animating = false
        if (!dragging && spring.step(if (config.debugSlowMotion) dt * .2f else dt, config.springStiffness, config.settlingDamping)) animating = true
        val drawerDelta = drawerTarget - drawer
        if (abs(drawerDelta) > .002f) {
            drawer += drawerDelta * min(1f, dt * 12f)
            animating = true
        } else drawer = drawerTarget

        drawShade(canvas)
        iconRects.clear()
        if (drawer < .995f) drawDesktop(canvas)
        if (drawer > .005f) drawDrawer(canvas)
        if (animating || dragging) postInvalidateOnAnimation()
    }

    private fun drawShade(canvas: Canvas) {
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0x35151B36, 0x80101422.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun drawDesktop(canvas: Canvas) {
        val offset = spring.position * width
        for (relative in -1..1) {
            val targetPage = page + relative
            if (targetPage !in 0 until pageCount) continue
            drawPage(canvas, targetPage, relative * width + offset)
        }
        paint.color = Color.WHITE
        paint.alpha = (210 * (1f - drawer)).toInt()
        val dotGap = 15f * density
        val start = width / 2f - (pageCount - 1) * dotGap / 2f
        repeat(pageCount) { i -> canvas.drawCircle(start + i * dotGap, height - 106f * density, if (i == page) 3.5f * density else 2f * density, paint) }
        paint.alpha = 255
        val size = 57f * density
        drawerButton.set(width / 2f - size / 2, height - 88f * density, width / 2f + size / 2, height - 31f * density)
        paint.color = 0x45FFFFFF
        canvas.drawRoundRect(drawerButton, 22f * density, 22f * density, paint)
        paint.color = Color.WHITE
        repeat(3) { r -> repeat(3) { c -> canvas.drawCircle(drawerButton.left + 20f * density + c * 8.5f * density, drawerButton.top + 18f * density + r * 8.5f * density, 1.6f * density, paint) } }
    }

    private fun drawPage(canvas: Canvas, pageIndex: Int, baseX: Float) {
        val columns = 4
        val top = paddingTop + 62f * density
        val usable = height - top - paddingBottom - 185f * density
        val cellW = width / columns.toFloat()
        val cellH = usable / 5f
        val icon = min(68f * density, cellW * .62f)
        val start = pageIndex * pageSize
        val end = min(apps.size, start + pageSize)
        for (index in start until end) {
            val local = index - start
            val cx = baseX + (local % columns + .5f) * cellW
            val cy = top + (local / columns + .43f) * cellH
            val wave = waveAt(cx, cy)
            val z = abs(wave) * config.zElevationMax * density
            val shift = wave * config.horizontalDisplacement * density
            val scale = 1f + z / (1800f * density)
            drawIcon(canvas, apps[index], cx + shift, cy - z * .11f, icon, scale, z, 1f - drawer, drawer == 0f && abs(baseX) < width / 2f)
        }
    }

    private fun waveAt(x: Float, y: Float): Float {
        if (!dragging && abs(spring.position) < .0002f) return 0f
        val sx = max(1f, config.waveWidth * density)
        val sy = max(1f, config.fingerVerticalSpread * density)
        val direction = if (touchX - downX >= 0) 1f else -1f
        val crest = touchX - config.curvatureStrength * (y - touchY) * (y - touchY) / (height * 1.8f) * direction
        val dx = x - crest
        val dy = y - touchY
        val envelope = exp(-(dx * dx / (2f * sx * sx) + dy * dy / (2f * sy * sy)))
        return (envelope * cos((2.0 * PI * dx / (sx * 1.7f))).toFloat() * min(1f, abs(spring.position) * 2.6f))
    }

    private fun drawIcon(canvas: Canvas, app: AppItem, cx: Float, cy: Float, size: Float, scale: Float, elevation: Float, opacity: Float, clickable: Boolean) {
        val half = size * scale / 2f
        val rect = RectF(cx - half, cy - half, cx + half, cy + half)
        canvas.drawRoundRect(RectF(rect.left + elevation * .08f, rect.top + elevation * .12f, rect.right + elevation * .08f, rect.bottom + elevation * .12f), size * .24f, size * .24f, shadowPaint)
        paint.alpha = (255 * opacity).toInt()
        canvas.drawBitmap(app.icon, null, rect, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f * density
        paint.color = Color.WHITE
        paint.setShadowLayer(4f * density, 0f, 1f * density, Color.BLACK)
        canvas.drawText(app.label.take(18), cx, rect.bottom + 20f * density, paint)
        paint.clearShadowLayer()
        paint.alpha = 255
        if (clickable) iconRects += RectF(rect.left - 8f * density, rect.top - 8f * density, rect.right + 8f * density, rect.bottom + 28f * density) to app
    }

    private fun drawDrawer(canvas: Canvas) {
        val alpha = (drawer * 245).toInt()
        paint.color = Color.argb(alpha, 10, 13, 24)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val titleY = paddingTop + 50f * density
        paint.color = Color.WHITE
        paint.alpha = (255 * drawer).toInt()
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 27f * density
        canvas.drawText("All apps", 24f * density, titleY, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 15f * density
        canvas.drawText("Swipe down to close", width - 24f * density, titleY, paint)
        val cols = 4
        val cellW = width / cols.toFloat()
        val cellH = 104f * density
        val top = titleY + 35f * density - drawerScroll
        val iconSize = 59f * density
        apps.forEachIndexed { i, app ->
            val cy = top + (i / cols) * cellH + cellH / 2
            if (cy < titleY || cy > height + cellH) return@forEachIndexed
            val theta = (cy - height / 2f) / (1600f * density)
            val curvedScale = 1f - (1f - cos(theta)) * config.appDrawerCurvature
            val cx = (i % cols + .5f) * cellW
            drawIcon(canvas, app, cx, cy, iconSize, curvedScale, 0f, drawer, drawer > .98f)
        }
        paint.alpha = 255
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocity?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; touchX = event.x; touchY = event.y
                downAt = SystemClock.uptimeMillis(); dragging = true; lastY = event.y
                velocity?.recycle(); velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                spring.position = 0f; spring.velocity = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                touchX = event.x; touchY = event.y
                if (drawer > .5f) {
                    val maxScroll = max(0f, ((apps.size + 3) / 4) * 104f * density - height + 150f * density)
                    drawerScroll = (drawerScroll - (event.y - lastY)).coerceIn(0f, maxScroll)
                } else spring.position = dx / width
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishGesture(event)
        }
        return true
    }

    private fun finishGesture(event: MotionEvent) {
        velocity?.computeCurrentVelocity(1000)
        val vx = velocity?.xVelocity ?: 0f
        val dx = event.x - downX
        val dy = event.y - downY
        val wasTap = abs(dx) < 18f * density && abs(dy) < 18f * density
        dragging = false
        if (drawer > .5f) {
            if (dy > 90f * density) drawerTarget = 0f
            else if (wasTap) launchAt(event.x, event.y)
        } else if (wasTap) {
            if (SystemClock.uptimeMillis() - downAt > 550) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onOpenTuning?.invoke()
            } else if (drawerButton.contains(event.x, event.y)) drawerTarget = 1f
            else launchAt(event.x, event.y)
            spring.target = 0f
        } else if (dy < -100f * density && abs(dy) > abs(dx)) {
            drawerTarget = 1f; spring.target = 0f
        } else {
            val commit = abs(vx) > 1200f || abs(spring.position) > .35f
            if (commit && (spring.position < 0 || vx < -1200) && page < pageCount - 1) page++
            else if (commit && (spring.position > 0 || vx > 1200) && page > 0) page--
            spring.position = if (commit) (if (dx < 0) -1f else 1f) else spring.position
            spring.target = 0f
            spring.velocity = vx / width
        }
        velocity?.recycle(); velocity = null
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        postInvalidateOnAnimation()
    }

    private fun launchAt(x: Float, y: Float) {
        iconRects.lastOrNull { it.first.contains(x, y) }?.second?.let { app ->
            runCatching {
                context.startActivity(Intent.makeMainActivity(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun closeDrawer(): Boolean {
        if (drawerTarget > 0f) { drawerTarget = 0f; postInvalidateOnAnimation(); return true }
        return false
    }
}
