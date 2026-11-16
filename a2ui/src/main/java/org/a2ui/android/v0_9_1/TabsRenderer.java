package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class TabsRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = new LinearLayout(context.getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tabBar = new LinearLayout(context.getContext());
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView scroll = new HorizontalScrollView(context.getContext());
        scroll.addView(tabBar);
        container.addView(scroll);
        FrameLayout content = new FrameLayout(context.getContext());
        container.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        JsonArray tabs = component.getAsJsonArray("tabs");
        List<TextView> buttons = new ArrayList<>();
        int[] selected = {0};
        for (int i = 0; tabs != null && i < tabs.size(); i++) {
            JsonObject tab = tabs.get(i).getAsJsonObject();
            TextView button = new TextView(context.getContext());
            button.setText(RenderSupport.string(context, tab.get("title"), "Tab " + (i + 1)));
            button.setGravity(Gravity.CENTER);
            button.setPadding(RenderSupport.dp(context, 16), RenderSupport.dp(context, 12),
                    RenderSupport.dp(context, 16), RenderSupport.dp(context, 12));
            button.setBackground(RenderSupport.ripple(context, Color.TRANSPARENT,
                    RenderSupport.primaryColor(context), 0));
            tabBar.addView(button);
            buttons.add(button);
            final int index = i;
            button.setOnClickListener(v -> {
                selected[0] = index;
                selectTab(context, tabs, buttons, content, selected[0]);
            });
        }
        if (tabs != null && tabs.size() > 0) {
            selectTab(context, tabs, buttons, content, selected[0]);
        }
        return container;
    }

    private static void selectTab(A2uiRenderContext context, JsonArray tabs, List<TextView> buttons,
                                  FrameLayout content, int selected) {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setTextColor(i == selected ? RenderSupport.primaryColor(context) : Color.GRAY);
            buttons.get(i).setTypeface(Typeface.DEFAULT, i == selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        content.removeAllViews();
        JsonObject tab = tabs.get(selected).getAsJsonObject();
        content.addView(context.renderChild(RenderSupport.requiredId(tab, "child")), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
