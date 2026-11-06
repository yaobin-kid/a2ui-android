package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 客户端 style 扩展：解析组件的 {@code style} 声明并应用到原生 View。
 *
 * <p><strong>注意：</strong>{@code style} 字段不是 A2UI v0.9.1 协议定义的属性。
 * 协议的 {@code catalogs/basic/catalog.json} 中每个组件都声明
 * {@code "unevaluatedProperties": false}，意味着 schema 外的字段应被静默忽略。
 * 本库在此基础上增加了客户端的 style 解析，解析失败会静默忽略，
 * 不会破坏协议兼容性。
 *
 * <p>完整 style 规格见 README 第 5 节。
 */
final class ComponentStyleApplier {
    private ComponentStyleApplier() {
    }

    static void apply(A2uiRenderContext context, JsonObject component, View view) {
        JsonElement value = component.get("style");
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (!value.isJsonObject()) {
            return; // 协议兼容：未知/非法字段静默忽略
        }
        JsonObject style = value.getAsJsonObject();
        try {
            applyBackground(context, style, view);
            applyPadding(context, style, view);
            if (style.has("elevation")) {
                view.setElevation(dp(context, style.get("elevation")));
            }
            if (style.has("alpha")) {
                float alpha = style.get("alpha").getAsFloat();
                if (alpha >= 0f && alpha <= 1f) {
                    view.setAlpha(alpha);
                }
            }
            applyTextAttributes(context, style, view);
        } catch (Exception ignored) {
            // style 解析失败静默忽略，不影响协议合规
        }
    }

    private static void applyBackground(A2uiRenderContext context, JsonObject style, View view) {
        // 1. 先处理 stateStyles（按下/选中/禁用态）
        if (style.has("stateStyles") && style.get("stateStyles").isJsonObject()) {
            JsonObject stateStyles = style.getAsJsonObject("stateStyles");
            StateListDrawable states = new StateListDrawable();
            // 正向 state 优先添加（Android StateListDrawable 是 first-match）
            // 顺序：disabled → pressed → checked → default
            if (stateStyles.has("disabled")) {
                states.addState(new int[]{-android.R.attr.state_enabled},
                        buildBackground(context, stateStyles.get("disabled")));
            }
            if (stateStyles.has("pressed")) {
                states.addState(new int[]{android.R.attr.state_pressed},
                        buildBackground(context, stateStyles.get("pressed")));
            }
            if (stateStyles.has("checked")) {
                states.addState(new int[]{android.R.attr.state_checked},
                        buildBackground(context, stateStyles.get("checked")));
            }
            if (stateStyles.has("focused")) {
                states.addState(new int[]{android.R.attr.state_focused},
                        buildBackground(context, stateStyles.get("focused")));
            }
            // default 放最后（空 state 匹配一切）
            GradientDrawable def = buildBackground(context, style.get("background"));
            states.addState(new int[]{}, def);
            view.setBackground(states);
            return;
        }
        // 2. 没有 stateStyles → 单一背景
        GradientDrawable single = buildBackground(context, style.get("background"));
        if (single != null) {
            view.setBackground(single);
            // clipToOutline：显式 true 或 cornerRadius > 0 时默认 true
            boolean hasRadius = style.has("background")
                    && style.get("background").isJsonObject()
                    && style.getAsJsonObject("background").has("cornerRadius");
            boolean clip = BasicCatalogRenderer.bool(context,
                    style.has("clipToOutline") ? style.get("clipToOutline") : null,
                    hasRadius);
            if (clip) view.setClipToOutline(true);
        }
    }

