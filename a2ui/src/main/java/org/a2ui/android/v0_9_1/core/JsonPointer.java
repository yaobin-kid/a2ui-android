package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Map;

final class JsonPointer {
    private JsonPointer() {
    }

    static JsonElement get(JsonElement root, String path) {
        String[] tokens = tokens(path);
        JsonElement current = root;
        for (String token : tokens) {
            if (current == null || current.isJsonNull()) {
                return null;
            }
            if (current.isJsonObject()) {
                JsonObject object = current.getAsJsonObject();
                if (!object.has(token)) {
                    return null;
                }
                current = object.get(token);
            } else if (current.isJsonArray()) {
                JsonArray array = current.getAsJsonArray();
                int index = arrayIndex(token);
                if (index >= array.size()) {
                    return null;
                }
                current = array.get(index);
            } else {
                return null;
            }
        }
        return current;
    }

    static void set(JsonObject root, String path, JsonElement value) {
        JsonElement newValue = value == null ? JsonNull.INSTANCE : value.deepCopy();
        String[] tokens = tokens(path);
        if (tokens.length == 0) {
            if (!newValue.isJsonObject()) {
                throw new A2uiException("The root data model must be a JSON object");
            }
            replaceRoot(root, newValue.getAsJsonObject());
            return;
        }

        JsonElement current = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if (current.isJsonObject()) {
                JsonObject object = current.getAsJsonObject();
                JsonElement child = object.get(token);
                if (child == null || child.isJsonNull()) {
                    child = new JsonObject();
                    object.add(token, child);
                } else if (!child.isJsonObject() && !child.isJsonArray()) {
                    throw new A2uiException("Cannot traverse a primitive at /" + token);
                }
                current = child;
            } else if (current.isJsonArray()) {
                JsonArray array = current.getAsJsonArray();
                int index = arrayIndex(token);
                if (index >= array.size()) {
                    throw new A2uiException("Array index out of bounds at " + path);
                }
                current = array.get(index);
                if (current == null || current.isJsonNull()
                        || (!current.isJsonObject() && !current.isJsonArray())) {
                    throw new A2uiException("Cannot traverse a non-container at " + path);
                }
            } else {
                throw new A2uiException("Cannot traverse a non-container at " + path);
            }
        }

        String last = tokens[tokens.length - 1];
        if (current.isJsonObject()) {
            current.getAsJsonObject().add(last, newValue);
        } else if (current.isJsonArray()) {
            JsonArray array = current.getAsJsonArray();
            if ("-".equals(last)) {
                array.add(newValue);
            } else {
                int index = arrayIndex(last);
                if (index == array.size()) {
                    array.add(newValue);
                } else if (index < array.size()) {
                    array.set(index, newValue);
                } else {
                    throw new A2uiException("Array index out of bounds: " + last);
                }
            }
        } else {
            throw new A2uiException("Cannot set a child of a non-container at " + path);
        }
    }

    static void remove(JsonObject root, String path) {
        String[] tokens = tokens(path);
        if (tokens.length == 0) {
            replaceRoot(root, new JsonObject());
            return;
        }

        JsonElement current = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if (current == null || current.isJsonNull()) {
                return;
            }
            if (current.isJsonObject()) {
                current = current.getAsJsonObject().get(token);
            } else if (current.isJsonArray()) {
                JsonArray array = current.getAsJsonArray();
                int index = arrayIndex(token);
                if (index >= array.size()) {
                    return;
                }
                current = array.get(index);
            } else {
                throw new A2uiException("Cannot traverse a primitive at " + path);
            }
        }

        if (current == null || current.isJsonNull()) {
            return;
        }
        String last = tokens[tokens.length - 1];
        if (current.isJsonObject()) {
            current.getAsJsonObject().remove(last);
        } else if (current.isJsonArray()) {
            JsonArray array = current.getAsJsonArray();
            int index = arrayIndex(last);
            if (index < array.size()) {
                array.remove(index);
            }
        } else {
            throw new A2uiException("Cannot remove a child of a primitive at " + path);
        }
    }

    static void validate(String path) {
        tokens(path);
    }

    private static String[] tokens(String path) {
        if (path == null) {
            throw new A2uiException("JSON Pointer path must not be null");
        }
        if (path.isEmpty()) {
            return new String[0];
        }
        if (!path.startsWith("/")) {
            throw new A2uiException("JSON Pointer must be empty or start with '/': " + path);
        }
        String[] rawTokens = path.substring(1).split("/", -1);
        for (int i = 0; i < rawTokens.length; i++) {
            rawTokens[i] = unescape(rawTokens[i]);
        }
        return rawTokens;
    }

    private static String unescape(String token) {
        StringBuilder result = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c != '~') {
                result.append(c);
                continue;
            }
            if (i + 1 >= token.length()) {
                throw new A2uiException("Invalid JSON Pointer escape in token: " + token);
            }
            char escaped = token.charAt(++i);
            if (escaped == '0') {
                result.append('~');
            } else if (escaped == '1') {
                result.append('/');
            } else {
                throw new A2uiException("Invalid JSON Pointer escape ~" + escaped);
            }
        }
        return result.toString();
    }

    private static int arrayIndex(String token) {
        if (token.isEmpty() || (token.length() > 1 && token.charAt(0) == '0')) {
            throw new A2uiException("Invalid array index: " + token);
        }
        int index = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c < '0' || c > '9') {
                throw new A2uiException("Invalid array index: " + token);
            }
            int digit = c - '0';
            if (index > (Integer.MAX_VALUE - digit) / 10) {
                throw new A2uiException("Array index is too large: " + token);
            }
            index = index * 10 + digit;
        }
        return index;
    }

    private static void replaceRoot(JsonObject target, JsonObject source) {
        JsonObject copy = source.deepCopy();
        for (Map.Entry<String, JsonElement> entry : copy.entrySet()) {
            target.add(entry.getKey(), entry.getValue());
        }
        for (String key : new java.util.ArrayList<String>(target.keySet())) {
            if (!copy.has(key)) {
                target.remove(key);
            }
        }
    }
}
