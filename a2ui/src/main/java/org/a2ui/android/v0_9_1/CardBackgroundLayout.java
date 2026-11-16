package org.a2ui.android.v0_9_1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Card 专用容器：背景图由容器自己在 onDraw 中绘制。
 *
 * <p>背景图不参与卡片尺寸计算，绘制时直接使用卡片自身的画布尺寸，
 * 因此一定铺满整个卡片（含 padding 区域），不会出现边距；
 * 容器设置了 clipToOutline，绘制内容（含背景图）按卡片圆角裁剪。
 * 内容子 View 在 onDraw 之后绘制，自然位于背景图之上。
 */
final class CardBackgroundLayout extends FrameLayout {
    private final Rect dstRect = new Rect();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private ImageView backgroundImage;

    CardBackgroundLayout(Context context) {
        super(context);
    }

    void setBackgroundImage(ImageView image) {
        this.backgroundImage = image;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackgroundImage(canvas);
    }

    private void drawBackgroundImage(Canvas canvas) {
        if (backgroundImage == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Bitmap bitmap = bitmapOf(backgroundImage);
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        ImageView.ScaleType scaleType = backgroundImage.getScaleType();
        if (scaleType == ImageView.ScaleType.FIT_XY) {
            dstRect.set(0, 0, width, height);
        } else {
            dstRectFor(scaleType, bitmap.getWidth(), bitmap.getHeight(), width, height, dstRect);
        }
        canvas.drawBitmap(bitmap, null, dstRect, bitmapPaint);
    }

    private static Bitmap bitmapOf(ImageView imageView) {
        android.graphics.drawable.Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null;
    }

    /** 与 ImageView 的 scaleType 语义保持一致（fill 之外的等比绘制方式）。 */
    private static void dstRectFor(ImageView.ScaleType scaleType, int sourceWidth,
                                   int sourceHeight, int width, int height, Rect out) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            out.set(0, 0, width, height);
            return;
        }
        float scale;
        switch (scaleType) {
            case CENTER_CROP:
                scale = Math.max((float) width / sourceWidth, (float) height / sourceHeight);
                break;
            case FIT_CENTER:
                scale = Math.min((float) width / sourceWidth, (float) height / sourceHeight);
                break;
            default:
                scale = 1f;
                break;
        }
        int scaledWidth = Math.round(sourceWidth * scale);
        int scaledHeight = Math.round(sourceHeight * scale);
        int left = (width - scaledWidth) / 2;
        int top = (height - scaledHeight) / 2;
        out.set(left, top, left + scaledWidth, top + scaledHeight);
    }
}
