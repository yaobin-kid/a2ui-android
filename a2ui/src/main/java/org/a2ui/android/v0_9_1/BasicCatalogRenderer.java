package org.a2ui.android.v0_9_1;

import android.view.View;

import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.core.A2uiAction;
import org.a2ui.android.v0_9_1.core.A2uiException;

import java.util.HashMap;
import java.util.Map;

/**
 * basic catalog 的渲染调度器：只负责按组件类型分派到独立渲染器，
 * 以及统一的 style / styler / accessibility / 错误处理流程。
 */
final class BasicCatalogRenderer {
    private final A2uiView host;
    private final Map<String, A2uiComponentRenderer> builtins = new HashMap<>();

    BasicCatalogRenderer(A2uiView host) {
        this.host = host;
        registerBuiltins();
    }

    private void registerBuiltins() {
        builtins.put("Text", new TextRenderer());
        builtins.put("Image", new ImageRenderer());
        builtins.put("Icon", new IconRenderer());
        builtins.put("Video", new VideoRenderer());
        builtins.put("AudioPlayer", new AudioPlayerRenderer());
        builtins.put("Row", new LayoutRenderer());
        builtins.put("Column", new LayoutRenderer());
        builtins.put("List", new ListRenderer());
        builtins.put("Card", new CardRenderer());
        builtins.put("Tabs", new TabsRenderer());
        builtins.put("Modal", new ModalRenderer());
        builtins.put("Divider", new DividerRenderer());
        builtins.put("Button", new ButtonRenderer());
        builtins.put("TextField", new TextFieldRenderer());
        builtins.put("CheckBox", new CheckBoxRenderer());
        builtins.put("ChoicePicker", new ChoicePickerRenderer());
        builtins.put("Slider", new SliderRenderer());
        builtins.put("DateTimeInput", new DateTimeInputRenderer());
    }

    View render(String componentId, A2uiRenderContext context) {
        JsonObject component = context.getSurface().getComponent(componentId);
        if (component == null) {
            return RenderSupport.placeholder(context, componentId);
        }
        String type = RenderSupport.optString(component, "component", "");
        try {
            A2uiComponentRenderer custom = host.getCustomRenderer(type);
            A2uiComponentRenderer renderer = custom != null ? custom : builtins.get(type);
            View view;
            if (renderer != null) {
                view = renderer.render(context, component);
            } else {
                context.reportError(componentId, "不支持的组件类型: " + type);
                view = RenderSupport.errorView(context, "Unsupported component: " + type);
            }
            if (view == null) {
                throw new A2uiException("组件渲染器返回 null: " + type);
            }
            ComponentStyleApplier.apply(context, component, view);
            A2uiComponentStyler styler = host.getComponentStyler(
                    context.getSurface().getSurfaceId(), componentId, type);
            if (styler != null) {
                styler.apply(view, component, context);
            }
            applyAccessibility(context, component, view);
            return view;
        } catch (Exception error) {
            context.reportError(componentId, error.getMessage() == null ? error.toString() : error.getMessage());
            return RenderSupport.errorView(context, "Render error: " + componentId);
        }
    }

    void dispatchAction(A2uiRenderContext context, JsonObject action, String sourceComponentId) {
        if (action == null) {
            return;
        }
        try {
            if (action.has("event")) {
                host.dispatchAction(A2uiAction.fromEvent(
                        context.getSurface(), sourceComponentId, action, context.getScope()));
            } else if (action.has("functionCall")) {
                context.getSurface().callFunction(
                        action.getAsJsonObject("functionCall"), context.getScope());
            }
        } catch (Exception error) {
            context.reportError(sourceComponentId, "Action 执行失败: " + error.getMessage());
        }
    }

    private void applyAccessibility(A2uiRenderContext context, JsonObject component, View view) {
        JsonObject accessibility = component.getAsJsonObject("accessibility");
        if (accessibility == null) {
            return;
        }
        String label = RenderSupport.string(context, accessibility.get("label"), "");
        String description = RenderSupport.string(context, accessibility.get("description"), "");
        String text = (label + (label.isEmpty() || description.isEmpty() ? "" : ". ") + description).trim();
        if (!text.isEmpty()) {
            view.setContentDescription(text);
        }
        if (android.os.Build.VERSION.SDK_INT >= 26 && !description.isEmpty()) {
            view.setTooltipText(description);
        }
    }
}
