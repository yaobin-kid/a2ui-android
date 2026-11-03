package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class A2uiFunctionRegistryTest {
    @Test
    public void validatesRequiredRegexLengthNumericEmailAndLogic() {
        A2uiFunctionRegistry registry = new A2uiFunctionRegistry();
        assertTrue(call(registry, "required", "value", "x").getAsBoolean());
        assertFalse(call(registry, "required", "value", "").getAsBoolean());
        assertFalse(call(registry, "required", "value", JsonNull.INSTANCE).getAsBoolean());

        Map<String, JsonElement> regex = args("value", "abc-123", "pattern", "[a-z]+-[0-9]+");
        assertTrue(registry.call("regex", regex).getAsBoolean());
        Map<String, JsonElement> length = args("value", "abcd", "min", 2, "max", 4);
        assertTrue(registry.call("length", length).getAsBoolean());
        Map<String, JsonElement> numeric = args("value", 5, "min", 1, "max", 5);
        assertTrue(registry.call("numeric", numeric).getAsBoolean());
        assertTrue(call(registry, "email", "value", "a@example.com").getAsBoolean());
        assertFalse(call(registry, "email", "value", "not-an-email").getAsBoolean());

        Map<String, JsonElement> logic = new HashMap<String, JsonElement>();
        JsonArray values = new JsonArray();
        values.add(true);
        values.add(false);
        logic.put("values", values);
        assertFalse(registry.call("and", logic).getAsBoolean());
        assertTrue(registry.call("or", logic).getAsBoolean());
        assertTrue(call(registry, "not", "value", false).getAsBoolean());
    }

    @Test
    public void formatsNumbersCurrencyDatesAndPlurals() {
        Locale oldLocale = Locale.getDefault();
        TimeZone oldZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            A2uiFunctionRegistry registry = new A2uiFunctionRegistry();
            Map<String, JsonElement> number = args("value", 1234.5, "decimals", 2, "grouping", true);
            assertEquals("1,234.50", registry.call("formatNumber", number).getAsString());
            number.put("grouping", new JsonPrimitive(false));
            assertEquals("1234.50", registry.call("formatNumber", number).getAsString());

            Map<String, JsonElement> currency = args("value", 1234.5, "currency", "USD");
            assertEquals("$1,234.50", registry.call("formatCurrency", currency).getAsString());
            Map<String, JsonElement> date = args("value", "2026-01-16T14:30:00Z",
                    "format", "yyyy-MM-dd HH:mm");
            assertEquals("2026-01-16 14:30", registry.call("formatDate", date).getAsString());

            Map<String, JsonElement> plural = args("value", 1, "one", "item", "other", "items");
            assertEquals("item", registry.call("pluralize", plural).getAsString());
            plural.put("value", new JsonPrimitive(2));
            assertEquals("items", registry.call("pluralize", plural).getAsString());
        } finally {
            Locale.setDefault(oldLocale);
            TimeZone.setDefault(oldZone);
        }
    }

    @Test
    public void allowsFunctionOverride() {
        A2uiFunctionRegistry registry = new A2uiFunctionRegistry();
        registry.register("required", args -> new JsonPrimitive("override"));
        assertEquals("override", call(registry, "required", "value", null).getAsString());
    }

    private static JsonElement call(A2uiFunctionRegistry registry, String name,
                                    String key, Object value) {
        return registry.call(name, args(key, value));
    }

    private static Map<String, JsonElement> args(Object... values) {
        Map<String, JsonElement> result = new HashMap<String, JsonElement>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], primitive(values[i + 1]));
        }
        return result;
    }

    private static JsonElement primitive(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof JsonElement) return (JsonElement) value;
        if (value instanceof Number) return new JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        return new JsonPrimitive(value.toString());
    }
}
