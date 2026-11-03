package org.a2ui.android.v0_9_1;

import android.view.View;

import com.google.gson.JsonObject;

/** 组件原生样式设置器，可修改背景、文字、尺寸、间距及具体原生控件的属性。 */
public interface A2uiComponentStyler {
    /**
     * @param view 组件最外层的原生 View
     * @param component 当前组件的原始 JSON 定义
     * @param context 当前 Surface 与数据作用域
     */
    void apply(View view, JsonObject component, A2uiRenderContext context);
}
