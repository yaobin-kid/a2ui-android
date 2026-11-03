package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class A2uiEngine {
    public interface Listener {
        void onSurfaceChanged(String surfaceId);

        void onSurfaceDeleted(String surfaceId);
    }

    private static final String VERSION = "v0.9.1";
    private static final String[] OPERATIONS = {
            "createSurface", "updateComponents", "updateDataModel", "deleteSurface"
    };

    private final Listener listener;
    private final A2uiFunctionRegistry functionRegistry = new A2uiFunctionRegistry();
    private final Map<String, SurfaceState> surfaces = new LinkedHashMap<String, SurfaceState>();

    public A2uiEngine(Listener listener) {
        this.listener = listener;
    }

    public void accept(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new A2uiException("A2UI input must not be empty");
        }
        final JsonElement parsed;
        try {
            parsed = JsonParser.parseString(input);
        } catch (JsonParseException e) {
            acceptJsonLines(input, e);
            return;
        }
        if (parsed.isJsonObject()) {
            accept(parsed.getAsJsonObject());
        } else if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    throw new A2uiException("Every A2UI array item must be a JSON object");
                }
                accept(element.getAsJsonObject());
            }
        } else {
            throw new A2uiException("A2UI input must be a JSON object, array, or JSONL");
        }
    }

    public void accept(JsonObject envelope) {
        if (envelope == null) {
            throw new A2uiException("A2UI envelope must not be null");
        }
        requireVersion(envelope);
        String operation = null;
        for (String candidate : OPERATIONS) {
            if (envelope.has(candidate)) {
                if (operation != null) {
                    throw new A2uiException("An A2UI envelope must contain exactly one operation");
                }
                operation = candidate;
            }
        }
        if (operation == null) {
            throw new A2uiException("A2UI envelope does not contain an operation");
        }
        Set<String> allowed = new HashSet<String>(Arrays.asList("version", operation));
        requireOnlyFields(envelope, allowed, "envelope");
        JsonElement payloadElement = envelope.get(operation);
        if (!payloadElement.isJsonObject()) {
            throw new A2uiException(operation + " must be a JSON object");
        }
        JsonObject payload = payloadElement.getAsJsonObject();
        if ("createSurface".equals(operation)) {
            createSurface(payload);
        } else if ("updateComponents".equals(operation)) {
            updateComponents(payload);
        } else if ("updateDataModel".equals(operation)) {
            updateDataModel(payload);
        } else {
            deleteSurface(payload);
        }
    }

    public SurfaceState getSurface(String surfaceId) {
        return surfaces.get(surfaceId);
    }

    public Map<String, SurfaceState> getSurfaces() {
        return Collections.unmodifiableMap(surfaces);
    }

    public A2uiFunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }

    private void createSurface(JsonObject payload) {
        requireOnlyFields(payload, setOf("surfaceId", "catalogId", "theme", "sendDataModel"),
                "createSurface");
        String surfaceId = requiredString(payload, "surfaceId", "createSurface");
        String catalogId = requiredString(payload, "catalogId", "createSurface");
        if (surfaces.containsKey(surfaceId)) {
            throw new A2uiException("Surface already exists: " + surfaceId);
        }
        JsonObject theme = null;
        if (payload.has("theme")) {
            JsonElement value = payload.get("theme");
            if (!value.isJsonObject()) {
                throw new A2uiException("createSurface.theme must be a JSON object");
            }
            theme = value.getAsJsonObject();
        }
        boolean sendDataModel = false;
        if (payload.has("sendDataModel")) {
            JsonElement value = payload.get("sendDataModel");
            if (!isBoolean(value)) {
                throw new A2uiException("createSurface.sendDataModel must be a boolean");
            }
            sendDataModel = value.getAsBoolean();
        }
        surfaces.put(surfaceId, new SurfaceState(surfaceId, catalogId, theme, sendDataModel,
                functionRegistry));
        notifyChanged(surfaceId);
    }

    private void updateComponents(JsonObject payload) {
        requireOnlyFields(payload, setOf("surfaceId", "components"), "updateComponents");
        String surfaceId = requiredString(payload, "surfaceId", "updateComponents");
        SurfaceState surface = requireSurface(surfaceId);
        JsonElement componentsElement = payload.get("components");
        if (componentsElement == null || !componentsElement.isJsonArray()
                || componentsElement.getAsJsonArray().size() == 0) {
            throw new A2uiException("updateComponents.components must be a non-empty array");
        }
        JsonArray components = componentsElement.getAsJsonArray();
        for (JsonElement element : components) {
            if (!element.isJsonObject()) {
                throw new A2uiException("Every component must be a JSON object");
            }
            JsonObject component = element.getAsJsonObject();
            requiredString(component, "id", "component");
            requiredString(component, "component", "component");
        }
        surface.updateComponents(components);
        notifyChanged(surfaceId);
    }

    private void updateDataModel(JsonObject payload) {
        requireOnlyFields(payload, setOf("surfaceId", "path", "value"), "updateDataModel");
        String surfaceId = requiredString(payload, "surfaceId", "updateDataModel");
        SurfaceState surface = requireSurface(surfaceId);
        String path = null;
        if (payload.has("path")) {
            JsonElement pathElement = payload.get("path");
            if (!pathElement.isJsonPrimitive() || !pathElement.getAsJsonPrimitive().isString()) {
                throw new A2uiException("updateDataModel.path must be a string");
            }
            path = pathElement.getAsString();
        }
        boolean hasValue = payload.has("value");
        surface.updateDataModel(path, hasValue, payload.get("value"));
        notifyChanged(surfaceId);
    }

    private void deleteSurface(JsonObject payload) {
        requireOnlyFields(payload, setOf("surfaceId"), "deleteSurface");
        String surfaceId = requiredString(payload, "surfaceId", "deleteSurface");
        requireSurface(surfaceId);
        surfaces.remove(surfaceId);
        if (listener != null) {
            listener.onSurfaceDeleted(surfaceId);
        }
    }

    private SurfaceState requireSurface(String surfaceId) {
        SurfaceState surface = surfaces.get(surfaceId);
        if (surface == null) {
            throw new A2uiException("Unknown surface: " + surfaceId);
        }
        return surface;
    }

    private void notifyChanged(String surfaceId) {
        if (listener != null) {
            listener.onSurfaceChanged(surfaceId);
        }
    }

    private void acceptJsonLines(String input, JsonParseException originalError) {
        String[] lines = input.split("\\r?\\n");
        boolean accepted = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final JsonElement element;
            try {
                element = JsonParser.parseString(trimmed);
            } catch (JsonParseException e) {
                if (lines.length == 1) {
                    throw new A2uiException("Invalid A2UI JSON", originalError);
                }
                throw new A2uiException("Invalid A2UI JSONL line", e);
            }
            if (!element.isJsonObject()) {
                throw new A2uiException("Every JSONL line must contain an A2UI envelope object");
            }
            accept(element.getAsJsonObject());
            accepted = true;
        }
        if (!accepted) {
            throw new A2uiException("A2UI input must not be empty", originalError);
        }
    }

    private static void requireVersion(JsonObject envelope) {
        JsonElement version = envelope.get("version");
        if (version == null || !version.isJsonPrimitive()
                || !version.getAsJsonPrimitive().isString()
                || !VERSION.equals(version.getAsString())) {
            throw new A2uiException("A2UI envelope version must be v0.9.1");
        }
    }

    private static String requiredString(JsonObject object, String field, String context) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new A2uiException(context + " requires a string " + field);
        }
        return value.getAsString();
    }

    private static boolean isBoolean(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

    private static void requireOnlyFields(JsonObject object, Set<String> allowed, String context) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                throw new A2uiException("Unknown field in " + context + ": " + entry.getKey());
            }
        }
    }
}
