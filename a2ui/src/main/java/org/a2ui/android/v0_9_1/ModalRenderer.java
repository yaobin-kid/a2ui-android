package org.a2ui.android.v0_9_1;

import android.app.AlertDialog;
import android.view.View;

import com.google.gson.JsonObject;

final class ModalRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        View trigger = context.renderChild(RenderSupport.requiredId(component, "trigger"));
        Object existingAction = trigger.getTag();
        trigger.setOnClickListener(v -> {
            if (existingAction instanceof ComponentAction) {
                ((ComponentAction) existingAction).dispatch();
            }
            View content = context.renderChild(RenderSupport.requiredId(component, "content"));
            new AlertDialog.Builder(context.getContext()).setView(content).show();
        });
        return trigger;
    }
}
