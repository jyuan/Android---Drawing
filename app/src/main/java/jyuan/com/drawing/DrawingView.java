package jyuan.com.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
    //drawing path
    private Path path;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int currentColor = 0xFF660000;
    //canvas
    private Canvas canvas;
    //canvas bitmap
    private Bitmap bitmap;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        path = new Path();
        drawPaint = new Paint();
        canvasPaint = new Paint();

        init();
    }

    private void init() {
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setColor(currentColor);
        drawPaint.setStrokeWidth(9);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //view given size
        super.onSizeChanged(w, h, oldw, oldh);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, canvasPaint);
        canvas.drawPath(path, drawPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                canvas.drawPath(path, drawPaint);
                path.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    /**
     * change current color to the new selected color
     * @param newColor selected color
     */
    public void setCurrentColor(int newColor) {
        currentColor = newColor;
        drawPaint.setColor(currentColor);
        invalidate();
    }

    /**
     * get current color
     * @return current color
     */
    public int getCurrentColor() {
        return currentColor;
    }

    /**
     * start a new canvas
     */
    public void createNewDrawing() {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    /**
     * erase part of image
     * @param erase true if it in the erase module
     */
    public void eraseImage(boolean erase) {
        if (erase) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            Log.i(getClass().getSimpleName(), "erase model");
            drawPaint.setStrokeWidth(45);
        } else {
            drawPaint.setXfermode(null);
            Log.i(getClass().getSimpleName(), "change to non-erase model");
            drawPaint.setStrokeWidth(10);
            drawPaint.setColor(currentColor);
        }
    }
}
