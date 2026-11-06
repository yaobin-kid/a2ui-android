package org.a2ui.android.v0_9_1;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.a2ui.android.v0_9_1.core.A2uiAction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class A2uiViewTest {
    @Test
    public void rendersTextAndReactsToDataUpdate() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Text\",\"text\":{\"path\":\"/title\"}}]}}");
        view.render("{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"main\",\"path\":\"/title\",\"value\":\"Hello\"}}");

        assertNotNull(view.getSurfaceView("main"));
        assertTrue(hasText(view, "Hello"));
        view.render("{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"main\",\"path\":\"/title\",\"value\":\"Updated\"}}");
        assertTrue(hasText(view, "Updated"));
    }

    @Test
    public void rendersDynamicChildrenWithScopedPaths() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"list\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"list\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Column\",\"children\":{\"path\":\"/items\",\"componentId\":\"item\"}},"
                + "{\"id\":\"item\",\"component\":\"Text\",\"text\":{\"path\":\"name\"}}]}}"
                + "\n{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"list\",\"value\":{\"items\":[{\"name\":\"One\"},{\"name\":\"Two\"}]}}}");

        assertTrue(hasText(view, "One"));
        assertTrue(hasText(view, "Two"));
    }

    @Test
    public void textFieldWritesBackToDataModel() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"form\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"form\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"TextField\",\"label\":\"Name\",\"value\":{\"path\":\"/name\"}}]}}"
                + "\n{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"form\",\"value\":{\"name\":\"Before\"}}}");

        EditText input = find(view, EditText.class);
        input.setText("After");
        assertEquals("After", view.getEngine().getSurface("form").getDataModel().get("name").getAsString());
    }

    @Test
    public void buttonDispatchesProtocolAction() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        AtomicReference<A2uiAction> received = new AtomicReference<>();
        view.setActionListener(received::set);
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Button\",\"child\":\"label\",\"action\":{\"event\":{\"name\":\"save\",\"context\":{\"value\":42}}}},"
                + "{\"id\":\"label\",\"component\":\"Text\",\"text\":\"Save\"}]}}");

        click(view);
        assertNotNull(received.get());
        assertEquals("save", received.get().getName());
        assertEquals("main", received.get().getSurfaceId());
        assertEquals(42, received.get().getContext().get("value").getAsInt());
        assertEquals("v0.9.1", received.get().toJson().get("version").getAsString());
    }

    @Test
    public void appliesTextColorAndSizeToTextComponent() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Text\",\"text\":\"Hello\",\"variant\":\"body\","
                + "\"style\":{\"textColor\":\"#FF001E63\",\"textSize\":26,\"textStyle\":\"bold\"}}]}}");

        android.widget.TextView tv = (android.widget.TextView) ((ViewGroup) view.getSurfaceView("main")).getChildAt(0);
        int expectedColor = android.graphics.Color.parseColor("#FF001E63");
        assertEquals("textColor 应被 style 覆盖", expectedColor, tv.getCurrentTextColor());
        assertEquals("textSize 应覆盖为 26sp", 26f, tv.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity, 0.01f);
        assertEquals("bold", android.graphics.Typeface.BOLD, tv.getTypeface().getStyle());
    }

    @Test
    public void appliesTextColorAndSizeToButtonChildText() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"btn\",\"component\":\"Button\",\"variant\":\"filled\",\"child\":\"btn-text\","
                + "\"style\":{\"background\":{\"color\":\"#FF6750A4\",\"cornerRadius\":24},"
                + "\"textColor\":\"#FFFFFFFF\",\"textSize\":16,\"padding\":[28,12,28,12]}},"
                + "{\"id\":\"btn-text\",\"component\":\"Text\",\"text\":\"按钮\",\"variant\":\"body\"}}]}}");

        android.view.ViewGroup btn = (android.view.ViewGroup) ((ViewGroup) view.getSurfaceView("main")).getChildAt(0);
        android.widget.TextView tv = (android.widget.TextView) btn.getChildAt(0);
        System.out.println("=== Button child debug ===");
        System.out.println("btn container bg class: " + btn.getBackground().getClass().getName());
        System.out.println("tv textColor: " + Integer.toHexString(tv.getCurrentTextColor()));
        System.out.println("tv textSize: " + tv.getTextSize());
        System.out.println("tv text: " + tv.getText());
        // Button variant "filled" 在 button() 里不走 primary/borderless 分支，所以不会被 button() 覆盖
        // 期望 ComponentStyleApplier 的 applyTextAttributes 递归覆盖
        assertEquals("Button 子 TextView textColor 应被 style 覆盖",
                android.graphics.Color.WHITE, tv.getCurrentTextColor());
        assertEquals(16f, tv.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity, 0.01f);
    }

    @Test
    public void appliesDeclarativeBackgroundAndPadding() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Text\",\"text\":\"Styled\",\"style\":{"
                + "\"background\":{\"color\":\"#FF112233\",\"cornerRadius\":8,\"strokeWidth\":1,\"strokeColor\":\"#FF000000\"},"
                + "\"padding\":[1,2,3,4],\"elevation\":5,\"alpha\":0.5}}]}}");

        View content = ((ViewGroup) view.getSurfaceView("main")).getChildAt(0);
        float density = view.getResources().getDisplayMetrics().density;
        assertTrue(content.getBackground() instanceof GradientDrawable);
        assertEquals(Math.round(density), content.getPaddingLeft());
        assertEquals(Math.round(2 * density), content.getPaddingTop());
        assertEquals(Math.round(3 * density), content.getPaddingRight());
        assertEquals(Math.round(4 * density), content.getPaddingBottom());
        assertEquals(5 * density, content.getElevation(), 0.01f);
        assertEquals(0.5f, content.getAlpha(), 0.01f);
    }

    @Test
    public void styleParsingFailureIsSilent() {
        // style 字段不是 v0.9.1 协议定义的——属于客户端扩展。
        // 协议要求未知字段静默忽略。非法 style 声明不应抛异常。
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Text\",\"text\":\"OK\",\"style\":42,"
                + "\"foo\":\"bar\"}]}}"); // style 类型非法，但不抛异常
        View content = ((ViewGroup) view.getSurfaceView("main")).getChildAt(0);
        assertNotNull(content);
    }

    @Test
    public void appliesSurfacePrimaryColorToNativeInputs() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\","
                + "\"theme\":{\"primaryColor\":\"#112233\"}}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Column\",\"children\":[\"input\",\"check\",\"slider\"]},"
                + "{\"id\":\"input\",\"component\":\"TextField\",\"label\":\"Name\",\"value\":\"\"},"
                + "{\"id\":\"check\",\"component\":\"CheckBox\",\"label\":\"Enabled\",\"value\":true},"
                + "{\"id\":\"slider\",\"component\":\"Slider\",\"label\":\"Volume\",\"min\":0,\"max\":100,\"value\":25}]}}");

        int primary = Color.rgb(17, 34, 51);
        EditText input = find(view, EditText.class);
        CheckBox check = find(view, CheckBox.class);
        SeekBar slider = find(view, SeekBar.class);
        assertEquals(primary, input.getBackgroundTintList().getColorForState(
                new int[]{android.R.attr.state_focused}, Color.BLACK));
        assertEquals(primary, check.getButtonTintList().getColorForState(
                new int[]{android.R.attr.state_checked}, Color.BLACK));
        assertEquals(primary, slider.getProgressTintList().getDefaultColor());
        assertEquals(primary, slider.getThumbTintList().getDefaultColor());
    }

    @Test
    public void componentStylerUsesIdThenTypeThenGlobal() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.setGlobalComponentStyler((target, component, context) -> target.setTag("global"));
        view.registerComponentTypeStyler("Button",
                (target, component, context) -> target.setTag("type"));
        view.registerComponentStyler("main", "private",
                (target, component, context) -> target.setTag("private"));
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Column\",\"children\":[\"private\",\"typed\",\"global\"]},"
                + "{\"id\":\"private\",\"component\":\"Button\",\"child\":\"privateLabel\"},"
                + "{\"id\":\"privateLabel\",\"component\":\"Text\",\"text\":\"Private\"},"
                + "{\"id\":\"typed\",\"component\":\"Button\",\"child\":\"typedLabel\"},"
                + "{\"id\":\"typedLabel\",\"component\":\"Text\",\"text\":\"Typed\"},"
                + "{\"id\":\"global\",\"component\":\"Text\",\"text\":\"Global\"}]}}");

        assertNotNull(findWithTag(view, "private"));
        assertNotNull(findWithTag(view, "type"));
        assertNotNull(findWithTag(view, "global"));
    }

    @Test
    public void deleteSurfaceRemovesNativeView() {
        A2uiView view = new A2uiView(RuntimeEnvironment.getApplication());
        view.render("{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\"basic\"}}"
                + "\n{\"version\":\"v0.9.1\",\"deleteSurface\":{\"surfaceId\":\"main\"}}");
        assertEquals(0, view.getChildCount() > 0 ? ((ViewGroup) view.getChildAt(0)).getChildCount() : -1);
    }

    private static boolean hasText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) {
            return true;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasText(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T extends View> T find(View root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T result = find(group.getChildAt(i), type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static View findWithTag(View root, Object tag) {
        if (tag.equals(root.getTag())) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = findWithTag(group.getChildAt(i), tag);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void click(View root) {
        if (root.isClickable() && root.hasOnClickListeners()) {
            root.performClick();
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                click(group.getChildAt(i));
            }
        }
    }
}
