package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class A2uiAction {
    private final String name;
    private final String surfaceId;
    private final String sourceComponentId;
    private final String timestamp;
    private final JsonObject context;
    private final JsonObject dataModel;

    private A2uiAction(String name, String surfaceId, String sourceComponentId, String timestamp,
                       JsonObject context, JsonObject dataModel) {
        this.name = name;
        this.surfaceId = surfaceId;
        this.sourceComponentId = sourceComponentId;
        this.timestamp = timestamp;
        this.context = context;
        this.dataModel = dataModel;
    }

    public static A2uiAction fromEvent(SurfaceState surface, String sourceComponentId,
                                       JsonObject action, String scope) {
        if (surface == null) {
            throw new A2uiException("Action surface must not be null");
        }
        if (sourceComponentId == null) {
            throw new A2uiException("Action sourceComponentId must not be null");
        }
        if (action == null || !action.has("event") || !action.get("event").isJsonObject()) {
            throw new A2uiException("Action requires an event object");
        }
        JsonObject event = action.getAsJsonObject("event");
        JsonElement nameElement = event.get("name");
        if (nameElement == null || !nameElement.isJsonPrimitive()
                || !nameElement.getAsJsonPrimitive().isString()) {
            throw new A2uiException("Action event requires a string name");
        }
        JsonObject context = new JsonObject();
        if (event.has("context")) {
            JsonElement contextElement = event.get("context");
            if (!contextElement.isJsonObject()) {
                throw new A2uiException("Action event context must be a JSON object");
            }
            for (Map.Entry<String, JsonElement> entry : contextElement.getAsJsonObject().entrySet()) {
                JsonElement resolved = surface.resolve(entry.getValue(), scope);
                context.add(entry.getKey(), resolved == null ? JsonNull.INSTANCE : resolved.deepCopy());
            }
        }
        JsonObject dataModel = surface.getSendDataModel() ? surface.getDataModel().deepCopy() : null;
        return new A2uiAction(nameElement.getAsString(), surface.getSurfaceId(), sourceComponentId,
                isoNow(), context, dataModel);
    }

    public String getName() {
        return name;
    }

    public String getSurfaceId() {
        return surfaceId;
    }

    public String getSourceComponentId() {
        return sourceComponentId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public JsonObject getContext() {
        return context.deepCopy();
    }

    public JsonObject getDataModel() {
        return dataModel == null ? null : dataModel.deepCopy();
    }

    private static String isoNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    public JsonObject toJson() {
        JsonObject action = new JsonObject();
        action.addProperty("name", name);
        action.addProperty("surfaceId", surfaceId);
        action.addProperty("sourceComponentId", sourceComponentId);
        action.addProperty("timestamp", timestamp);
        action.add("context", context.deepCopy());
        JsonObject envelope = new JsonObject();
        envelope.addProperty("version", "v0.9.1");
        envelope.add("action", action);
        return envelope;
    }
}
