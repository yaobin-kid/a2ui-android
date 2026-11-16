package org.a2ui.android.v0_9_1;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.JsonObject;

/** Video 与 AudioPlayer 共用的媒体视图构建逻辑。 */
final class MediaSupport {
    private MediaSupport() {
    }

    static View render(A2uiRenderContext context, JsonObject component, boolean audio) {
        LinearLayout container = new LinearLayout(context.getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        if (audio) {
            TextView description = new TextView(context.getContext());
            description.setText(RenderSupport.string(context, component.get("description"), "Audio"));
            description.setPadding(RenderSupport.dp(context, 8), RenderSupport.dp(context, 8),
                    RenderSupport.dp(context, 8), 0);
            container.addView(description);
        }
        VideoView video = new VideoView(context.getContext());
        MediaController controller = new MediaController(context.getContext());
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setVideoURI(Uri.parse(RenderSupport.string(context, component.get("url"), "")));
        container.addView(video, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, RenderSupport.dp(context, audio ? 72 : 220)));
        return container;
    }
}
