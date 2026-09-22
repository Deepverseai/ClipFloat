package io.antigravity.clipfloat.service

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

object DrawableFactory {
    fun createSurface(fillColor: Int, strokeColor: Int, cornerRadius: Float): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(2, strokeColor)
            this.cornerRadius = cornerRadius
        }
    }

    fun createSlotCard(fillColor: Int, strokeColor: Int, cornerRadius: Float): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(1, strokeColor)
            this.cornerRadius = cornerRadius
        }
    }
}
