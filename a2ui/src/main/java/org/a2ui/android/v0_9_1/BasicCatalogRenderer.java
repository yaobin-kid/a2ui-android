package org.a2ui.android.v0_9_1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.a2ui.android.v0_9_1.core.A2uiAction;
import org.a2ui.android.v0_9_1.core.A2uiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BasicCatalogRenderer {
    private static final Pattern MARKDOWN = Pattern.compile("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`)");
    private final A2uiView host;
    private final InputRenderer inputRenderer;

    BasicCatalogRenderer(A2uiView host) {
        this.host = host;
        this.inputRenderer = new InputRenderer(this);
    }

    View render(String componentId, A2uiRenderContext context) {
        JsonObject component = context.getSurface().getComponent(componentId);
        if (component == null) {
            return placeholder(context, componentId);
        }
        String type = optString(component, "component", "");
        try {
            A2uiComponentRenderer custom = host.getCustomRenderer(type);
            View view;
            if (custom != null) {
                view = custom.render(context, component);
            } else {
                switch (type) {
                    case "Text":
                        view = text(context, component);
                        break;
                    case "Image":
                        view = image(context, component);
                        break;
                    case "Icon":
                        view = icon(context, component);
                        break;
                    case "Video":
                        view = video(context, component, false);
                        break;
                    case "AudioPlayer":
                        view = video(context, component, true);
                        break;
                    case "Row":
                        view = layout(context, component, LinearLayout.HORIZONTAL, false);
                        break;
                    case "Column":
                        view = layout(context, component, LinearLayout.VERTICAL, false);
                        break;
                    case "List":
                        view = list(context, component);
                        break;
                    case "Card":
                        view = card(context, component);
                        break;
                    case "Tabs":
                        view = tabs(context, component);
                        break;
                    case "Modal":
                        view = modal(context, component);
                        break;
                    case "Divider":
                        view = divider(context, component);
                        break;
                    case "Button":
                        view = button(context, component);
                        break;
                    case "TextField":
                    case "CheckBox":
                    case "ChoicePicker":
                    case "Slider":
                    case "DateTimeInput":
                        view = inputRenderer.render(context, component, type);
                        break;
                    default:
                        context.reportError(componentId, "不支持的组件类型: " + type);
                        view = errorView(context, "Unsupported component: " + type);
                        break;
                }
            }
            if (view == null) {
                throw new A2uiException("组件渲染器返回 null: " + type);
            }
            ComponentStyleApplier.apply(context, component, view);
            A2uiComponentStyler styler = host.getComponentStyler(
                    context.getSurface().getSurfaceId(), componentId, type);
            if (styler != null) {
                styler.apply(view, component, context);
            }
            applyAccessibility(context, component, view);
            return view;
        } catch (Exception error) {
            context.reportError(componentId, error.getMessage() == null ? error.toString() : error.getMessage());
            return errorView(context, "Render error: " + componentId);
        }
    }

    void dispatchAction(A2uiRenderContext context, JsonObject action, String sourceComponentId) {
        if (action == null) {
            return;
        }
        try {
            if (action.has("event")) {
                host.dispatchAction(A2uiAction.fromEvent(
                        context.getSurface(), sourceComponentId, action, context.getScope()));
            } else if (action.has("functionCall")) {
                context.getSurface().callFunction(
                        action.getAsJsonObject("functionCall"), context.getScope());
            }
        } catch (Exception error) {
            context.reportError(sourceComponentId, "Action 执行失败: " + error.getMessage());
        }
    }

    A2uiView getHost() {
        return host;
    }

    private View text(A2uiRenderContext context, JsonObject component) {
        TextView view = new TextView(context.getContext());
        view.setText(markdown(string(context, component.get("text"), "")));
        view.setTextColor(Color.rgb(33, 33, 33));
        view.setLineSpacing(0, 1.15f);
        switch (optString(component, "variant", "body")) {
            case "h1":
                textStyle(view, 32, Typeface.BOLD);
                break;
            case "h2":
                textStyle(view, 28, Typeface.BOLD);
                break;
            case "h3":
                textStyle(view, 24, Typeface.BOLD);
                break;
            case "h4":
                textStyle(view, 20, Typeface.BOLD);
                break;
            case "h5":
                textStyle(view, 18, Typeface.BOLD);
                break;
            case "caption":
                textStyle(view, 12, Typeface.NORMAL);
                view.setTextColor(Color.rgb(100, 100, 100));
                break;
            default:
                textStyle(view, 16, Typeface.NORMAL);
                break;
        }
        return view;
    }

    private View image(A2uiRenderContext context, JsonObject component) {
        ImageView view = new ImageView(context.getContext());
        String url = string(context, component.get("url"), "");
        view.setContentDescription(string(context, component.get("description"), url));
        switch (optString(component, "fit", "fill")) {
            case "contain":
            case "scaleDown":
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case "cover":
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case "none":
                view.setScaleType(ImageView.ScaleType.CENTER);
                break;
            default:
                view.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
        }
        String variant = optString(component, "variant", "mediumFeature");
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = dp(context, 160);
        if ("icon".equals(variant)) {
            width = height = dp(context, 24);
        } else if ("avatar".equals(variant)) {
            width = height = dp(context, 48);
            view.setClipToOutline(true);
            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View target, android.graphics.Outline outline) {
                    outline.setOval(0, 0, target.getWidth(), target.getHeight());
                }
            });
        } else if ("smallFeature".equals(variant)) {
            width = dp(context, 120);
            height = dp(context, 90);
        } else if ("largeFeature".equals(variant)) {
            height = dp(context, 240);
        } else if ("header".equals(variant)) {
            height = dp(context, 180);
        }
        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setBackgroundColor(Color.rgb(238, 238, 238));
        host.getImageLoader().load(url, view);
        return view;
    }

    private View icon(A2uiRenderContext context, JsonObject component) {
        JsonElement resolved = resolve(context, component.get("name"));
        if (resolved != null && resolved.isJsonObject()) {
            context.reportError(optString(component, "id", ""), "默认 Icon 渲染器不支持 svgPath，请注册自定义渲染器");
            return errorView(context, "SVG icon");
        }
        String name = resolved == null || resolved.isJsonNull() ? "help" : resolved.getAsString();
        ImageView view = new ImageView(context.getContext());
        view.setImageResource(iconResource(name));
        view.setColorFilter(primaryColor(context));
        view.setContentDescription(name);
        view.setLayoutParams(new ViewGroup.LayoutParams(dp(context, 24), dp(context, 24)));
        return view;
    }

    private View video(A2uiRenderContext context, JsonObject component, boolean audio) {
        LinearLayout container = new LinearLayout(context.getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        if (audio) {
            TextView description = new TextView(context.getContext());
            description.setText(string(context, component.get("description"), "Audio"));
            description.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), 0);
            container.addView(description);
        }
        VideoView video = new VideoView(context.getContext());
        MediaController controller = new MediaController(context.getContext());
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setVideoURI(Uri.parse(string(context, component.get("url"), "")));
        container.addView(video, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, audio ? 72 : 220)));
        return container;
    }

    private View layout(A2uiRenderContext context, JsonObject component, int orientation, boolean insideList) {
        LinearLayout layout = new LinearLayout(context.getContext());
        layout.setOrientation(orientation);
        layout.setGravity("center".equals(optString(component, "align", ""))
                ? (orientation == LinearLayout.VERTICAL ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL)
                : Gravity.TOP);
        List<View> children = children(context, component.get("children"));
        String justify = optString(component, "justify", "start");
        String align = optString(component, "align", "stretch");
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
            int margin = dp(context, 2);
            params.setMargins(margin, margin, margin, margin);
            layout.addView(child, params);
            boolean between = "spaceBetween".equals(justify) && i < children.size() - 1;
            boolean around = "spaceAround".equals(justify) || "spaceEvenly".equals(justify);
            if (between || around) {
                addSpacer(context, layout, horizontal);
            }
        }
        if (!insideList && "List".equals(optString(component, "component", ""))) {
            return list(context, component);
        }
        return layout;
    }

    private View list(A2uiRenderContext context, JsonObject component) {
        boolean horizontal = "horizontal".equals(optString(component, "direction", "vertical"));
        JsonObject copy = component.deepCopy();
        copy.addProperty("component", horizontal ? "Row" : "Column");
        View content = layout(context, copy, horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL, true);
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

    private View card(A2uiRenderContext context, JsonObject component) {
        FrameLayout card = new FrameLayout(context.getContext());
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(context, 12));
        card.setBackground(background);
        card.setElevation(dp(context, 3));
        int padding = dp(context, 16);
        card.setPadding(padding, padding, padding, padding);
        card.setClipToOutline(true);
        View child = context.renderChild(requiredId(component, "child"));
        card.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View tabs(A2uiRenderContext context, JsonObject component) {
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
            button.setText(string(context, tab.get("title"), "Tab " + (i + 1)));
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
            button.setBackground(ripple(context, Color.TRANSPARENT, primaryColor(context), 0));
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

    private void selectTab(A2uiRenderContext context, JsonArray tabs, List<TextView> buttons,
                           FrameLayout content, int selected) {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setTextColor(i == selected ? primaryColor(context) : Color.GRAY);
            buttons.get(i).setTypeface(Typeface.DEFAULT, i == selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        content.removeAllViews();
        JsonObject tab = tabs.get(selected).getAsJsonObject();
        content.addView(context.renderChild(requiredId(tab, "child")), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View modal(A2uiRenderContext context, JsonObject component) {
        View trigger = context.renderChild(requiredId(component, "trigger"));
        Object existingAction = trigger.getTag();
        trigger.setOnClickListener(v -> {
            if (existingAction instanceof ComponentAction) {
                ((ComponentAction) existingAction).dispatch();
            }
            View content = context.renderChild(requiredId(component, "content"));
            new AlertDialog.Builder(context.getContext()).setView(content).show();
        });
        return trigger;
    }

    private View divider(A2uiRenderContext context, JsonObject component) {
        View view = new View(context.getContext());
        view.setBackgroundColor(Color.rgb(220, 220, 220));
        boolean horizontal = !"vertical".equals(optString(component, "axis", "horizontal"));
        view.setLayoutParams(new ViewGroup.LayoutParams(
                horizontal ? ViewGroup.LayoutParams.MATCH_PARENT : dp(context, 1),
                horizontal ? dp(context, 1) : ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    private View button(A2uiRenderContext context, JsonObject component) {
        LinearLayout button = new LinearLayout(context.getContext());
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));
        String variant = optString(component, "variant", "default");
        int primary = primaryColor(context);
        if ("primary".equals(variant)) {
            button.setBackground(ripple(context, primary, Color.WHITE, 8));
        } else if ("borderless".equals(variant)) {
            button.setBackground(ripple(context, Color.TRANSPARENT, primary, 0));
        } else {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setStroke(dp(context, 1), Color.rgb(200, 200, 200));
            background.setCornerRadius(dp(context, 8));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(withAlpha(primary, 50)),
                    background, null));
        }
        String childId = requiredId(component, "child");
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
                optString(component, "id", "")));
        button.setOnClickListener(v -> ((ComponentAction) v.getTag()).dispatch());
        return button;
    }

    private final class ComponentAction {
        private final A2uiRenderContext context;
        private final JsonObject action;
        private final String componentId;

        ComponentAction(A2uiRenderContext context, JsonObject action, String componentId) {
            this.context = context;
            this.action = action;
            this.componentId = componentId;
        }

        void dispatch() {
            dispatchAction(context, action, componentId);
        }
    }

    private List<View> children(A2uiRenderContext context, JsonElement definition) {
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
        String fullPath = absolutePath(path, context.getScope());
        JsonElement data = context.getSurface().getValue(path, context.getScope());
        if (data == null || !data.isJsonArray()) {
            return views;
        }
        JsonArray array = data.getAsJsonArray();
        String componentId = requiredId(template, "componentId");
        for (int i = 0; i < array.size(); i++) {
            A2uiRenderContext scoped = context.withScope(fullPath + "/" + i);
            views.add(scoped.renderChild(componentId));
        }
        return views;
    }

    private View placeholder(A2uiRenderContext context, String componentId) {
        TextView view = new TextView(context.getContext());
        view.setText("…");
        view.setGravity(Gravity.CENTER);
        view.setContentDescription("Loading " + componentId);
        view.setMinimumHeight(dp(context, 32));
        return view;
    }

    private View errorView(A2uiRenderContext context, String message) {
        TextView view = new TextView(context.getContext());
        view.setText(message);
        view.setTextSize(12);
        view.setTextColor(Color.rgb(180, 40, 40));
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        return view;
    }

    private void applyAccessibility(A2uiRenderContext context, JsonObject component, View view) {
        JsonObject accessibility = component.getAsJsonObject("accessibility");
        if (accessibility == null) {
            return;
        }
        String label = string(context, accessibility.get("label"), "");
        String description = string(context, accessibility.get("description"), "");
        String text = (label + (label.isEmpty() || description.isEmpty() ? "" : ". ") + description).trim();
        if (!text.isEmpty()) {
            view.setContentDescription(text);
        }
        if (android.os.Build.VERSION.SDK_INT >= 26 && !description.isEmpty()) {
            view.setTooltipText(description);
        }
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

    private static int mainGravity(String justify, boolean horizontal) {
        int axis = horizontal ? 0 : Gravity.CENTER_VERTICAL;
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

    private static RippleDrawable ripple(A2uiRenderContext context, int background, int rippleColor, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(background);
        shape.setCornerRadius(dp(context, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(rippleColor, 70)), shape, null);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static CharSequence markdown(String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Matcher matcher = MARKDOWN.matcher(value);
        int cursor = 0;
        while (matcher.find()) {
            builder.append(value, cursor, matcher.start());
            String token = matcher.group();
            int start = builder.length();
            if (token.startsWith("**")) {
                builder.append(token, 2, token.length() - 2);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (token.startsWith("*")) {
                builder.append(token, 1, token.length() - 1);
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                builder.append(token, 1, token.length() - 1);
                builder.setSpan(new TypefaceSpan("monospace"), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            cursor = matcher.end();
        }
        builder.append(value, cursor, value.length());
        return builder;
    }

    private static int iconResource(String name) {
        if (name == null) {
            return android.R.drawable.ic_menu_help;
        }
        switch (name) {
            case "add": return android.R.drawable.ic_menu_add;
            case "arrowBack": return android.R.drawable.ic_media_rew;
            case "arrowForward": return android.R.drawable.ic_media_ff;
            case "call":
            case "phone": return android.R.drawable.ic_menu_call;
            case "camera": return android.R.drawable.ic_menu_camera;
            case "check": return android.R.drawable.checkbox_on_background;
            case "close": return android.R.drawable.ic_menu_close_clear_cancel;
            case "delete": return android.R.drawable.ic_menu_delete;
            case "download": return android.R.drawable.stat_sys_download;
            case "upload": return android.R.drawable.ic_menu_upload;
            case "edit": return android.R.drawable.ic_menu_edit;
            case "event":
            case "calendarToday": return android.R.drawable.ic_menu_month;
            case "error":
            case "warning": return android.R.drawable.ic_dialog_alert;
            case "fastForward": return android.R.drawable.ic_media_ff;
            case "favorite":
            case "star": return android.R.drawable.star_big_on;
            case "favoriteOff":
            case "starOff": return android.R.drawable.star_big_off;
            case "starHalf": return android.R.drawable.star_on;
            case "help": return android.R.drawable.ic_menu_help;
            case "home": return android.R.drawable.ic_menu_myplaces;
            case "info": return android.R.drawable.ic_dialog_info;
            case "locationOn": return android.R.drawable.ic_menu_mylocation;
            case "lock":
            case "lockOpen": return android.R.drawable.ic_lock_lock;
            case "mail": return android.R.drawable.ic_dialog_email;
            case "menu":
            case "moreVert":
            case "moreHoriz": return android.R.drawable.ic_menu_more;
            case "notifications":
            case "notificationsOff": return android.R.drawable.ic_menu_info_details;
            case "pause": return android.R.drawable.ic_media_pause;
            case "payment": return android.R.drawable.ic_menu_directions;
            case "person":
            case "accountCircle": return android.R.drawable.ic_menu_myplaces;
            case "photo": return android.R.drawable.ic_menu_gallery;
            case "play": return android.R.drawable.ic_media_play;
            case "refresh": return android.R.drawable.ic_menu_rotate;
            case "rewind": return android.R.drawable.ic_media_rew;
            case "search": return android.R.drawable.ic_menu_search;
            case "send": return android.R.drawable.ic_menu_send;
            case "settings": return android.R.drawable.ic_menu_preferences;
            case "share": return android.R.drawable.ic_menu_share;
            case "shoppingCart": return android.R.drawable.ic_menu_agenda;
            case "skipNext": return android.R.drawable.ic_media_next;
            case "skipPrevious": return android.R.drawable.ic_media_previous;
            case "stop": return android.R.drawable.ic_media_pause;
            case "visibility":
            case "visibilityOff": return android.R.drawable.ic_menu_view;
            case "volumeDown":
            case "volumeMute":
            case "volumeOff":
            case "volumeUp": return android.R.drawable.ic_lock_silent_mode_off;
            default: return android.R.drawable.ic_menu_help;
        }
    }
}
