package org.a2ui.android.v0_9_1;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class IconRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        JsonElement resolved = RenderSupport.resolve(context, component.get("name"));
        if (resolved != null && resolved.isJsonObject()) {
            context.reportError(RenderSupport.optString(component, "id", ""),
                    "默认 Icon 渲染器不支持 svgPath，请注册自定义渲染器");
            return RenderSupport.errorView(context, "SVG icon");
        }
        String name = resolved == null || resolved.isJsonNull() ? "help" : resolved.getAsString();
        ImageView view = new ImageView(context.getContext());
        view.setImageResource(iconResource(name));
        view.setColorFilter(RenderSupport.primaryColor(context));
        view.setContentDescription(name);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                RenderSupport.dp(context, 24), RenderSupport.dp(context, 24)));
        return view;
    }

    private static int iconResource(String name) {
        if (name == null) {
            return android.R.drawable.ic_menu_help;
        }
        switch (name) {
            case "add": return android.R.drawable.ic_menu_add;
            case "arrowBack": return android.R.drawable.ic_media_rew;
            case "arrowForward": return android.R.drawable.ic_media_ff;
            case "call":
            case "phone": return android.R.drawable.ic_menu_call;
            case "camera": return android.R.drawable.ic_menu_camera;
            case "check": return android.R.drawable.checkbox_on_background;
            case "close": return android.R.drawable.ic_menu_close_clear_cancel;
            case "delete": return android.R.drawable.ic_menu_delete;
            case "download": return android.R.drawable.stat_sys_download;
            case "upload": return android.R.drawable.ic_menu_upload;
            case "edit": return android.R.drawable.ic_menu_edit;
            case "event":
            case "calendarToday": return android.R.drawable.ic_menu_month;
            case "error":
            case "warning": return android.R.drawable.ic_dialog_alert;
            case "fastForward": return android.R.drawable.ic_media_ff;
            case "favorite":
            case "star": return android.R.drawable.star_big_on;
            case "favoriteOff":
            case "starOff": return android.R.drawable.star_big_off;
            case "starHalf": return android.R.drawable.star_on;
            case "help": return android.R.drawable.ic_menu_help;
            case "home": return android.R.drawable.ic_menu_myplaces;
            case "info": return android.R.drawable.ic_dialog_info;
            case "locationOn": return android.R.drawable.ic_menu_mylocation;
            case "lock":
            case "lockOpen": return android.R.drawable.ic_lock_lock;
            case "mail": return android.R.drawable.ic_dialog_email;
            case "menu":
            case "moreVert":
            case "moreHoriz": return android.R.drawable.ic_menu_more;
            case "notifications":
            case "notificationsOff": return android.R.drawable.ic_menu_info_details;
            case "pause": return android.R.drawable.ic_media_pause;
            case "payment": return android.R.drawable.ic_menu_directions;
            case "person":
            case "accountCircle": return android.R.drawable.ic_menu_myplaces;
            case "photo": return android.R.drawable.ic_menu_gallery;
            case "play": return android.R.drawable.ic_media_play;
            case "refresh": return android.R.drawable.ic_menu_rotate;
            case "rewind": return android.R.drawable.ic_media_rew;
            case "search": return android.R.drawable.ic_menu_search;
            case "send": return android.R.drawable.ic_menu_send;
            case "settings": return android.R.drawable.ic_menu_preferences;
            case "share": return android.R.drawable.ic_menu_share;
            case "shoppingCart": return android.R.drawable.ic_menu_agenda;
            case "skipNext": return android.R.drawable.ic_media_next;
            case "skipPrevious": return android.R.drawable.ic_media_previous;
            case "stop": return android.R.drawable.ic_media_pause;
            case "visibility":
            case "visibilityOff": return android.R.drawable.ic_menu_view;
            case "volumeDown":
            case "volumeMute":
            case "volumeOff":
            case "volumeUp": return android.R.drawable.ic_lock_silent_mode_off;
            default: return android.R.drawable.ic_menu_help;
        }
    }
}
