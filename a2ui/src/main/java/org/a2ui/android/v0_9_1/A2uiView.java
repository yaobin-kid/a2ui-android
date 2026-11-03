package org.a2ui.android.v0_9_1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.core.A2uiEngine;
import org.a2ui.android.v0_9_1.core.A2uiException;
import org.a2ui.android.v0_9_1.core.SurfaceState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2UI 原生 Android 容器。调用 {@link #render(String)} 传入单条消息、消息数组或 JSONL 即可渲染。
 */
public class A2uiView extends FrameLayout {
    private final LinearLayout surfacesLayout;
    private final Map<String, FrameLayout> surfaceViews = new LinkedHashMap<>();
    private final Map<String, A2uiComponentRenderer> customRenderers = new HashMap<>();
    private final Map<String, A2uiComponentStyler> componentTypeStylers = new HashMap<>();
    private final Map<String, A2uiComponentStyler> componentStylers = new HashMap<>();
    private final A2uiEngine engine;
    private final BasicCatalogRenderer renderer;
    private A2uiActionListener actionListener;
    private A2uiErrorListener errorListener;
    private A2uiImageLoader imageLoader = new DefaultImageLoader();
    private A2uiComponentStyler globalComponentStyler;

    public A2uiView(Context context) {
        this(context, null);
    }

    public A2uiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public A2uiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        surfacesLayout = new LinearLayout(context);
        surfacesLayout.setOrientation(LinearLayout.VERTICAL);
        addView(surfacesLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        engine = new A2uiEngine(new A2uiEngine.Listener() {
            @Override
            public void onSurfaceChanged(String surfaceId) {
                runOnMain(() -> renderSurface(surfaceId));
            }

            @Override
            public void onSurfaceDeleted(String surfaceId) {
                runOnMain(() -> removeSurface(surfaceId));
            }
        });
        renderer = new BasicCatalogRenderer(this);
        registerOpenUrlFunction();
    }

    /** 渲染 v0.9.1 单条 JSON、JSON 数组或 JSONL 消息流。 */
    public void render(String json) {
        runOnMain(() -> engine.accept(json));
    }

    /** 渲染一条已经解析的 v0.9.1 消息。 */
    public void render(JsonObject message) {
        runOnMain(() -> engine.accept(message));
    }

    public A2uiEngine getEngine() {
        return engine;
    }

    /** 返回指定 Surface 的原生容器，ID 与协议中的 surfaceId 完全一致。 */
    public View getSurfaceView(String surfaceId) {
        return surfaceViews.get(surfaceId);
    }

    public void setActionListener(A2uiActionListener listener) {
        this.actionListener = listener;
    }

    public void setErrorListener(A2uiErrorListener listener) {
        this.errorListener = listener;
    }

    public void setImageLoader(A2uiImageLoader loader) {
        this.imageLoader = loader == null ? new DefaultImageLoader() : loader;
    }

    /** 注册自定义组件类型；已有 Surface 会立即刷新。 */
    public void registerComponentRenderer(String componentType, A2uiComponentRenderer componentRenderer) {
        if (componentType == null || componentType.trim().isEmpty() || componentRenderer == null) {
            throw new IllegalArgumentException("componentType 和 componentRenderer 不能为空");
        }
        customRenderers.put(componentType, componentRenderer);
        refreshSurfaces();
    }

    /** 设置所有组件共用的原生样式设置器；传 null 可清除。 */
    public void setGlobalComponentStyler(A2uiComponentStyler styler) {
        globalComponentStyler = styler;
        refreshSurfaces();
    }

    /** 注册某个组件类型的样式设置器；传 null 可移除。 */
    public void registerComponentTypeStyler(String componentType, A2uiComponentStyler styler) {
        requireKey(componentType, "componentType");
        if (styler == null) {
            componentTypeStylers.remove(componentType);
        } else {
            componentTypeStylers.put(componentType, styler);
        }
        refreshSurfaces();
    }

    /** 为所有 Surface 中指定 ID 的组件注册私有样式设置器；传 null 可移除。 */
    public void registerComponentStyler(String componentId, A2uiComponentStyler styler) {
        registerComponentStyler("*", componentId, styler);
    }

    /** 为指定 Surface 中指定 ID 的组件注册私有样式设置器；传 null 可移除。 */
    public void registerComponentStyler(String surfaceId, String componentId, A2uiComponentStyler styler) {
        requireKey(surfaceId, "surfaceId");
        requireKey(componentId, "componentId");
        String key = componentKey(surfaceId, componentId);
        if (styler == null) {
            componentStylers.remove(key);
        } else {
            componentStylers.put(key, styler);
        }
        refreshSurfaces();
    }

    A2uiComponentStyler getComponentStyler(String surfaceId, String componentId, String componentType) {
        A2uiComponentStyler styler = componentStylers.get(componentKey(surfaceId, componentId));
        if (styler == null) {
            styler = componentStylers.get(componentKey("*", componentId));
        }
        if (styler == null) {
            styler = componentTypeStylers.get(componentType);
        }
        return styler == null ? globalComponentStyler : styler;
    }

    A2uiComponentRenderer getCustomRenderer(String componentType) {
        return customRenderers.get(componentType);
    }

    A2uiImageLoader getImageLoader() {
        return imageLoader;
    }

    void dispatchAction(org.a2ui.android.v0_9_1.core.A2uiAction action) {
        if (actionListener != null) {
            actionListener.onAction(action);
        }
    }

    void reportError(String surfaceId, String componentId, String message) {
        if (errorListener != null) {
            errorListener.onError(surfaceId, componentId, message);
        }
    }

    private void renderSurface(String surfaceId) {
        SurfaceState surface = engine.getSurface(surfaceId);
        if (surface == null) {
            removeSurface(surfaceId);
            return;
        }
        FrameLayout container = surfaceViews.get(surfaceId);
        if (container == null) {
            container = new FrameLayout(getContext());
            container.setTag(surfaceId);
            surfaceViews.put(surfaceId, container);
            surfacesLayout.addView(container, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        container.removeAllViews();
        A2uiRenderContext context = new A2uiRenderContext(this, renderer, surface, "");
        View content;
        if (surface.getComponent("root") == null) {
            content = new ProgressBar(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    dp(36), dp(36), Gravity.CENTER);
            content.setLayoutParams(params);
            container.setMinimumHeight(dp(72));
        } else {
            container.setMinimumHeight(0);
            content = renderer.render("root", context);
        }
        container.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    private void removeSurface(String surfaceId) {
        FrameLayout removed = surfaceViews.remove(surfaceId);
        if (removed != null) {
            surfacesLayout.removeView(removed);
        }
    }

    private void registerOpenUrlFunction() {
        engine.getFunctionRegistry().register("openUrl", args -> {
            JsonElement value = args.get("url");
            if (value == null || !value.isJsonPrimitive()) {
                throw new A2uiException("openUrl 需要字符串参数 url");
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value.getAsString()));
            if (!(getContext() instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            getContext().startActivity(intent);
            return JsonNull.INSTANCE;
        });
    }

    private void refreshSurfaces() {
        for (String surfaceId : surfaceViews.keySet()) {
            renderSurface(surfaceId);
        }
    }

    private static String componentKey(String surfaceId, String componentId) {
        return surfaceId + "\0" + componentId;
    }

    private static void requireKey(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            post(runnable);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
