package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.gson.JsonObject;

final class ImageRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        ImageView view = new ImageView(context.getContext());
        String url = RenderSupport.string(context, component.get("url"), "");
        view.setContentDescription(RenderSupport.string(context, component.get("description"), url));
        switch (RenderSupport.optString(component, "fit", "fill")) {
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
        String variant = RenderSupport.optString(component, "variant", "mediumFeature");
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = RenderSupport.dp(context, 160);
        if ("icon".equals(variant)) {
            width = height = RenderSupport.dp(context, 24);
        } else if ("avatar".equals(variant)) {
            width = height = RenderSupport.dp(context, 48);
            view.setClipToOutline(true);
            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View target, android.graphics.Outline outline) {
                    outline.setOval(0, 0, target.getWidth(), target.getHeight());
                }
            });
        } else if ("smallFeature".equals(variant)) {
            width = RenderSupport.dp(context, 120);
            height = RenderSupport.dp(context, 90);
        } else if ("largeFeature".equals(variant)) {
            height = RenderSupport.dp(context, 240);
        } else if ("header".equals(variant)) {
            height = RenderSupport.dp(context, 180);
        }
        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setBackgroundColor(Color.rgb(238, 238, 238));
        context.loadImage(url, view);
        return view;
    }
}
