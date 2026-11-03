package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.core.A2uiException;

/** 应用所有组件共用的声明式样式扩展。 */
final class ComponentStyleApplier {
    private ComponentStyleApplier() {
    }

    static void apply(A2uiRenderContext context, JsonObject component, View view) {
        JsonElement value = component.get("style");
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (!value.isJsonObject()) {
            throw new A2uiException("style 必须是 JSON 对象");
        }
        JsonObject style = value.getAsJsonObject();
        applyBackground(context, style.get("background"), view);
        applyPadding(context, style, view);
        if (style.has("elevation")) {
            view.setElevation(dp(context, style.get("elevation")));
        }
        if (style.has("alpha")) {
            float alpha = style.get("alpha").getAsFloat();
            if (alpha < 0f || alpha > 1f) {
                throw new A2uiException("style.alpha 必须在 0 到 1 之间");
            }
            view.setAlpha(alpha);
        }
    }

    private static void applyBackground(A2uiRenderContext context, JsonElement value, View view) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        GradientDrawable background = new GradientDrawable();
        if (value.isJsonPrimitive()) {
            background.setColor(Color.parseColor(value.getAsString()));
            view.setBackground(background);
            return;
        }
        if (!value.isJsonObject()) {
            throw new A2uiException("style.background 必须是颜色字符串或 JSON 对象");
        }
        JsonObject definition = value.getAsJsonObject();
        if (definition.has("gradientColors")) {
            JsonArray colors = definition.getAsJsonArray("gradientColors");
            if (colors == null || colors.size() < 2) {
                throw new A2uiException("gradientColors 至少需要两种颜色");
            }
            int[] parsed = new int[colors.size()];
            for (int i = 0; i < colors.size(); i++) {
                parsed[i] = Color.parseColor(colors.get(i).getAsString());
            }
            background.setColors(parsed);
            background.setOrientation(orientation(BasicCatalogRenderer.optString(
                    definition, "gradientOrientation", "TOP_BOTTOM")));
        } else {
            background.setColor(Color.parseColor(BasicCatalogRenderer.optString(
                    definition, "color", "#00000000")));
        }
        float radius = definition.has("cornerRadius")
                ? dp(context, definition.get("cornerRadius")) : 0f;
        if (radius > 0f) {
            background.setCornerRadius(radius);
        }
        int strokeWidth = definition.has("strokeWidth")
                ? Math.round(dp(context, definition.get("strokeWidth"))) : 0;
        if (strokeWidth > 0) {
            background.setStroke(strokeWidth, Color.parseColor(BasicCatalogRenderer.optString(
                    definition, "strokeColor", "#FF000000")));
        }
        view.setBackground(background);
        if (BasicCatalogRenderer.bool(context, definition.get("clipToOutline"), radius > 0f)) {
            view.setClipToOutline(true);
        }
    }

    private static void applyPadding(A2uiRenderContext context, JsonObject style, View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        if (style.has("padding")) {
            JsonElement padding = style.get("padding");
            if (padding.isJsonArray()) {
                JsonArray values = padding.getAsJsonArray();
                if (values.size() != 4) {
                    throw new A2uiException("style.padding 数组必须依次为 left、top、right、bottom");
                }
                left = Math.round(dp(context, values.get(0)));
                top = Math.round(dp(context, values.get(1)));
                right = Math.round(dp(context, values.get(2)));
                bottom = Math.round(dp(context, values.get(3)));
            } else {
                int all = Math.round(dp(context, padding));
                left = top = right = bottom = all;
            }
        }
        if (style.has("paddingHorizontal")) {
            left = right = Math.round(dp(context, style.get("paddingHorizontal")));
        }
        if (style.has("paddingVertical")) {
            top = bottom = Math.round(dp(context, style.get("paddingVertical")));
        }
        view.setPadding(left, top, right, bottom);
    }

    private static float dp(A2uiRenderContext context, JsonElement value) {
        return value.getAsFloat() * context.getContext().getResources().getDisplayMetrics().density;
    }

    private static GradientDrawable.Orientation orientation(String value) {
        switch (value) {
            case "BOTTOM_TOP":
                return GradientDrawable.Orientation.BOTTOM_TOP;
            case "LEFT_RIGHT":
                return GradientDrawable.Orientation.LEFT_RIGHT;
            case "RIGHT_LEFT":
                return GradientDrawable.Orientation.RIGHT_LEFT;
            case "TL_BR":
                return GradientDrawable.Orientation.TL_BR;
            case "BR_TL":
                return GradientDrawable.Orientation.BR_TL;
            case "TR_BL":
                return GradientDrawable.Orientation.TR_BL;
            case "BL_TR":
                return GradientDrawable.Orientation.BL_TR;
            case "TOP_BOTTOM":
                return GradientDrawable.Orientation.TOP_BOTTOM;
            default:
                throw new A2uiException("不支持的 gradientOrientation: " + value);
        }
    }
}
