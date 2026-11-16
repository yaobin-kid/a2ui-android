package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
        // clipToOutline 会按卡片背景轮廓（含圆角）裁剪 onDraw 中绘制的背景图
        card.setClipToOutline(true);
        addBackgroundImage(context, component, card);
        View child = context.renderChild(RenderSupport.requiredId(component, "child"));
        card.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    /**
     * style.background.image 支持字符串（仅图片地址）或对象：
     * {@code {"url": "https://...", "fit": "fill|cover|contain|scaleDown|none"}}。
     * 默认 fill（宽高同步拉伸），铺满整个卡片；卡片是圆角时背景图同步裁剪成圆角。
     */
    private static void addBackgroundImage(A2uiRenderContext context, JsonObject component,
                                           CardBackgroundLayout card) {
        JsonObject style = component.getAsJsonObject("style");
        JsonElement background = style == null ? null : style.get("background");
        if (background == null || !background.isJsonObject()) {
            return;
        }
        JsonElement definition = background.getAsJsonObject().get("image");
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
        ImageView image = new ImageView(context.getContext()) {
            @Override
            public void setImageDrawable(Drawable drawable) {
                super.setImageDrawable(drawable);
                // 图片异步加载完成：0 尺寸子 View 自身不会触发父级重绘，这里显式让卡片重绘
                card.invalidate();
            }
        };
        image.setScaleType(scaleType(fit));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // 0×0 布局参数：图片不参与布局、也不作为子 View 直接绘制，
        // 只作为图片加载的载体，由 CardBackgroundLayout 在 onDraw 中按卡片尺寸铺满绘制。
        card.addView(image, 0, new FrameLayout.LayoutParams(0, 0));
        card.setBackgroundImage(image);
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
