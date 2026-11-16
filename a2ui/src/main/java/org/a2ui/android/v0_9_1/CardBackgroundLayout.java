package org.a2ui.android.v0_9_1;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Card 专用容器：背景图不参与卡片尺寸计算，
 * 卡片测量完成后背景图被重新测量为卡片大小，从而铺满整个卡片。
 */
final class CardBackgroundLayout extends FrameLayout {
    private View backgroundView;

    CardBackgroundLayout(Context context) {
        super(context);
    }

    void setBackgroundView(View backgroundView) {
        this.backgroundView = backgroundView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (backgroundView == null) {
            return;
        }
        backgroundView.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }
}
