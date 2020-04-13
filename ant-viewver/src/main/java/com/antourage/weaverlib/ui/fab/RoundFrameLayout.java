package com.antourage.weaverlib.ui.fab;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

public class RoundFrameLayout extends FrameLayout {

    private final Path clip = new Path();

    private int posX;
    private int posY;
    private int radius;

    public RoundFrameLayout(Context context) {
        this(context, null);

    }

    public RoundFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // We can use outlines on 21 and up for anti-aliased clipping.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        posX = Math.round((float) width / 2);
        posY = Math.round((float) height / 2);

        // noinspection NumericCastThatLosesPrecision
        radius = (int) Math.floor((float) Math.min(width, height) / 2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new OutlineProvider(posX, posY, radius));
        } else {
            clip.reset();
            clip.addCircle(posX, posY, radius, Path.Direction.CW);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Not needed on 21 and up since we're clipping to the outline instead.
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Don't pass touch events that occur outside of our clip to the children.
        float distanceX = Math.abs(event.getX() - posX);
        float distanceY = Math.abs(event.getY() - posY);
        double distance = Math.hypot(distanceX, distanceY);

        return distance > radius;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class OutlineProvider extends ViewOutlineProvider {

        final int left;
        final int top;
        final int right;
        final int bottom;

        OutlineProvider(int posX, int posY, int radius) {
            left = posX - radius;
            top = posY - radius;
            right = posX + radius;
            bottom = posY + radius;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(left, top, right, bottom);
        }

    }
}
