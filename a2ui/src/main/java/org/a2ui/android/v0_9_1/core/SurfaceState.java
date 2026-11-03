package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SurfaceState {
    private final String surfaceId;
    private final String catalogId;
    private final JsonObject theme;
    private final boolean sendDataModel;
    private final JsonObject dataModel = new JsonObject();
    private final Map<String, JsonObject> components = new LinkedHashMap<String, JsonObject>();
    private final A2uiFunctionRegistry functionRegistry;

    SurfaceState(String surfaceId, String catalogId, JsonObject theme, boolean sendDataModel,
                 A2uiFunctionRegistry functionRegistry) {
        this.surfaceId = surfaceId;
        this.catalogId = catalogId;
        this.theme = theme == null ? null : theme.deepCopy();
        this.sendDataModel = sendDataModel;
        this.functionRegistry = functionRegistry;
    }

    public String getSurfaceId() {
        return surfaceId;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public JsonObject getTheme() {
        return theme;
    }

    public boolean getSendDataModel() {
        return sendDataModel;
    }

    public JsonObject getDataModel() {
        return dataModel;
    }

    public Map<String, JsonObject> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    public JsonObject getComponent(String id) {
        return components.get(id);
    }

    public JsonElement resolve(JsonElement value, String scope) {
        if (value == null) {
            return null;
        }
        if (!value.isJsonObject()) {
            return value.deepCopy();
        }
        JsonObject object = value.getAsJsonObject();
        boolean binding = object.has("path");
        boolean function = object.has("call");
        if (binding && function) {
            throw new A2uiException("A dynamic value cannot contain both path and call");
        }
        if (binding) {
            JsonElement path = object.get("path");
            if (!path.isJsonPrimitive() || !path.getAsJsonPrimitive().isString()) {
                throw new A2uiException("Data binding path must be a string");
            }
            return getValue(path.getAsString(), scope);
        }
        if (function) {
            return callFunction(object, scope);
        }
        return object.deepCopy();
    }

    public String resolveString(JsonElement value, String scope) {
        return stringValue(resolve(value, scope));
    }

    public boolean resolveBoolean(JsonElement value, String scope) {
        JsonElement resolved = resolve(value, scope);
        if (resolved == null || resolved.isJsonNull() || !resolved.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = resolved.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsDouble() != 0.0d;
        }
        return Boolean.parseBoolean(primitive.getAsString());
    }

    public double resolveDouble(JsonElement value, String scope) {
        JsonElement resolved = resolve(value, scope);
        if (resolved == null || resolved.isJsonNull()) {
            return 0.0d;
        }
        if (!resolved.isJsonPrimitive()) {
            throw new A2uiException("Expected a numeric dynamic value");
        }
        try {
            return resolved.getAsDouble();
        } catch (NumberFormatException e) {
            throw new A2uiException("Expected a numeric dynamic value", e);
        }
    }

    public List<String> resolveStringList(JsonElement value, String scope) {
        JsonElement resolved = resolve(value, scope);
        List<String> result = new ArrayList<String>();
        if (resolved == null || resolved.isJsonNull()) {
            return result;
        }
        if (!resolved.isJsonArray()) {
            throw new A2uiException("Expected a dynamic string list");
        }
        for (JsonElement item : resolved.getAsJsonArray()) {
            String string = resolveString(item, scope);
            if (string != null) {
                result.add(string);
            }
        }
        return result;
    }

    public JsonElement getValue(String path, String scope) {
        return JsonPointer.get(dataModel, absolutePath(path, scope));
    }

    public void setLocalValue(String path, JsonElement value, String scope) {
        JsonPointer.set(dataModel, absolutePath(path, scope), value);
    }

    public JsonElement callFunction(JsonObject functionCall, String scope) {
        if (functionCall == null) {
            throw new A2uiException("Function call must not be null");
        }
        JsonElement call = functionCall.get("call");
        if (call == null || !call.isJsonPrimitive() || !call.getAsJsonPrimitive().isString()) {
            throw new A2uiException("Function call requires a string call field");
        }
        JsonObject rawArgs = new JsonObject();
        if (functionCall.has("args")) {
            JsonElement args = functionCall.get("args");
            if (args == null || !args.isJsonObject()) {
                throw new A2uiException("Function args must be a JSON object");
            }
            rawArgs = args.getAsJsonObject();
        }
        Map<String, JsonElement> resolvedArgs = new LinkedHashMap<String, JsonElement>();
        for (Map.Entry<String, JsonElement> entry : rawArgs.entrySet()) {
            JsonElement resolved = resolveArgument(entry.getValue(), scope);
            resolvedArgs.put(entry.getKey(), resolved == null ? JsonNull.INSTANCE : resolved);
        }
        return functionRegistry.call(call.getAsString(), resolvedArgs, this, scope);
    }

    void updateComponents(JsonArray updates) {
        for (JsonElement element : updates) {
            JsonObject component = element.getAsJsonObject();
            components.put(component.get("id").getAsString(), component.deepCopy());
        }
    }

    void updateDataModel(String path, boolean hasValue, JsonElement value) {
        boolean rootUpdate = path == null || path.isEmpty() || "/".equals(path);
        String pointer = rootUpdate ? "" : path;
        if (hasValue) {
            JsonPointer.set(dataModel, pointer, value);
        } else {
            JsonPointer.remove(dataModel, pointer);
        }
    }

    private JsonElement resolveArgument(JsonElement value, String scope) {
        if (value == null) {
            return null;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement item : value.getAsJsonArray()) {
                JsonElement resolved = resolveArgument(item, scope);
                result.add(resolved == null ? JsonNull.INSTANCE : resolved);
            }
            return result;
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("path") || object.has("call")) {
                return resolve(object, scope);
            }
            JsonObject result = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement resolved = resolveArgument(entry.getValue(), scope);
                result.add(entry.getKey(), resolved == null ? JsonNull.INSTANCE : resolved);
            }
            return result;
        }
        return value.deepCopy();
    }

    private static String absolutePath(String path, String scope) {
        if (path == null) {
            throw new A2uiException("Data binding path must not be null");
        }
        if (scope != null && !scope.isEmpty()) {
            JsonPointer.validate(scope);
            if (path.isEmpty()) {
                return scope;
            }
            String relative = path.startsWith("/") ? path.substring(1) : path;
            return scope + "/" + relative;
        }
        if (path.isEmpty() || path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }

    static String stringValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                if (number instanceof BigDecimal) {
                    return ((BigDecimal) number).toPlainString();
                }
                return number.toString();
            }
            return primitive.getAsString();
        }
        return value.toString();
    }
}
