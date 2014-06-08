package com.example.complementaryfilter.app;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

public class CompassView extends View{

    private Paint fusedPaint;
    private Paint rawPaint;

    private float fusedX;
    private float fusedY;
    private float fusedAngle;

    private float rawX;
    private float rawY;
    private float rawAngle;

    public CompassView(Context context) {
        super(context);
        fusedPaint = new Paint();
        rawPaint = new Paint();

        fusedPaint.setColor(Color.GREEN);
        rawPaint.setColor(Color.BLUE);

        fusedX = 50;
        fusedY = 50;
        fusedAngle = 0;

        rawX = 50;
        rawY = 50;
        rawAngle = 0;
    }

    public void setFusedAngle(float angle) {
        fusedAngle = -angle;
        this.invalidate();
    }

    public void setRawAngle(float angle) {
        rawAngle = -angle;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawLine(500 * (float) Math.cos(fusedAngle) + fusedX,
                500 * (float) Math.sin(fusedAngle) + fusedY,
                -500 * (float) Math.cos(fusedAngle) + fusedX,
                -500 * (float) Math.sin(fusedAngle) + fusedY,
                fusedPaint);
        canvas.drawLine( 500*(float)Math.cos(rawAngle) + rawX,
                500*(float)Math.sin(rawAngle) + rawY,
                -500*(float)Math.cos(rawAngle) + rawX,
                -500*(float)Math.sin(rawAngle) + rawY,
                rawPaint);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        fusedX = w/2;
        fusedY = h/2;
        rawX = w/2;
        rawY = h/2;
    }
}
