package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonObject;

final class DividerRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        View view = new View(context.getContext());
        view.setBackgroundColor(Color.rgb(220, 220, 220));
        boolean horizontal = !"vertical".equals(RenderSupport.optString(component, "axis", "horizontal"));
        view.setLayoutParams(new ViewGroup.LayoutParams(
                horizontal ? ViewGroup.LayoutParams.MATCH_PARENT : RenderSupport.dp(context, 1),
                horizontal ? RenderSupport.dp(context, 1) : ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }
}