    /** 构建一个 GradientDrawable（纯色 / 渐变 + 圆角 + 描边）。参数可以是字符串（纯色）或对象。 */
    private static GradientDrawable buildBackground(A2uiRenderContext context, JsonElement value) {
        GradientDrawable drawable = new GradientDrawable();
        if (value == null || value.isJsonNull()) {
            drawable.setColor(Color.TRANSPARENT);
            return drawable;
        }
        if (value.isJsonPrimitive()) {
            drawable.setColor(Color.parseColor(value.getAsString()));
            return drawable;
        }
        if (!value.isJsonObject()) {
            drawable.setColor(Color.TRANSPARENT);
            return drawable;
        }
        JsonObject definition = value.getAsJsonObject();
        if (definition.has("gradientColors")) {
            JsonArray colors = definition.getAsJsonArray("gradientColors");
            if (colors != null && colors.size() >= 2) {
                int[] parsed = new int[colors.size()];
                for (int i = 0; i < colors.size(); i++) {
                    parsed[i] = Color.parseColor(colors.get(i).getAsString());
                }
                drawable.setColors(parsed);
                drawable.setOrientation(orientation(BasicCatalogRenderer.optString(
                        definition, "gradientOrientation", "TOP_BOTTOM")));
            }
        } else {
            drawable.setColor(Color.parseColor(BasicCatalogRenderer.optString(
                    definition, "color", "#00000000")));
        }
        float radius = definition.has("cornerRadius")
                ? dp(context, definition.get("cornerRadius")) : 0f;
        if (radius > 0f) {
            drawable.setCornerRadius(radius);
        }
        int strokeWidth = definition.has("strokeWidth")
                ? Math.round(dp(context, definition.get("strokeWidth"))) : 0;
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, Color.parseColor(BasicCatalogRenderer.optString(
                    definition, "strokeColor", "#FF000000")));
        }
        return drawable;
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
                if (values.size() == 4) {
                    left = Math.round(dp(context, values.get(0)));
                    top = Math.round(dp(context, values.get(1)));
                    right = Math.round(dp(context, values.get(2)));
                    bottom = Math.round(dp(context, values.get(3)));
                }
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

    /** 在 view 树内找 TextView 并应用 textColor / textSize / textStyle。 */
    private static void applyTextAttributes(A2uiRenderContext context, JsonObject style, View view) {
        boolean hasColor = style.has("textColor");
        boolean hasSize = style.has("textSize");
        boolean hasStyle = style.has("textStyle");
        if (!hasColor && !hasSize && !hasStyle) return;
        applyTextToView(context, style, view, hasColor, hasSize, hasStyle);
    }

    private static void applyTextToView(A2uiRenderContext context, JsonObject style, View view,
                                        boolean hasColor, boolean hasSize, boolean hasStyle) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (hasColor) {
                tv.setTextColor(Color.parseColor(style.get("textColor").getAsString()));
            }
            if (hasSize) {
                float sp = style.get("textSize").getAsFloat();
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
            }
            if (hasStyle) {
                String s = style.get("textStyle").getAsString();
                int tf = Typeface.NORMAL;
                if ("bold".equals(s)) tf = Typeface.BOLD;
                else if ("italic".equals(s)) tf = Typeface.ITALIC;
                else if ("boldItalic".equals(s)) tf = Typeface.BOLD_ITALIC;
                tv.setTypeface(null, tf);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTextToView(context, style, group.getChildAt(i), hasColor, hasSize, hasStyle);
            }
        }
    }

    private static float dp(A2uiRenderContext context, JsonElement value) {
        return value.getAsFloat() * context.getContext().getResources().getDisplayMetrics().density;
    }

    private static GradientDrawable.Orientation orientation(String value) {
        switch (value) {
            case "BOTTOM_TOP": return GradientDrawable.Orientation.BOTTOM_TOP;
            case "LEFT_RIGHT": return GradientDrawable.Orientation.LEFT_RIGHT;
            case "RIGHT_LEFT": return GradientDrawable.Orientation.RIGHT_LEFT;
            case "TL_BR": return GradientDrawable.Orientation.TL_BR;
            case "BR_TL": return GradientDrawable.Orientation.BR_TL;
            case "TR_BL": return GradientDrawable.Orientation.TR_BL;
            case "BL_TR": return GradientDrawable.Orientation.BL_TR;
            case "TOP_BOTTOM": return GradientDrawable.Orientation.TOP_BOTTOM;
            default: return GradientDrawable.Orientation.TOP_BOTTOM;
        }
    }
}
