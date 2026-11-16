package org.a2ui.android.v0_9_1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.core.A2uiException;

import java.util.ArrayList;
import java.util.List;

/** 内置组件渲染器共用的纯工具方法，不含任何组件逻辑。 */
final class RenderSupport {
    private RenderSupport() {
    }

    static JsonElement resolve(A2uiRenderContext context, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return context.getSurface().resolve(value, context.getScope());
    }

    static String string(A2uiRenderContext context, JsonElement value, String fallback) {
        JsonElement resolved = resolve(context, value);
        if (resolved == null || resolved.isJsonNull()) {
            return fallback;
        }
        return resolved.isJsonPrimitive() ? resolved.getAsString() : resolved.toString();
    }

    static boolean bool(A2uiRenderContext context, JsonElement value, boolean fallback) {
        JsonElement resolved = resolve(context, value);
        if (resolved == null || resolved.isJsonNull()) {
            return fallback;
        }
        return resolved.getAsBoolean();
    }

    static double number(A2uiRenderContext context, JsonElement value, double fallback) {
        JsonElement resolved = resolve(context, value);
        if (resolved == null || resolved.isJsonNull()) {
            return fallback;
        }
        return resolved.getAsDouble();
    }

    static List<String> strings(A2uiRenderContext context, JsonElement value) {
        List<String> result = new ArrayList<>();
        JsonElement resolved = resolve(context, value);
        if (resolved != null && resolved.isJsonArray()) {
            for (JsonElement item : resolved.getAsJsonArray()) {
                result.add(item.getAsString());
            }
        }
        return result;
    }

    static String bindingPath(JsonElement value) {
        if (value != null && value.isJsonObject() && value.getAsJsonObject().has("path")) {
            return value.getAsJsonObject().get("path").getAsString();
        }
        return null;
    }

    static String absolutePath(String path, String scope) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return scope == null || scope.isEmpty() ? normalized : scope + normalized;
    }

    static int primaryColor(A2uiRenderContext context) {
        JsonObject theme = context.getSurface().getTheme();
        if (theme != null && theme.has("primaryColor")) {
            try {
                return Color.parseColor(theme.get("primaryColor").getAsString());
            } catch (IllegalArgumentException ignored) {
                // 使用默认主题色。
            }
        }
        return Color.rgb(63, 81, 181);
    }

    static int dp(A2uiRenderContext context, int value) {
        return Math.round(value * context.getContext().getResources().getDisplayMetrics().density);
    }

    static String optString(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    static String requiredId(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            throw new A2uiException("缺少组件引用: " + name);
        }
        return value.getAsString();
    }

    static void textStyle(TextView view, int sp, int style) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTypeface(Typeface.DEFAULT, style);
    }

    static View placeholder(A2uiRenderContext context, String componentId) {
        TextView view = new TextView(context.getContext());
        view.setText("…");
        view.setGravity(Gravity.CENTER);
        view.setContentDescription("Loading " + componentId);
        view.setMinimumHeight(dp(context, 32));
        return view;
    }

    static View errorView(A2uiRenderContext context, String message) {
        TextView view = new TextView(context.getContext());
        view.setText(message);
        view.setTextSize(12);
        view.setTextColor(Color.rgb(180, 40, 40));
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        return view;
    }

    static RippleDrawable ripple(A2uiRenderContext context, int background, int rippleColor, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(background);
        shape.setCornerRadius(dp(context, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(rippleColor, 70)), shape, null);
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
