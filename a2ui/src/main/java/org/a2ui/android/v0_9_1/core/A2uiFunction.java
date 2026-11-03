package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonElement;

import java.util.Map;

public interface A2uiFunction {
    JsonElement invoke(Map<String, JsonElement> args);
}
