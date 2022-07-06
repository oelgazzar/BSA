package com.example.bsa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.pow

@SuppressLint("ResourceType")
class Body(ctx: Context, attrs: AttributeSet)
    : AppCompatImageView(ctx, attrs), View.OnTouchListener {

    companion object {
        const val SELECTION = "selection"
        const val CALCULATED_SURFACE_AREA = "calculatedSurfaceArea"
        const val scaleFactor = 1.6
    }

    private val bodyBitmap = BitmapFactory.decodeResource(resources, R.drawable.body)

    private val originalWidth = bodyBitmap.width
    private val originalHeight = bodyBitmap.height
    private val scaledWidth = (originalWidth * scaleFactor).toInt()
    private val scaledHeight = (originalHeight * scaleFactor).toInt()
    private var offsetX: Float = 0f
    private var touchX: Float = 0f
    private var touchY: Float = 0f
    private var lastSelected: Part? = null
    private var overlappedParts = mutableListOf<Part>()

    private val surfaceBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
    private val mCanvas = Canvas(surfaceBitmap)
    private val paint = Paint()


    private val parts = mutableListOf<Part>()

    var calculatedSurfaceArea: Float = 0f
        get() {
            field = calculateSelectedSurfaceArea()
            Log.d("field", field.toString())
            return field
        }

    var age: Int = 20
        set(age: Int) {
            if (age < 0) throw AgeOutOfBounds("Age should not be negative")
            if (ageIn == AgeIn.MONTHS && age > 12) throw AgeOutOfBounds("Age should be 0 - 12 months or use years")
            field = age
        }
    var ageIn = AgeIn.YEARS
        set(ageIn: AgeIn) {
            if (ageIn == AgeIn.MONTHS && age > 12) throw AgeOutOfBounds("Age should be 0 - 12 months or use years")
            field = ageIn
        }

    private var onSelectionListener: OnSelectionListener? = null

    init {
        // setUp image view
        minimumHeight = scaledHeight
        minimumWidth = scaledWidth
        setOnTouchListener(this)
        Log.d("width", "$originalWidth, $scaledWidth, $width, $offsetX")

        // create array of body parts of array resources
        val partArray = resources.obtainTypedArray(R.array.all_parts)
        for (i in 0 until partArray.length()) {
            val partData = resources.obtainTypedArray(partArray.getResourceId(i, -1))
            var (left, top, width, height) = resources.getIntArray(partData.getResourceId(2, -1))
            val part = Part(
                (2.0).pow(i).toLong(),
                partData.getString(0)!!,
                partData.getResourceId(1, -1),
                left.toFloat(),
                top.toFloat(),
                width,
                height,
                resources.getTextArray(partData.getResourceId(3, -1)).map { it.toString().toFloat() })
            parts.add(part)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        /*
         * Draw body and parts (centered)
         */
        super.onDraw(canvas)
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC)
        mCanvas.drawBitmap(bodyBitmap, 0f, 0f, paint)
        for (part in parts) {
            if (part.isSelected) {
                part.draw()
            }
            if (part in overlappedParts) {
                part.drawCenter()
                mCanvas.drawLine(touchX, touchY, part.centerX(), part.centerY(), paint)
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
                mCanvas.drawCircle(touchX, touchY, 2f, paint)
            }
        }
        if (lastSelected != null) {
            paint.style = Paint.Style.STROKE
            mCanvas.drawRect(lastSelected!!, paint)
        }
        offsetX = (width - scaledWidth) / 2f
        Log.d("width", "$originalWidth, $scaledWidth, $width, $offsetX")
        canvas!!.drawBitmap(Bitmap.createScaledBitmap(surfaceBitmap,
            scaledWidth, scaledHeight, true), offsetX, 0f, paint)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        /*
         * Select corresponding body parts on touch
         */
        val x = (event.x - offsetX) / scaleFactor
        touchX = x.toFloat()
        val y = event.y / scaleFactor
        touchY = y.toFloat()

        if (event.action == MotionEvent.ACTION_UP) {
            overlappedParts = mutableListOf<Part>()
            for (part in parts) {
                if (part.contains(x.toFloat(), y.toFloat())) {
                    overlappedParts.add(part)
                }
            }

            val closestPart: Part? = overlappedParts.minWithOrNull { p1, p2 ->
                Log.d("dist", p1.name + p1.distanceOfCenterToPoint(x, y).toString())
                Log.d("dist", p2.name + p2.distanceOfCenterToPoint(x, y).toString())
                (p1.distanceOfCenterToPoint(x, y) - p2.distanceOfCenterToPoint(x, y)).toInt()
            }
            if (closestPart != null) {
                Log.d("dist-closest", closestPart.name + " is closer!")
                closestPart.isSelected = !closestPart.isSelected
                lastSelected = closestPart
                invalidate()
                onSelectionListener?.onSelection(calculateSelectedSurfaceArea())
            }
        }
        return true
    }

    private fun calculateSelectedSurfaceArea(): Float {
        var selectedSurfaceArea: Float = 0f
        for (part in parts) {
            if (part.isSelected) selectedSurfaceArea += part.getSurfaceArea()
        }
        return selectedSurfaceArea
    }

    fun saveState(state: Bundle) {
        var selection: Long = 0
        parts.forEach {
            if (it.isSelected) selection = selection or it.id
        }
        state.putLong(SELECTION, selection)
        state.putFloat(CALCULATED_SURFACE_AREA, calculatedSurfaceArea)
    }

    fun loadState(state: Bundle?) {
        if (state == null) return

        val selection = state.getLong(SELECTION)
        parts.forEach {
            if (selection and it.id != 0L) it.isSelected = true
        }

        calculatedSurfaceArea = state.getFloat(CALCULATED_SURFACE_AREA)
    }

    inner class Part(
        /*
         * This class create body part for selections
         */
        val id: Long,
        val name: String,
        private val drawableResourceId: Int,
        x: Float,
        y: Float,
        w: Int,
        h: Int,
        private val surfaceAreas: List<Float>): RectF(x, y, x + w, y + h) {

        var isSelected = false
        var partBitmap: Bitmap = BitmapFactory.decodeResource(resources, drawableResourceId)

        fun draw() {
            mCanvas.drawBitmap(partBitmap, left, top, paint)
            paint.textSize = 14f
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            mCanvas.drawText(getSurfaceArea().toString() + " %", centerX(), centerY(), paint)
        }

        fun drawCenter() {
            paint.color = Color.rgb((0..255).random(),(0..255).random(),(0..255).random())
            mCanvas.drawCircle(centerX(), centerY(), 5f, paint)
        }

        fun getSurfaceArea(): Float {
            return when {
                ageIn == AgeIn.MONTHS || age <= 1 -> surfaceAreas[0]
                age <= 4 -> surfaceAreas[1]
                age <= 9 -> surfaceAreas[2]
                age <= 14 -> surfaceAreas[3]
                age == 15 -> surfaceAreas[4]
                else -> surfaceAreas[5]
            }
        }

        fun distanceOfCenterToPoint(x: Double, y: Double): Double {
            val deltaY = (y - centerY()).toDouble()
            val deltaX = (x - centerX()).toDouble()
            Log.d("dist", "touchX: $x, centerX: ${centerX()}, y: $y, centerY: ${centerY()}")
            Log.d("dist", "deltaX: $deltaX, deltaY: $deltaY")
            Log.d("dist", "dX^2: ${Math.pow(deltaX, 2.0)}, dY^2: ${Math.pow(deltaY, 2.0)}")
            var distance = Math.sqrt(Math.pow(deltaX, 2.0) + Math.pow(deltaY, 2.0))
            Log.d("dist", "distance: $distance")
            return distance
        }
    }

    fun interface OnSelectionListener {
        fun onSelection(surfaceArea: Float)
    }

    fun setOnSelectionListener(onSelectionListener: OnSelectionListener) {
        this.onSelectionListener = onSelectionListener
    }

    enum class AgeIn {
        YEARS, MONTHS
    }

    class AgeOutOfBounds(message: String): Throwable(message)

}