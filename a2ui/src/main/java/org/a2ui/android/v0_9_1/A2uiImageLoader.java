package org.a2ui.android.v0_9_1;

import android.widget.ImageView;

/** 图片加载扩展点，可接入 Glide、Coil 等图片库。 */
public interface A2uiImageLoader {
    void load(String url, ImageView target);
}
