package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class A2uiFunctionRegistry {
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private final Map<String, A2uiFunction> functions = new LinkedHashMap<String, A2uiFunction>();

    public A2uiFunctionRegistry() {
        register("required", args -> new JsonPrimitive(required(args.get("value"))));
        register("regex", args -> {
            String value = nullableString(args.get("value"));
            if (value == null) {
                return new JsonPrimitive(false);
            }
            String pattern = requiredString(args, "pattern");
            try {
                return new JsonPrimitive(Pattern.compile(pattern).matcher(value).matches());
            } catch (PatternSyntaxException e) {
                throw new A2uiException("Invalid regex pattern: " + pattern, e);
            }
        });
        register("length", args -> {
            String value = nullableString(args.get("value"));
            if (value == null) {
                return new JsonPrimitive(false);
            }
            if (!args.containsKey("min") && !args.containsKey("max")) {
                throw new A2uiException("length requires min or max");
            }
            int length = value.codePointCount(0, value.length());
            boolean valid = true;
            if (args.containsKey("min")) {
                int min = nonNegativeInt(args, "min");
                valid = length >= min;
            }
            if (args.containsKey("max")) {
                int max = nonNegativeInt(args, "max");
                valid = valid && length <= max;
            }
            return new JsonPrimitive(valid);
        });
        register("numeric", args -> {
            JsonElement element = args.get("value");
            if (element == null || element.isJsonNull()) {
                return new JsonPrimitive(false);
            }
            if (!args.containsKey("min") && !args.containsKey("max")) {
                throw new A2uiException("numeric requires min or max");
            }
            double value = number(element, "value");
            boolean valid = !Double.isNaN(value) && !Double.isInfinite(value);
            if (args.containsKey("min")) {
                valid = valid && value >= number(args.get("min"), "min");
            }
            if (args.containsKey("max")) {
                valid = valid && value <= number(args.get("max"), "max");
            }
            return new JsonPrimitive(valid);
        });
        register("email", args -> {
            String value = nullableString(args.get("value"));
            return new JsonPrimitive(value != null && EMAIL.matcher(value).matches());
        });
        register("formatString", new FormatStringFunction());
        register("formatNumber", args -> {
            JsonElement value = args.get("value");
            if (value == null || value.isJsonNull()) {
                return new JsonPrimitive("");
            }
            NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
            format.setGroupingUsed(optionalBoolean(args, "grouping", true));
            if (args.containsKey("decimals")) {
                int decimals = decimals(args);
                format.setMinimumFractionDigits(decimals);
                format.setMaximumFractionDigits(decimals);
            } else {
                format.setMinimumFractionDigits(0);
                format.setMaximumFractionDigits(2);
            }
            return new JsonPrimitive(format.format(number(value, "value")));
        });
        register("formatCurrency", args -> {
            JsonElement value = args.get("value");
            if (value == null || value.isJsonNull()) {
                return new JsonPrimitive("");
            }
            String currencyCode = requiredString(args, "currency").toUpperCase(Locale.ROOT);
            final Currency currency;
            try {
                currency = Currency.getInstance(currencyCode);
            } catch (IllegalArgumentException e) {
                throw new A2uiException("Invalid ISO 4217 currency: " + currencyCode, e);
            }
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            format.setCurrency(currency);
            format.setGroupingUsed(optionalBoolean(args, "grouping", true));
            if (args.containsKey("decimals")) {
                int decimals = decimals(args);
                format.setMinimumFractionDigits(decimals);
                format.setMaximumFractionDigits(decimals);
            }
            return new JsonPrimitive(format.format(number(value, "value")));
        });
        register("formatDate", args -> new JsonPrimitive(formatDate(args)));
        register("pluralize", args -> {
            String other = requiredString(args, "other");
            JsonElement value = args.get("value");
            if (value == null || value.isJsonNull()) {
                return new JsonPrimitive(other);
            }
            String category = pluralCategory(number(value, "value"), Locale.getDefault());
            String selected = nullableString(args.get(category));
            return new JsonPrimitive(selected == null ? other : selected);
        });
        register("openUrl", args -> {
            throw new A2uiException("openUrl is unavailable in the pure Java core; "
                    + "register an Android implementation");
        });
        register("and", args -> {
            JsonArray values = booleanValues(args);
            for (JsonElement value : values) {
                if (!booleanValue(value)) {
                    return new JsonPrimitive(false);
                }
            }
            return new JsonPrimitive(true);
        });
        register("or", args -> {
            JsonArray values = booleanValues(args);
            for (JsonElement value : values) {
                if (booleanValue(value)) {
                    return new JsonPrimitive(true);
                }
            }
            return new JsonPrimitive(false);
        });
        register("not", args -> new JsonPrimitive(!booleanValue(args.get("value"))));
    }

    public void register(String name, A2uiFunction function) {
        if (name == null || name.isEmpty()) {
            throw new A2uiException("Function name must not be empty");
        }
        if (function == null) {
            throw new A2uiException("Function must not be null");
        }
        functions.put(name, function);
    }

    public JsonElement call(String name, Map<String, JsonElement> args) {
        return call(name, args, null, null);
    }

    JsonElement call(String name, Map<String, JsonElement> args, SurfaceState surface, String scope) {
        A2uiFunction function = functions.get(name);
        if (function == null) {
            throw new A2uiException("Unknown A2UI function: " + name);
        }
        Map<String, JsonElement> actualArgs = args == null
                ? Collections.<String, JsonElement>emptyMap() : args;
        JsonElement result;
        if (function instanceof ContextualFunction) {
            result = ((ContextualFunction) function).invoke(surface, scope, actualArgs);
        } else {
            result = function.invoke(actualArgs);
        }
        return result == null ? JsonNull.INSTANCE : result;
    }

    abstract static class ContextualFunction implements A2uiFunction {
        @Override
        public JsonElement invoke(Map<String, JsonElement> args) {
            return invoke(null, null, args);
        }

        abstract JsonElement invoke(SurfaceState surface, String scope, Map<String, JsonElement> args);
    }

    private static boolean required(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        if (value.isJsonArray()) {
            return value.getAsJsonArray().size() > 0;
        }
        if (value.isJsonObject()) {
            return value.getAsJsonObject().entrySet().size() > 0;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        return !primitive.isString() || !primitive.getAsString().isEmpty();
    }

    private static String nullableString(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!value.isJsonPrimitive()) {
            throw new A2uiException("Expected a string argument");
        }
        return value.getAsString();
    }

    private static String requiredString(Map<String, JsonElement> args, String name) {
        String value = nullableString(args.get(name));
        if (value == null) {
            throw new A2uiException("Missing required function argument: " + name);
        }
        return value;
    }

    private static double number(JsonElement value, String name) {
        if (value == null || !value.isJsonPrimitive()) {
            throw new A2uiException("Expected numeric function argument: " + name);
        }
        try {
            return value.getAsDouble();
        } catch (NumberFormatException e) {
            throw new A2uiException("Expected numeric function argument: " + name, e);
        }
    }

    private static int nonNegativeInt(Map<String, JsonElement> args, String name) {
        double value = number(args.get(name), name);
        if (value < 0 || value != Math.rint(value) || value > Integer.MAX_VALUE) {
            throw new A2uiException("Expected a non-negative integer argument: " + name);
        }
        return (int) value;
    }

    private static int decimals(Map<String, JsonElement> args) {
        int decimals = nonNegativeInt(args, "decimals");
        if (decimals > 340) {
            throw new A2uiException("decimals must not exceed 340");
        }
        return decimals;
    }

    private static boolean optionalBoolean(Map<String, JsonElement> args, String name,
                                           boolean defaultValue) {
        return args.containsKey(name) ? booleanValue(args.get(name)) : defaultValue;
    }

    private static boolean booleanValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        if (!value.isJsonPrimitive()) {
            throw new A2uiException("Expected a boolean function argument");
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsDouble() != 0.0d;
        }
        return Boolean.parseBoolean(primitive.getAsString());
    }

    private static JsonArray booleanValues(Map<String, JsonElement> args) {
        JsonElement value = args.get("values");
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() < 2) {
            throw new A2uiException("Logical functions require at least two values");
        }
        return value.getAsJsonArray();
    }

    private static String formatDate(Map<String, JsonElement> args) {
        String pattern = requiredString(args, "format");
        JsonElement value = args.get("value");
        if (value == null || value.isJsonNull()) {
            return "";
        }
        try {
            Date date = value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                    ? new Date(value.getAsLong()) : parseDateTime(requiredString(args, "value"));
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
        } catch (ParseException | IllegalArgumentException e) {
            throw new A2uiException("Invalid date value or format", e);
        }
    }

    private static Date parseDateTime(String value) throws ParseException {
        String base = value.trim();
        TimeZone timeZone = TimeZone.getDefault();
        Matcher zone = Pattern.compile("(Z|[+-]\\d{2}:?\\d{2})$").matcher(base);
        if (zone.find()) {
            String offset = zone.group(1);
            timeZone = "Z".equals(offset) ? TimeZone.getTimeZone("UTC")
                    : TimeZone.getTimeZone("GMT" + offset.replace(":", ""));
            base = base.substring(0, zone.start());
        }
        base = base.replaceFirst("(\\.\\d{3})\\d+(?=$)", "$1");
        ParseException last = null;
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"}) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                format.setTimeZone(timeZone);
                return format.parse(base);
            } catch (ParseException error) {
                last = error;
            }
        }
        throw last == null ? new ParseException(value, 0) : last;
    }

    private static String pluralCategory(double value, Locale locale) {
        String language = locale.getLanguage();
        double absolute = Math.abs(value);
        boolean integer = !Double.isNaN(absolute) && !Double.isInfinite(absolute)
                && absolute == Math.rint(absolute);
        long n = integer && absolute <= Long.MAX_VALUE ? (long) absolute : 0L;

        if ("ar".equals(language)) {
            if (integer && n == 0) return "zero";
            if (integer && n == 1) return "one";
            if (integer && n == 2) return "two";
            long mod100 = n % 100;
            if (integer && mod100 >= 3 && mod100 <= 10) return "few";
            if (integer && mod100 >= 11 && mod100 <= 99) return "many";
            return "other";
        }
        if ("cy".equals(language)) {
            if (integer && n == 0) return "zero";
            if (integer && n == 1) return "one";
            if (integer && n == 2) return "two";
            if (integer && n == 3) return "few";
            if (integer && n == 6) return "many";
            return "other";
        }
        if ("ga".equals(language)) {
            if (integer && n == 1) return "one";
            if (integer && n == 2) return "two";
            if (integer && n >= 3 && n <= 6) return "few";
            if (integer && n >= 7 && n <= 10) return "many";
            return "other";
        }
        if ("ru".equals(language) || "uk".equals(language) || "be".equals(language)) {
            long mod10 = n % 10;
            long mod100 = n % 100;
            if (integer && mod10 == 1 && mod100 != 11) return "one";
            if (integer && mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "few";
            if (integer && (mod10 == 0 || (mod10 >= 5 && mod10 <= 9)
                    || (mod100 >= 11 && mod100 <= 14))) return "many";
            return "other";
        }
        if ("pl".equals(language)) {
            long mod10 = n % 10;
            long mod100 = n % 100;
            if (integer && n == 1) return "one";
            if (integer && mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "few";
            if (integer) return "many";
            return "other";
        }
        if ("cs".equals(language) || "sk".equals(language)) {
            if (integer && n == 1) return "one";
            if (integer && n >= 2 && n <= 4) return "few";
            if (!integer) return "many";
            return "other";
        }
        if ("sl".equals(language)) {
            long mod100 = n % 100;
            if (integer && mod100 == 1) return "one";
            if (integer && mod100 == 2) return "two";
            if (!integer || mod100 == 3 || mod100 == 4) return "few";
            return "other";
        }
        if ("ro".equals(language)) {
            if (integer && n == 1) return "one";
            long mod100 = n % 100;
            if (!integer || n == 0 || (mod100 >= 1 && mod100 <= 19)) return "few";
            return "other";
        }
        if ("fr".equals(language) || "pt".equals(language)) {
            return absolute >= 0 && absolute < 2 ? "one" : "other";
        }
        return integer && n == 1 ? "one" : "other";
    }
}
