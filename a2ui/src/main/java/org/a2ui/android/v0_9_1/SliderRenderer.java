package org.a2ui.android.v0_9_1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

final class SliderRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        LinearLayout container = InputSupport.vertical(context);
        String labelText = RenderSupport.string(context, component.get("label"), "");
        TextView label = InputSupport.label(context, labelText);
        if (!labelText.isEmpty()) {
            container.addView(label);
        }
        double min = RenderSupport.number(context, component.get("min"), 0);
        double max = RenderSupport.number(context, component.get("max"), 100);
        double value = RenderSupport.number(context, component.get("value"), min);
        SeekBar seekBar = new SeekBar(context.getContext());
        int primary = RenderSupport.primaryColor(context);
        seekBar.setProgressTintList(ColorStateList.valueOf(primary));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(
                Color.argb(60, Color.red(primary), Color.green(primary), Color.blue(primary))));
        seekBar.setThumbTintList(ColorStateList.valueOf(primary));
        seekBar.setMax(1000);
        seekBar.setProgress((int) Math.round(Math.max(0, Math.min(1, (value - min) / (max - min))) * 1000));
        TextView current = new TextView(context.getContext());
        current.setText(InputSupport.formatNumber(value));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                double next = min + (max - min) * progress / 1000.0;
                current.setText(InputSupport.formatNumber(next));
                InputSupport.write(context, component.get("value"), new JsonPrimitive(next));
                InputSupport.validate(context, component, label);
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
}
