package org.a2ui.android.v0_9_1;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class LayoutRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        int orientation = "Row".equals(RenderSupport.optString(component, "component", ""))
                ? LinearLayout.HORIZONTAL
                : LinearLayout.VERTICAL;
        return render(context, component, orientation);
    }

    static View render(A2uiRenderContext context, JsonObject component, int orientation) {
        LinearLayout layout = new LinearLayout(context.getContext());
        layout.setOrientation(orientation);
        layout.setGravity("center".equals(RenderSupport.optString(component, "align", ""))
                ? (orientation == LinearLayout.VERTICAL ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL)
                : Gravity.TOP);
        List<View> children = children(context, component.get("children"));
        String justify = RenderSupport.optString(component, "justify", "start");
        String align = RenderSupport.optString(component, "align", "stretch");
        boolean horizontal = orientation == LinearLayout.HORIZONTAL;
        boolean stretchMain = "stretch".equals(justify) && !children.isEmpty();
        int mainGravity = mainGravity(justify, horizontal);
        int crossGravity = crossGravity(align, horizontal);
        layout.setGravity(mainGravity | crossGravity);

        if ("spaceAround".equals(justify) || "spaceEvenly".equals(justify)) {
            addSpacer(context, layout, horizontal);
        }
        for (int i = 0; i < children.size(); i++) {
            View child = children.get(i);
            ViewGroup.LayoutParams original = child.getLayoutParams();
            int childWidth = original == null ? ViewGroup.LayoutParams.WRAP_CONTENT : original.width;
            int childHeight = original == null ? ViewGroup.LayoutParams.WRAP_CONTENT : original.height;
            int width = horizontal ? childWidth : ViewGroup.LayoutParams.MATCH_PARENT;
            int height = childHeight;
            if (stretchMain) {
                if (horizontal) {
                    width = 0;
                } else {
                    height = 0;
                }
            }
            if ("stretch".equals(align)) {
                if (horizontal && child.getLayoutParams() != null
                        && child.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if (!horizontal) {
                    width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            if (stretchMain) {
                params.weight = 1f;
            }
            int margin = RenderSupport.dp(context, 2);
            params.setMargins(margin, margin, margin, margin);
            layout.addView(child, params);
            boolean between = "spaceBetween".equals(justify) && i < children.size() - 1;
            boolean around = "spaceAround".equals(justify) || "spaceEvenly".equals(justify);
            if (between || around) {
                addSpacer(context, layout, horizontal);
            }
        }
        return layout;
    }

    private static List<View> children(A2uiRenderContext context, JsonElement definition) {
        List<View> views = new ArrayList<>();
        if (definition == null || definition.isJsonNull()) {
            return views;
        }
        if (definition.isJsonArray()) {
            for (JsonElement id : definition.getAsJsonArray()) {
                views.add(context.renderChild(id.getAsString()));
            }
            return views;
        }
        JsonObject template = definition.getAsJsonObject();
        String path = template.get("path").getAsString();
        String fullPath = RenderSupport.absolutePath(path, context.getScope());
        JsonElement data = context.getSurface().getValue(path, context.getScope());
        if (data == null || !data.isJsonArray()) {
            return views;
        }
        JsonArray array = data.getAsJsonArray();
        String componentId = RenderSupport.requiredId(template, "componentId");
        for (int i = 0; i < array.size(); i++) {
            A2uiRenderContext scoped = context.withScope(fullPath + "/" + i);
            views.add(scoped.renderChild(componentId));
        }
        return views;
    }

    private static int mainGravity(String justify, boolean horizontal) {
        if ("center".equals(justify)) {
            return horizontal ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL;
        }
        if ("end".equals(justify)) {
            return horizontal ? Gravity.END : Gravity.BOTTOM;
        }
        return horizontal ? Gravity.START : Gravity.TOP;
    }

    private static int crossGravity(String align, boolean horizontal) {
        if ("center".equals(align)) {
            return horizontal ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL;
        }
        if ("end".equals(align)) {
            return horizontal ? Gravity.BOTTOM : Gravity.END;
        }
        return horizontal ? Gravity.TOP : Gravity.START;
    }

    private static void addSpacer(A2uiRenderContext context, LinearLayout layout, boolean horizontal) {
        View spacer = new View(context.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                horizontal ? 0 : 1, horizontal ? 1 : 0, 1f);
        layout.addView(spacer, params);
    }
}
