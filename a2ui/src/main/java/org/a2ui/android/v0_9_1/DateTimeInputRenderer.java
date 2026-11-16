package org.a2ui.android.v0_9_1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class DateTimeInputRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = InputSupport.vertical(context);
        String labelText = RenderSupport.string(context, component.get("label"), "");
        if (!labelText.isEmpty()) {
            container.addView(InputSupport.label(context, labelText));
        }
        EditText input = new EditText(context.getContext());
        input.setBackgroundTintList(ColorStateList.valueOf(RenderSupport.primaryColor(context)));
        input.setFocusable(false);
        input.setClickable(true);
        input.setText(RenderSupport.string(context, component.get("value"), ""));
        boolean enableDate = RenderSupport.bool(context, component.get("enableDate"), false);
        boolean enableTime = RenderSupport.bool(context, component.get("enableTime"), false);
        input.setHint(enableDate && enableTime ? "Select date and time" : enableDate ? "Select date" : "Select time");
        input.setOnClickListener(v -> {
            Calendar calendar = InputSupport.calendar(input.getText().toString());
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
                Long min = InputSupport.parseDate(RenderSupport.string(context, component.get("min"), ""));
                Long max = InputSupport.parseDate(RenderSupport.string(context, component.get("max"), ""));
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
        boolean enableDate = RenderSupport.bool(context, component.get("enableDate"), false);
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
        InputSupport.write(context, component.get("value"), new JsonPrimitive(value));
        InputSupport.validate(context, component, input);
    }
}
