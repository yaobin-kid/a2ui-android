package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextRenderer implements A2uiComponentRenderer {
    private static final Pattern MARKDOWN = Pattern.compile("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`)");

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        TextView view = new TextView(context.getContext());
        view.setText(markdown(RenderSupport.string(context, component.get("text"), "")));
        view.setTextColor(Color.rgb(33, 33, 33));
        view.setLineSpacing(0, 1.15f);
        switch (RenderSupport.optString(component, "variant", "body")) {
            case "h1":
                RenderSupport.textStyle(view, 32, Typeface.BOLD);
                break;
            case "h2":
                RenderSupport.textStyle(view, 28, Typeface.BOLD);
                break;
            case "h3":
                RenderSupport.textStyle(view, 24, Typeface.BOLD);
                break;
            case "h4":
                RenderSupport.textStyle(view, 20, Typeface.BOLD);
                break;
            case "h5":
                RenderSupport.textStyle(view, 18, Typeface.BOLD);
                break;
            case "caption":
                RenderSupport.textStyle(view, 12, Typeface.NORMAL);
                view.setTextColor(Color.rgb(100, 100, 100));
                break;
            default:
                RenderSupport.textStyle(view, 16, Typeface.NORMAL);
                break;
        }
        return view;
    }

    private static CharSequence markdown(String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Matcher matcher = MARKDOWN.matcher(value);
        int cursor = 0;
        while (matcher.find()) {
            builder.append(value, cursor, matcher.start());
            String token = matcher.group();
            int start = builder.length();
            if (token.startsWith("**")) {
                builder.append(token, 2, token.length() - 2);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (token.startsWith("*")) {
                builder.append(token, 1, token.length() - 1);
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                builder.append(token, 1, token.length() - 1);
                builder.setSpan(new TypefaceSpan("monospace"), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            cursor = matcher.end();
        }
        builder.append(value, cursor, value.length());
        return builder;
    }
}
