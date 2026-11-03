package org.a2ui.android.v0_9_1;

import android.content.Context;
import android.view.View;

import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.core.SurfaceState;

/** 向自定义组件暴露当前 Surface、数据作用域和子组件渲染能力。 */
public final class A2uiRenderContext {
    private final A2uiView host;
    private final BasicCatalogRenderer renderer;
    private final SurfaceState surface;
    private final String scope;

    A2uiRenderContext(A2uiView host, BasicCatalogRenderer renderer, SurfaceState surface, String scope) {
        this.host = host;
        this.renderer = renderer;
        this.surface = surface;
        this.scope = scope;
    }

    public Context getContext() {
        return host.getContext();
    }

    public SurfaceState getSurface() {
        return surface;
    }

    public String getScope() {
        return scope;
    }

    public View renderChild(String componentId) {
        return renderer.render(componentId, this);
    }

    public A2uiRenderContext withScope(String newScope) {
        return new A2uiRenderContext(host, renderer, surface, newScope);
    }

    public void dispatchAction(JsonObject action, String sourceComponentId) {
        renderer.dispatchAction(this, action, sourceComponentId);
    }

    public void reportError(String componentId, String message) {
        host.reportError(surface.getSurfaceId(), componentId, message);
    }

    A2uiView getHost() {
        return host;
    }
}
