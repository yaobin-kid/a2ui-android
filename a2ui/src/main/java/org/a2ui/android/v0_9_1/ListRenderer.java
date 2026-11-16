package org.a2ui.android.v0_9_1;

import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.gson.JsonObject;

final class ListRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        boolean horizontal = "horizontal".equals(RenderSupport.optString(component, "direction", "vertical"));
        JsonObject copy = component.deepCopy();
        copy.addProperty("component", horizontal ? "Row" : "Column");
        View content = LayoutRenderer.render(context, copy,
                horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        if (horizontal) {
            HorizontalScrollView scroll = new HorizontalScrollView(context.getContext());
            scroll.setFillViewport(true);
            scroll.addView(content, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return scroll;
        }
        ScrollView scroll = new ScrollView(context.getContext());
        scroll.setFillViewport(true);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }
}
