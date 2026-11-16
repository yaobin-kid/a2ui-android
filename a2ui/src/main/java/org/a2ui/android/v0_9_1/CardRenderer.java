package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class CardRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        CardBackgroundLayout card = new CardBackgroundLayout(context.getContext());
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(RenderSupport.dp(context, 12));
        card.setBackground(background);
        card.setElevation(RenderSupport.dp(context, 3));
        int padding = RenderSupport.dp(context, 16);
        card.setPadding(padding, padding, padding, padding);
        card.setClipToOutline(true);
        addBackgroundImage(context, component, card);
        View child = context.renderChild(RenderSupport.requiredId(component, "child"));
        card.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    /**
     * backgroundImage 支持字符串（仅图片地址）或对象：
     * {@code {"url": "https://...", "fit": "fill|cover|contain|scaleDown|none"}}。
     * 默认 fill（宽高同步拉伸），铺满整个卡片；圆角由卡片背景轮廓裁剪。
     */
    private static void addBackgroundImage(A2uiRenderContext context, JsonObject component,
                                           CardBackgroundLayout card) {
        JsonElement definition = component.get("backgroundImage");
        if (definition == null || definition.isJsonNull()) {
            return;
        }
        String url;
        String fit = "fill";
        if (definition.isJsonPrimitive()) {
            url = RenderSupport.string(context, definition, "");
        } else if (definition.isJsonObject()) {
            JsonObject object = definition.getAsJsonObject();
            url = RenderSupport.string(context, object.get("url"), "");
            fit = RenderSupport.string(context, object.get("fit"), "fill");
        } else {
            return;
        }
        if (url.trim().isEmpty()) {
            return;
        }
        ImageView image = new ImageView(context.getContext());
        image.setScaleType(scaleType(fit));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // 0×0 的布局参数让背景图不参与卡片尺寸计算，由 CardBackgroundLayout 在测量后铺满卡片。
        card.addView(image, 0, new FrameLayout.LayoutParams(0, 0));
        card.setBackgroundView(image);
        context.loadImage(url, image);
    }

    private static ImageView.ScaleType scaleType(String fit) {
        switch (fit) {
            case "contain":
            case "scaleDown":
                return ImageView.ScaleType.FIT_CENTER;
            case "cover":
                return ImageView.ScaleType.CENTER_CROP;
            case "none":
                return ImageView.ScaleType.CENTER;
            default:
                return ImageView.ScaleType.FIT_XY;
        }
    }
}
