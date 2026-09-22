package io.antigravity.clipfloat.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import io.antigravity.clipfloat.ClipFloatApplication
import io.antigravity.clipfloat.data.ClipboardRepository
import io.antigravity.clipfloat.data.EngineConfig
import io.antigravity.clipfloat.data.PreferenceBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlin.math.abs

class FloatingHUDService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var rootOverlayView: FrameLayout
    private lateinit var hudContainer: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var prefBridge: PreferenceBridge
    private var touchSlop: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Guard: Verify permission before attempting WindowManager operations
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefBridge = PreferenceBridge(this)
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        startForeground(NOTIFICATION_ID, createNotification())
        initWindowHierarchy()
        observeState()
    }

    private fun initWindowHierarchy() {
        val (screenWidth, screenHeight) = getScreenDimensions()

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16f)
            y = screenHeight / 3
        }

        rootOverlayView = FrameLayout(this)
        hudContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            elevation = dpToPx(8f).toFloat()
        }

        rootOverlayView.addView(hudContainer)
        setupDragPhysics()

        try {
            windowManager.addView(rootOverlayView, layoutParams)
        } catch (ex: WindowManager.BadTokenException) {
            stopSelf()
        }
    }

    private fun observeState() {
        serviceScope.launch {
            combine(prefBridge.configStream, ClipboardRepository.slotsStream) { config, slots ->
                Pair(config, slots)
            }.collect { (config, slots) ->
                renderHUD(config, slots)
            }
        }
    }

    private fun renderHUD(config: EngineConfig, slots: List<io.antigravity.clipfloat.data.SlotItem>) {
        hudContainer.removeAllViews()

        hudContainer.background = DrawableFactory.createSurface(
            fillColor = config.theme.dominantColor.toInt(),
            strokeColor = config.theme.secondaryBorderColor.toInt(),
            cornerRadius = dpToPx(16f).toFloat()
        )
        layoutParams.alpha = config.hudOpacity

        val visibleCount = config.slotCount.coerceIn(2, 8)
        for (i in 0 until visibleCount) {
            val slotData = slots.getOrNull(i)
            val slotView = createSlotItem(
                index = i,
                text = slotData?.text.orEmpty(),
                config = config
            )
            hudContainer.addView(slotView)
        }

        if (rootOverlayView.isAttachedToWindow) {
            windowManager.updateViewLayout(rootOverlayView, layoutParams)
        }
    }

    private fun createSlotItem(index: Int, text: String, config: EngineConfig): View {
        val sizePx = dpToPx(48f)
        val marginPx = dpToPx(4f)

        val slotFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            background = DrawableFactory.createSlotCard(
                fillColor = config.theme.secondaryColor.toInt(),
                strokeColor = config.theme.secondaryBorderColor.toInt(),
                cornerRadius = dpToPx(8f).toFloat()
            )
            isClickable = true
            isFocusable = true
        }

        val label = TextView(this).apply {
            this.text = "S${index + 1}"
            textSize = 9f
            setTextColor(config.theme.accentColor.toInt())
            setPadding(dpToPx(4f), dpToPx(2f), 0, 0)
        }

        val preview = TextView(this).apply {
            this.text = if (text.isEmpty()) "—" else if (text.length > 4) "${text.take(4)}…" else text
            textSize = 10f
            setTextColor(config.theme.textColor.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        slotFrame.addView(label)
        slotFrame.addView(preview)

        slotFrame.setOnClickListener {
            val payload = ClipboardRepository.getSlotPayload(index)
            if (payload.isNotEmpty()) {
                val service = ClipFloatAccessibilityService.get()
                service?.injectText(payload)
            }
        }

        return slotFrame
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragPhysics() {
        rootOverlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initX = 0
            private var initY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = layoutParams.x
                        initY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()

                        if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            layoutParams.x = initX + dx
                            layoutParams.y = initY + dy
                            windowManager.updateViewLayout(rootOverlayView, layoutParams)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            snapToNearestEdge()
                            return true
                        } else {
                            v?.performClick()
                        }
                    }
                }
                return false
            }
        })
    }

    private fun snapToNearestEdge() {
        val (screenWidth, _) = getScreenDimensions()
        val snapTarget = if (layoutParams.x + (rootOverlayView.width / 2) < screenWidth / 2) {
            dpToPx(8f)
        } else {
            screenWidth - rootOverlayView.width - dpToPx(8f)
        }

        ValueAnimator.ofInt(layoutParams.x, snapTarget).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                layoutParams.x = it.animatedValue as Int
                if (rootOverlayView.isAttachedToWindow) {
                    windowManager.updateViewLayout(rootOverlayView, layoutParams)
                }
            }
            start()
        }
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            Pair(metrics.bounds.width(), metrics.bounds.height())
        } else {
            val display = windowManager.defaultDisplay
            val size = Point()
            @Suppress("DEPRECATION")
            display.getSize(size)
            Pair(size.x, size.y)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, ClipFloatApplication.CHANNEL_ID)
            .setContentTitle("ClipFloat Active")
            .setContentText("Multi-register clipboard HUD running")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::rootOverlayView.isInitialized && rootOverlayView.isAttachedToWindow) {
            windowManager.removeView(rootOverlayView)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 5050
    }
}
