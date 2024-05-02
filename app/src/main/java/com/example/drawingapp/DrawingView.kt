package com.example.drawingapp


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context: Context, attrs : AttributeSet): View(context, attrs){
    private var mDrawPath : CustomPath? = null  // custom path inner class to use
    private var mCanvasBitmap : Bitmap? = null  // an instance of bitmap
    private var mDrawPaint : Paint? = null  // paint class holds the style and color info about
    // how to draw geometrics ,text and bitmap.
    private var mCanvasPaint : Paint? = null // an instance of canvas paint view
    private var mBrushSize : Float = 0.toFloat() // variable for stroke/brush size to draw on the canvas
    private var color = Color.BLACK // variable to hold a color of the strokes
    private var canvas : Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    init{
        setupDrawing()
    }

    // func of undo
    fun onClickUndo(){
        if(mPaths.size>0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }

    // func of redo
    fun onClickRedo(){
        if(mUndoPaths.size>0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }

    // func of drawing on canvas
    private fun setupDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE  // this is to draw a stroke style
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND // this is for store join
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND // this is for stroke cap
        mCanvasPaint = Paint(Paint.DITHER_FLAG) // paint flag that enables dithering when blitting
    }

    // func to change stroke size
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint,
    // transformed by the current matrix.
    // If the bitmap and canvas have different densities, this function will take care of,
    // automatically scaling the bitmap to draw at the same density as the canvas.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

   // func acts as an event listener when a touch event is detected on the device
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x  // touch event of x coordinate
        val touchY = event?.y  // touch event of y coordinate
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset() // clear any lines and curves from the path, making it empty
                if(touchX!=null){
                    if(touchY!=null){
                        // set the beginning of the next corner to the point (x,y)
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(touchX!=null){
                    if(touchY!=null){
                        // add a line from the last point to the specified point (x,y)
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
               mPaths.add(mDrawPath!!)
               mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    // func for either the brush or the eraser
    fun setSizeForBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
            )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    // func for changing user desires color
    fun setColor(newColor: String){
        color = Color.parseColor(newColor.toString())
        mDrawPaint!!.color = color
    }

   internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }
}