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
    private int initialColor = 0xFF660000;
    //canvas
    private Canvas canvas;
    //canvas bitmap
    private Bitmap bitmap;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        path = new Path();
        drawPaint = new Paint();

        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setColor(initialColor);
        drawPaint.setStrokeWidth(9);

        // bit mask for the flag enabling dithering
        canvasPaint = new Paint(Paint.DITHER_FLAG);
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
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(touchX, touchY);
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

    public void setColor(String newColor) {
        //set color
        invalidate();

        initialColor = Color.parseColor(newColor);
        drawPaint.setColor(initialColor);
    }

    public void startNew() {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    public void eraseImage(boolean erase) {
        if (erase) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            Log.i("color", "erase model");
            drawPaint.setStrokeWidth(45);
        } else {
            drawPaint.setXfermode(null);
            Log.i("color", "change to non-erase model");
            drawPaint.setStrokeWidth(10);
            drawPaint.setColor(initialColor);
        }
    }

    public int getInitialColor() {
        return initialColor;
    }
}
