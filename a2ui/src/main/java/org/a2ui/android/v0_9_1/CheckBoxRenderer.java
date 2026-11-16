package org.a2ui.android.v0_9_1;

import android.view.View;
import android.widget.CheckBox;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

final class CheckBoxRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        CheckBox checkBox = new CheckBox(context.getContext());
        checkBox.setButtonTintList(InputSupport.controlTint(context));
        checkBox.setText(RenderSupport.string(context, component.get("label"), ""));
        checkBox.setChecked(RenderSupport.bool(context, component.get("value"), false));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            InputSupport.write(context, component.get("value"), new JsonPrimitive(checked));
            InputSupport.validate(context, component, checkBox);
        });
        return checkBox;
    }
}
