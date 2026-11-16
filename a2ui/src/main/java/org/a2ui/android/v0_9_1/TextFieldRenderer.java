package org.a2ui.android.v0_9_1;

import android.text.Editable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.regex.Pattern;

final class TextFieldRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = InputSupport.vertical(context);
        TextView label = InputSupport.label(context, RenderSupport.string(context, component.get("label"), ""));
        container.addView(label);
        EditText input = new EditText(context.getContext());
        input.setBackgroundTintList(InputSupport.editTextTint(context));
        input.setText(RenderSupport.string(context, component.get("value"), ""));
        input.setSingleLine(true);
        switch (RenderSupport.optString(component, "variant", "shortText")) {
            case "longText":
                input.setSingleLine(false);
                input.setMinLines(3);
                input.setGravity(Gravity.TOP | Gravity.START);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                break;
            case "number":
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
                break;
            case "obscured":
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
            default:
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
        }
        String regexp = RenderSupport.optString(component, "validationRegexp", "");
        input.addTextChangedListener(new InputSupport.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable value) {
                InputSupport.write(context, component.get("value"), new JsonPrimitive(value.toString()));
                InputSupport.validate(context, component, input);
                if (!regexp.isEmpty()) {
                    try {
                        boolean valid = value.length() == 0 || Pattern.matches(regexp, value.toString());
                        if (!valid) {
                            input.setError("输入格式不正确");
                        }
                    } catch (Exception error) {
                        context.reportError(RenderSupport.optString(component, "id", ""),
                                "validationRegexp 无效: " + error.getMessage());
                    }
                }
            }
        });
        container.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }
}
