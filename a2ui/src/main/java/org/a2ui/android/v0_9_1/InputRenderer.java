package org.a2ui.android.v0_9_1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class InputRenderer {
    private final BasicCatalogRenderer renderer;

    InputRenderer(BasicCatalogRenderer renderer) {
        this.renderer = renderer;
    }

    View render(A2uiRenderContext context, JsonObject component, String type) {
        switch (type) {
            case "TextField":
                return textField(context, component);
            case "CheckBox":
                return checkBox(context, component);
            case "ChoicePicker":
                return choicePicker(context, component);
            case "Slider":
                return slider(context, component);
            case "DateTimeInput":
                return dateTimeInput(context, component);
            default:
                throw new IllegalArgumentException("未知输入组件: " + type);
        }
    }

    private View textField(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = vertical(context);
        TextView label = label(context, BasicCatalogRenderer.string(context, component.get("label"), ""));
        container.addView(label);
        EditText input = new EditText(context.getContext());
        input.setText(BasicCatalogRenderer.string(context, component.get("value"), ""));
        input.setSingleLine(true);
        switch (BasicCatalogRenderer.optString(component, "variant", "shortText")) {
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
        String regexp = BasicCatalogRenderer.optString(component, "validationRegexp", "");
        input.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable value) {
                write(context, component.get("value"), new JsonPrimitive(value.toString()));
                validate(context, component, input);
                if (!regexp.isEmpty()) {
                    try {
                        boolean valid = value.length() == 0 || Pattern.matches(regexp, value.toString());
                        if (!valid) {
                            input.setError("输入格式不正确");
                        }
                    } catch (Exception error) {
                        context.reportError(BasicCatalogRenderer.optString(component, "id", ""),
                                "validationRegexp 无效: " + error.getMessage());
                    }
                }
            }
        });
        container.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    private View checkBox(A2uiRenderContext context, JsonObject component) {
        CheckBox checkBox = new CheckBox(context.getContext());
        checkBox.setText(BasicCatalogRenderer.string(context, component.get("label"), ""));
        checkBox.setChecked(BasicCatalogRenderer.bool(context, component.get("value"), false));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            write(context, component.get("value"), new JsonPrimitive(checked));
            validate(context, component, checkBox);
        });
        return checkBox;
    }

    private View choicePicker(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = vertical(context);
        String labelText = BasicCatalogRenderer.string(context, component.get("label"), "");
        TextView label = label(context, labelText);
        if (!labelText.isEmpty()) {
            container.addView(label);
        }
        boolean multiple = "multipleSelection".equals(
                BasicCatalogRenderer.optString(component, "variant", "mutuallyExclusive"));
        boolean chips = "chips".equals(BasicCatalogRenderer.optString(component, "displayStyle", "checkbox"));
        Set<String> selected = new HashSet<>(BasicCatalogRenderer.strings(context, component.get("value")));
        JsonArray options = component.getAsJsonArray("options");
        List<View> optionViews = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        RadioGroup radioGroup = multiple ? null : new RadioGroup(context.getContext());
        radioGroupSetup(radioGroup);

        if (BasicCatalogRenderer.bool(context, component.get("filterable"), false)) {
            EditText filter = new EditText(context.getContext());
            filter.setHint("Filter");
            filter.setSingleLine(true);
            filter.addTextChangedListener(new SimpleTextWatcher() {
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
            String optionLabel = BasicCatalogRenderer.string(context, option.get("label"), "");
            String optionValue = option.get("value").getAsString();
            CompoundButton optionView;
            if (multiple || chips) {
                CheckBox check = new CheckBox(context.getContext());
                if (chips) {
                    check.setButtonDrawable(null);
                    check.setGravity(Gravity.CENTER);
                    check.setPadding(BasicCatalogRenderer.dp(context, 16), BasicCatalogRenderer.dp(context, 8),
                            BasicCatalogRenderer.dp(context, 16), BasicCatalogRenderer.dp(context, 8));
                    check.setBackground(BasicCatalogRenderer.optString(component, "displayStyle", "").isEmpty()
                            ? null : selectableBackground(context));
                }
                optionView = check;
            } else {
                optionView = new RadioButton(context.getContext());
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
                write(context, component.get("value"), values);
                validate(context, component, label);
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

    private View slider(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = vertical(context);
        String labelText = BasicCatalogRenderer.string(context, component.get("label"), "");
        TextView label = label(context, labelText);
        if (!labelText.isEmpty()) {
            container.addView(label);
        }
        double min = BasicCatalogRenderer.number(context, component.get("min"), 0);
        double max = BasicCatalogRenderer.number(context, component.get("max"), 100);
        double value = BasicCatalogRenderer.number(context, component.get("value"), min);
        SeekBar seekBar = new SeekBar(context.getContext());
        seekBar.setMax(1000);
        seekBar.setProgress((int) Math.round(Math.max(0, Math.min(1, (value - min) / (max - min))) * 1000));
        TextView current = new TextView(context.getContext());
        current.setText(formatNumber(value));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                double next = min + (max - min) * progress / 1000.0;
                current.setText(formatNumber(next));
                write(context, component.get("value"), new JsonPrimitive(next));
                validate(context, component, label);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        container.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(current);
        return container;
    }

    private View dateTimeInput(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = vertical(context);
        String labelText = BasicCatalogRenderer.string(context, component.get("label"), "");
        if (!labelText.isEmpty()) {
            container.addView(label(context, labelText));
        }
        EditText input = new EditText(context.getContext());
        input.setFocusable(false);
        input.setClickable(true);
        input.setText(BasicCatalogRenderer.string(context, component.get("value"), ""));
        boolean enableDate = BasicCatalogRenderer.bool(context, component.get("enableDate"), false);
        boolean enableTime = BasicCatalogRenderer.bool(context, component.get("enableTime"), false);
        input.setHint(enableDate && enableTime ? "Select date and time" : enableDate ? "Select date" : "Select time");
        input.setOnClickListener(v -> {
            Calendar calendar = calendar(input.getText().toString());
            if (enableDate) {
                DatePickerDialog dialog = new DatePickerDialog(context.getContext(), (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    if (enableTime) {
                        showTimePicker(context, component, input, calendar);
                    } else {
                        updateDateTime(context, component, input, calendar, true, false);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                Long min = parseDate(BasicCatalogRenderer.string(context, component.get("min"), ""));
                Long max = parseDate(BasicCatalogRenderer.string(context, component.get("max"), ""));
                if (min != null) {
                    dialog.getDatePicker().setMinDate(min);
                }
                if (max != null) {
                    dialog.getDatePicker().setMaxDate(max);
                }
                dialog.show();
            } else if (enableTime) {
                showTimePicker(context, component, input, calendar);
            }
        });
        container.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    private void showTimePicker(A2uiRenderContext context, JsonObject component, EditText input, Calendar calendar) {
        boolean enableDate = BasicCatalogRenderer.bool(context, component.get("enableDate"), false);
        new TimePickerDialog(context.getContext(), (view, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            updateDateTime(context, component, input, calendar, enableDate, true);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateTime(A2uiRenderContext context, JsonObject component, EditText input,
                                Calendar calendar, boolean date, boolean time) {
        String pattern = date && time ? "yyyy-MM-dd'T'HH:mm:ss" : date ? "yyyy-MM-dd" : "HH:mm:ss";
        String value = new SimpleDateFormat(pattern, Locale.US).format(calendar.getTime());
        input.setText(value);
        write(context, component.get("value"), new JsonPrimitive(value));
        validate(context, component, input);
    }

    private static void write(A2uiRenderContext context, JsonElement binding, JsonElement value) {
        String path = BasicCatalogRenderer.bindingPath(binding);
        if (path != null) {
            context.getSurface().setLocalValue(path, value, context.getScope());
        }
    }

    private static void validate(A2uiRenderContext context, JsonObject component, TextView target) {
        JsonArray checks = component.getAsJsonArray("checks");
        boolean valid = true;
        String message = null;
        if (checks != null) {
            for (JsonElement item : checks) {
                JsonObject check = item.getAsJsonObject();
                if (!BasicCatalogRenderer.bool(context, check.get("condition"), false)) {
                    valid = false;
                    message = BasicCatalogRenderer.optString(check, "message", "输入无效");
                    break;
                }
            }
        }
        target.setError(valid ? null : message);
    }

    private static LinearLayout vertical(A2uiRenderContext context) {
        LinearLayout layout = new LinearLayout(context.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(BasicCatalogRenderer.dp(context, 4), BasicCatalogRenderer.dp(context, 4),
                BasicCatalogRenderer.dp(context, 4), BasicCatalogRenderer.dp(context, 4));
        return layout;
    }

    private static TextView label(A2uiRenderContext context, String text) {
        TextView label = new TextView(context.getContext());
        label.setText(text);
        label.setTextColor(Color.rgb(80, 80, 80));
        label.setTextSize(14);
        return label;
    }

    private static android.graphics.drawable.Drawable selectableBackground(A2uiRenderContext context) {
        android.util.TypedValue value = new android.util.TypedValue();
        context.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return context.getContext().getDrawable(value.resourceId);
    }

    private static void radioGroupSetup(RadioGroup group) {
        if (group != null) {
            group.setOrientation(RadioGroup.VERTICAL);
        }
    }

    private static Calendar calendar(String value) {
        Calendar calendar = Calendar.getInstance();
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "HH:mm:ss"}) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                calendar.setTime(format.parse(value));
                break;
            } catch (Exception ignored) {
                // 尝试下一种 ISO 格式。
            }
        }
        return calendar;
    }

    private static Long parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"}) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(value).getTime();
            } catch (Exception ignored) {
                // 尝试下一种 ISO 格式。
            }
        }
        return null;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
