package org.a2ui.android.v0_9_1;

import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ChoicePickerRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = InputSupport.vertical(context);
        String labelText = RenderSupport.string(context, component.get("label"), "");
        TextView label = InputSupport.label(context, labelText);
        if (!labelText.isEmpty()) {
            container.addView(label);
        }
        boolean multiple = "multipleSelection".equals(
                RenderSupport.optString(component, "variant", "mutuallyExclusive"));
        boolean chips = "chips".equals(RenderSupport.optString(component, "displayStyle", "checkbox"));
        Set<String> selected = new HashSet<>(RenderSupport.strings(context, component.get("value")));
        JsonArray options = component.getAsJsonArray("options");
        List<View> optionViews = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        RadioGroup radioGroup = multiple ? null : new RadioGroup(context.getContext());
        InputSupport.radioGroupSetup(radioGroup);

        if (RenderSupport.bool(context, component.get("filterable"), false)) {
            EditText filter = new EditText(context.getContext());
            filter.setHint("Filter");
            filter.setSingleLine(true);
            filter.addTextChangedListener(new InputSupport.SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable value) {
                    String query = value.toString().toLowerCase(Locale.getDefault());
                    for (int i = 0; i < optionViews.size(); i++) {
                        boolean visible = optionLabels.get(i).toLowerCase(Locale.getDefault()).contains(query);
                        optionViews.get(i).setVisibility(visible ? View.VISIBLE : View.GONE);
                    }
                }
            });
            container.addView(filter);
        }

        for (int i = 0; options != null && i < options.size(); i++) {
            JsonObject option = options.get(i).getAsJsonObject();
            String optionLabel = RenderSupport.string(context, option.get("label"), "");
            String optionValue = option.get("value").getAsString();
            CompoundButton optionView;
            if (multiple || chips) {
                CheckBox check = new CheckBox(context.getContext());
                if (chips) {
                    check.setButtonDrawable(null);
                    check.setGravity(Gravity.CENTER);
                    check.setPadding(RenderSupport.dp(context, 16), RenderSupport.dp(context, 8),
                            RenderSupport.dp(context, 16), RenderSupport.dp(context, 8));
                    check.setBackground(RenderSupport.optString(component, "displayStyle", "").isEmpty()
                            ? null : InputSupport.selectableBackground(context));
                }
                optionView = check;
            } else {
                optionView = new RadioButton(context.getContext());
            }
            if (!chips) {
                optionView.setButtonTintList(InputSupport.controlTint(context));
            }
            optionView.setText(optionLabel);
            optionView.setChecked(selected.contains(optionValue));
            optionView.setOnCheckedChangeListener((button, checked) -> {
                if (!multiple && checked) {
                    selected.clear();
                    for (View other : optionViews) {
                        if (other != button && other instanceof CompoundButton) {
                            ((CompoundButton) other).setChecked(false);
                        }
                    }
                }
                if (checked) {
                    selected.add(optionValue);
                } else {
                    selected.remove(optionValue);
                }
                JsonArray values = new JsonArray();
                for (String value : selected) {
                    values.add(value);
                }
                InputSupport.write(context, component.get("value"), values);
                InputSupport.validate(context, component, label);
            });
            optionViews.add(optionView);
            optionLabels.add(optionLabel);
            if (radioGroup != null && !chips) {
                radioGroup.addView(optionView);
            } else {
                container.addView(optionView);
            }
        }
        if (radioGroup != null && !chips) {
            container.addView(radioGroup);
        }
        return container;
    }
}
