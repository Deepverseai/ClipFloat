package io.antigravity.clipfloat.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

class ClipFloatAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private var serviceRef: WeakReference<ClipFloatAccessibilityService>? = null

        val isServiceActive: Boolean
            get() = serviceRef?.get() != null

        fun get(): ClipFloatAccessibilityService? = serviceRef?.get()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event stream intentionally passive to avoid unnecessary UI thread overhead
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceRef?.clear()
        serviceRef = null
    }

    /**
     * Non-destructive text injection pipeline:
     * 1. Direct ACTION_PASTE check
     * 2. Non-destructive AST Text Insertion (preserves text at cursor)
     * 3. Fallback via ClipboardManager with safe Handler restoration
     */
    fun injectText(payload: String): Boolean {
        if (payload.isEmpty()) return false

        val targetNode = findFocusedEditableNode() ?: return false
        var success = false

        try {
            // Strategy 1: Direct node paste action if supported
            if (targetNode.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }) {
                success = performClipboardAssistedPaste(targetNode, payload)
                if (success) return true
            }

            // Strategy 2: Non-destructive AST cursor-aware insertion
            if (targetNode.isEditable) {
                val existingText = targetNode.text?.toString() ?: ""
                val selStart = targetNode.textSelectionStart.coerceAtLeast(0)
                val selEnd = targetNode.textSelectionEnd.coerceAtLeast(selStart)

                val newText = if (selStart <= existingText.length && selEnd <= existingText.length) {
                    existingText.substring(0, selStart) + payload + existingText.substring(selEnd)
                } else {
                    existingText + payload
                }

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                }
                success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }

            // Strategy 3: Secondary Fallback
            if (!success) {
                success = performClipboardAssistedPaste(targetNode, payload)
            }
        } finally {
            // Deprecated and no-op on API 34+; recycle safely on older platforms
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                targetNode.recycle()
            }
        }

        return success
    }

    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val activeWindows = windows
        if (activeWindows.isNotEmpty()) {
            for (window in activeWindows) {
                val root = window.root ?: continue
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                if (focused != null && focused.isEditable) {
                    return focused
                }
            }
        }
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
    }

    private fun performClipboardAssistedPaste(target: AccessibilityNodeInfo, payload: String): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = runCatching { clipboard.primaryClip }.getOrNull()

        val tempClip = ClipData.newPlainText("ClipFloat_Payload", payload)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tempClip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }

        clipboard.setPrimaryClip(tempClip)
        val pasteSuccess = target.performAction(AccessibilityNodeInfo.ACTION_PASTE)

        // Revert clipboard safely via MainLooper Handler
        mainHandler.postDelayed({
            runCatching {
                if (previousClip != null) {
                    clipboard.setPrimaryClip(previousClip)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    }
                }
            }
        }, 300)

        return pasteSuccess
    }
}
