package org.a2ui.android.v0_9_1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;

final class ButtonRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout button = new LinearLayout(context.getContext());
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setMinimumHeight(RenderSupport.dp(context, 48));
        button.setPadding(RenderSupport.dp(context, 16), RenderSupport.dp(context, 8),
                RenderSupport.dp(context, 16), RenderSupport.dp(context, 8));
        String variant = RenderSupport.optString(component, "variant", "default");
        int primary = RenderSupport.primaryColor(context);
        if ("primary".equals(variant)) {
            button.setBackground(RenderSupport.ripple(context, primary, Color.WHITE, 8));
        } else if ("borderless".equals(variant)) {
            button.setBackground(RenderSupport.ripple(context, Color.TRANSPARENT, primary, 0));
        } else {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setStroke(RenderSupport.dp(context, 1), Color.rgb(200, 200, 200));
            background.setCornerRadius(RenderSupport.dp(context, 8));
            button.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(RenderSupport.withAlpha(primary, 50)), background, null));
        }
        String childId = RenderSupport.requiredId(component, "child");
        View child = context.renderChild(childId);
        // 只有当子 Text 组件自己没有 style.textColor 时，variant 才设默认颜色。
        // 如果子组件有 textColor（ComponentStyleApplier 已经设置了），不要覆盖它。
        boolean childHasColor = false;
        try {
            JsonObject childComp = context.getSurface().getComponent(childId);
            if (childComp != null && childComp.has("style")
                    && childComp.getAsJsonObject("style").has("textColor")) {
                childHasColor = true;
            }
        } catch (Exception ignored) {}
        if (child instanceof TextView && !childHasColor) {
            if ("primary".equals(variant)) {
                ((TextView) child).setTextColor(Color.WHITE);
            } else if ("borderless".equals(variant)) {
                ((TextView) child).setTextColor(primary);
            }
        }
        button.addView(child);
        button.setTag(new ComponentAction(context, component.getAsJsonObject("action"),
                RenderSupport.optString(component, "id", "")));
        button.setOnClickListener(v -> ((ComponentAction) v.getTag()).dispatch());
        return button;
    }
}
