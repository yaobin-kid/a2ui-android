package org.a2ui.android.v0_9_1;

import com.google.gson.JsonObject;

/** 绑定在组件 View 上的 action 包装，供 Button 与 Modal 共享。 */
final class ComponentAction {
    private final A2uiRenderContext context;
    private final JsonObject action;
    private final String componentId;

    ComponentAction(A2uiRenderContext context, JsonObject action, String componentId) {
        this.context = context;
        this.action = action;
        this.componentId = componentId;
    }

    void dispatch() {
        context.dispatchAction(action, componentId);
    }
}
