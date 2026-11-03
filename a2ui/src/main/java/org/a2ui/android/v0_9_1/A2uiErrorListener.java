package org.a2ui.android.v0_9_1;

/** 接收不影响整个 Surface 的局部渲染错误。 */
public interface A2uiErrorListener {
    void onError(String surfaceId, String componentId, String message);
}
