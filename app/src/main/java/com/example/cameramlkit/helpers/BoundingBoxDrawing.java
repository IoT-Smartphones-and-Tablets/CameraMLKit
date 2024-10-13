package com.example.cameramlkit.helpers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BoundingBoxDrawing extends Drawable {

    private final Paint boundingRectPaint = new Paint();
    private final Rect rect;
    private final int colorRect;
    private final String text;
    private final int colorText;

    public BoundingBoxDrawing(Rect rect, int colorRect, String text, int colorText) {
        this.rect = rect;
        this.colorRect = colorRect;
        this.text = text;
        this.colorText = colorText;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

        boundingRectPaint.setStyle(Paint.Style.STROKE);
        boundingRectPaint.setColor(colorRect);
        boundingRectPaint.setStrokeWidth(5F);
        boundingRectPaint.setAlpha(200);

        canvas.drawRect(rect, boundingRectPaint);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        canvas.drawText(text, rect.left, rect.bottom + 30, paint);

    }

    @Override
    public void setAlpha(int i) {
        boundingRectPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        boundingRectPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
