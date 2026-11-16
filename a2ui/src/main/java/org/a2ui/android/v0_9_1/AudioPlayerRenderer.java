package org.a2ui.android.v0_9_1;

import android.view.View;

import com.google.gson.JsonObject;

final class AudioPlayerRenderer implements A2uiComponentRenderer {

    @Override
    public View render(A2uiRenderContext context, JsonObject component) {
        return MediaSupport.render(context, component, true);
    }
}
