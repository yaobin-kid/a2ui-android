package org.a2ui.android.v0_9_1;

import android.view.View;

import com.google.gson.JsonObject;

/** 自定义 Catalog 组件渲染器。 */
public interface A2uiComponentRenderer {
    View render(A2uiRenderContext context, JsonObject component);
}
