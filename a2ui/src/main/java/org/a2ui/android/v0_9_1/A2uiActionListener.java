package org.a2ui.android.v0_9_1;

import org.a2ui.android.v0_9_1.core.A2uiAction;

/** 接收用户触发的 A2UI 服务端事件。 */
public interface A2uiActionListener {
    void onAction(A2uiAction action);
}
