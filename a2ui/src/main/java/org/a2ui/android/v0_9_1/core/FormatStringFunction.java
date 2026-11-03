package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class FormatStringFunction extends A2uiFunctionRegistry.ContextualFunction {
    @Override
    JsonElement invoke(SurfaceState surface, String scope, Map<String, JsonElement> args) {
        JsonElement value = args.get("value");
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw new A2uiException("formatString requires a string value");
        }
        String template = value.getAsString();
        if (surface == null) {
            return new JsonPrimitive(template);
        }
        return new JsonPrimitive(interpolate(template, surface, scope));
    }

    private static String interpolate(String template, SurfaceState surface, String scope) {
        StringBuilder result = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 2 < template.length()
                    && template.charAt(i + 1) == '$' && template.charAt(i + 2) == '{') {
                result.append("${");
                i += 3;
                continue;
            }
            if (c == '$' && i + 1 < template.length() && template.charAt(i + 1) == '{') {
                int end = expressionEnd(template, i + 2);
                if (end < 0) {
                    throw new A2uiException("Unclosed formatString expression");
                }
                String expression = template.substring(i + 2, end);
                JsonElement evaluated = evaluate(expression, surface, scope);
                String string = SurfaceState.stringValue(evaluated);
                if (string != null) {
                    result.append(string);
                }
                i = end + 1;
                continue;
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    private static JsonElement evaluate(String source, SurfaceState surface, String scope) {
        String expression = source.trim();
        int open = expression.indexOf('(');
        if (open > 0 && expression.endsWith(")") && validFunctionName(expression.substring(0, open))) {
            int close = closingParenthesis(expression, open);
            if (close != expression.length() - 1) {
                throw new A2uiException("Invalid function expression: " + expression);
            }
            String name = expression.substring(0, open);
            JsonObject call = new JsonObject();
            call.addProperty("call", name);
            JsonObject args = new JsonObject();
            String body = expression.substring(open + 1, close);
            if (!body.trim().isEmpty()) {
                for (String argument : splitTopLevel(body, ',')) {
                    int colon = topLevelDelimiter(argument, ':');
                    if (colon <= 0) {
                        throw new A2uiException("Function arguments must be named: " + argument);
                    }
                    String argumentName = argument.substring(0, colon).trim();
                    if (argumentName.isEmpty()) {
                        throw new A2uiException("Function argument name must not be empty");
                    }
                    args.add(argumentName, parseValue(argument.substring(colon + 1).trim(),
                            surface, scope));
                }
            }
            call.add("args", args);
            return surface.callFunction(call, scope);
        }
        return surface.getValue(expression, expression.startsWith("/") ? null : scope);
    }

    private static JsonElement parseValue(String value, SurfaceState surface, String scope) {
        if (value.isEmpty()) {
            return new JsonPrimitive("");
        }
        char first = value.charAt(0);
        if ((first == '\'' || first == '"') && value.length() >= 2
                && value.charAt(value.length() - 1) == first) {
            return new JsonPrimitive(unquote(value));
        }
        if (value.startsWith("${")) {
            int end = expressionEnd(value, 2);
            if (end != value.length() - 1) {
                throw new A2uiException("Invalid nested expression: " + value);
            }
            return evaluate(value.substring(2, end), surface, scope);
        }
        int open = value.indexOf('(');
        if (open > 0 && value.endsWith(")") && validFunctionName(value.substring(0, open))) {
            return evaluate(value, surface, scope);
        }
        if ("true".equals(value)) return new JsonPrimitive(true);
        if ("false".equals(value)) return new JsonPrimitive(false);
        if ("null".equals(value)) return JsonNull.INSTANCE;
        if (first == '[' || first == '{' || looksLikeNumber(value)) {
            try {
                return JsonParser.parseString(value);
            } catch (JsonParseException e) {
                throw new A2uiException("Invalid function argument: " + value, e);
            }
        }
        return new JsonPrimitive(value);
    }

    private static int expressionEnd(String value, int start) {
        int parentheses = 0;
        int brackets = 0;
        int braces = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                if (parentheses > 0) parentheses--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                if (brackets > 0) brackets--;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                if (braces > 0) {
                    braces--;
                } else if (parentheses == 0 && brackets == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int closingParenthesis(String value, int open) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = open; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        throw new A2uiException("Unclosed function expression: " + value);
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        int parentheses = 0;
        int brackets = 0;
        int braces = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == delimiter && parentheses == 0 && brackets == 0 && braces == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
            if (parentheses < 0 || brackets < 0 || braces < 0) {
                throw new A2uiException("Unbalanced function arguments: " + value);
            }
        }
        if (quote != 0 || parentheses != 0 || brackets != 0 || braces != 0) {
            throw new A2uiException("Unbalanced function arguments: " + value);
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static int topLevelDelimiter(String value, char delimiter) {
        List<String> parts = splitTopLevel(value, delimiter);
        if (parts.size() < 2) {
            return -1;
        }
        return parts.get(0).length();
    }

    private static String unquote(String value) {
        char quote = value.charAt(0);
        StringBuilder result = new StringBuilder(value.length() - 2);
        for (int i = 1; i < value.length() - 1; i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                result.append(c);
                continue;
            }
            if (++i >= value.length() - 1) {
                throw new A2uiException("Invalid quoted function argument: " + value);
            }
            char escaped = value.charAt(i);
            switch (escaped) {
                case 'n': result.append('\n'); break;
                case 'r': result.append('\r'); break;
                case 't': result.append('\t'); break;
                case 'b': result.append('\b'); break;
                case 'f': result.append('\f'); break;
                case 'u':
                    if (i + 4 >= value.length()) {
                        throw new A2uiException("Invalid Unicode escape: " + value);
                    }
                    try {
                        result.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    } catch (NumberFormatException e) {
                        throw new A2uiException("Invalid Unicode escape: " + value, e);
                    }
                    i += 4;
                    break;
                default: result.append(escaped); break;
            }
        }
        return result.toString();
    }

    private static boolean validFunctionName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || !trimmed.equals(name)) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_') || (i == 0 && Character.isDigit(c))) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeNumber(String value) {
        return value.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    }
}
