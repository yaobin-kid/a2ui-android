package org.a2ui.android.v0_9_1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** 输入组件渲染器共用的工具方法，不含具体组件逻辑。 */
final class InputSupport {
    private InputSupport() {
    }

    static void write(A2uiRenderContext context, JsonElement binding, JsonElement value) {
        String path = RenderSupport.bindingPath(binding);
        if (path != null) {
            context.getSurface().setLocalValue(path, value, context.getScope());
        }
    }

    static void validate(A2uiRenderContext context, JsonObject component, TextView target) {
        JsonArray checks = component.getAsJsonArray("checks");
        boolean valid = true;
        String message = null;
        if (checks != null) {
            for (JsonElement item : checks) {
                JsonObject check = item.getAsJsonObject();
                if (!RenderSupport.bool(context, check.get("condition"), false)) {
                    valid = false;
                    message = RenderSupport.optString(check, "message", "输入无效");
                    break;
                }
            }
        }
        target.setError(valid ? null : message);
    }

    static LinearLayout vertical(A2uiRenderContext context) {
        LinearLayout layout = new LinearLayout(context.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(RenderSupport.dp(context, 4), RenderSupport.dp(context, 4),
                RenderSupport.dp(context, 4), RenderSupport.dp(context, 4));
        return layout;
    }

    static TextView label(A2uiRenderContext context, String text) {
        TextView label = new TextView(context.getContext());
        label.setText(text);
        label.setTextColor(Color.rgb(80, 80, 80));
        label.setTextSize(14);
        return label;
    }

    static ColorStateList controlTint(A2uiRenderContext context) {
        int primary = RenderSupport.primaryColor(context);
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{primary, Color.rgb(158, 158, 158)});
    }

    static ColorStateList editTextTint(A2uiRenderContext context) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{RenderSupport.primaryColor(context), Color.rgb(158, 158, 158)});
    }

    static Drawable selectableBackground(A2uiRenderContext context) {
        TypedValue value = new TypedValue();
        context.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return context.getContext().getDrawable(value.resourceId);
    }

    static void radioGroupSetup(RadioGroup group) {
        if (group != null) {
            group.setOrientation(RadioGroup.VERTICAL);
        }
    }

    static Calendar calendar(String value) {
        Calendar calendar = Calendar.getInstance();
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "HH:mm:ss"}) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                calendar.setTime(format.parse(value));
                break;
            } catch (Exception ignored) {
                // 尝试下一种 ISO 格式。
            }
        }
        return calendar;
    }

    static Long parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"}) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(value).getTime();
            } catch (Exception ignored) {
                // 尝试下一种 ISO 格式。
            }
        }
        return null;
    }

    static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
