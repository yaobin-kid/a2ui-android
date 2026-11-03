package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SurfaceStateTest {
    @Test
    public void resolvesScopedAndRootBindings() {
        A2uiEngine engine = engineWithData();
        SurfaceState surface = engine.getSurface("s");

        assertEquals("One", surface.resolveString(binding("name"), "/products/0"));
        assertEquals("One", surface.resolveString(binding("/name"), "/products/0"));
        assertEquals("Two", surface.resolveString(binding("name"), "/products/1"));
        assertEquals("Root", surface.resolveString(binding("/name"), null));
        assertEquals("12.5", surface.resolveString(binding("price"), "/products/0"));
        assertTrue(surface.resolveBoolean(binding("visible"), "/products/0"));
        assertEquals(12.5, surface.resolveDouble(binding("price"), "/products/0"), 0.0);

        List<String> tags = surface.resolveStringList(binding("tags"), "/products/0");
        assertEquals(Arrays.asList("a", "b"), tags);
        assertNull(surface.resolve(binding("missing"), "/products/0"));
        assertFalse(surface.resolveBoolean(binding("missing"), "/products/0"));
    }

    @Test
    public void writesEscapedPointersAndArraysWithoutNotifyingEngine() {
        A2uiEngine engine = engineWithData();
        SurfaceState surface = engine.getSurface("s");
        surface.setLocalValue("name", new JsonPrimitive("Updated"), "/products/0");
        assertEquals("Updated", surface.getValue("/products/0/name", null).getAsString());

        surface.setLocalValue("/a~1b/~0key/-", new JsonPrimitive("one"), null);
        surface.setLocalValue("/a~1b/~0key/0", new JsonPrimitive("changed"), null);
        surface.setLocalValue("/list/-", new JsonPrimitive(3), null);
        surface.setLocalValue("/list/3", new JsonPrimitive(4), null);
        assertEquals("changed", surface.getValue("/a~1b/~0key/0", null).getAsString());
        assertEquals("one", surface.getValue("/a~1b/~0key/1", null).getAsString());
        assertEquals(4, surface.getDataModel().getAsJsonArray("list").size());
        assertEquals(4, surface.getValue("/list/3", null).getAsInt());

        A2uiEngineTest.expectException(() -> surface.getValue("/a~2b", null));
        A2uiEngineTest.expectException(() -> surface.setLocalValue("/list/8",
                new JsonPrimitive(8), null));
    }

    @Test
    public void deletesArrayEntriesThroughDataUpdates() {
        A2uiEngine engine = engineWithData();
        engine.accept(A2uiEngineTest.update("s", "/list/0", null, false));
        JsonArray list = engine.getSurface("s").getDataModel().getAsJsonArray("list");
        assertEquals(1, list.size());
        assertEquals(2, list.get(0).getAsInt());
    }

    @Test
    public void evaluatesFunctionsWithScopedArguments() {
        A2uiEngine engine = engineWithData();
        SurfaceState surface = engine.getSurface("s");
        JsonObject format = function("formatString");
        format.getAsJsonObject("args").addProperty("value",
                "Hello ${name}: ${formatDate(value:${/date}, format:'yyyy-MM-dd')}");
        assertEquals("Hello One: 2026-01-16", surface.resolveString(format, "/products/0"));

        JsonObject and = function("and");
        JsonArray values = new JsonArray();
        values.add(binding("visible"));
        JsonObject not = function("not");
        not.getAsJsonObject("args").addProperty("value", false);
        values.add(not);
        and.getAsJsonObject("args").add("values", values);
        assertTrue(surface.resolveBoolean(and, "/products/0"));

        JsonObject required = function("required");
        required.getAsJsonObject("args").add("value", binding("name"));
        assertTrue(surface.resolveBoolean(required, "/products/0"));
    }

    @Test
    public void supportsCustomFunctionsAndDefaultOpenUrlFailure() {
        A2uiEngine engine = engineWithData();
        engine.getFunctionRegistry().register("join", args -> {
            String value = args.get("value").getAsString();
            return new JsonPrimitive("custom:" + value);
        });
        SurfaceState surface = engine.getSurface("s");
        JsonObject join = function("join");
        join.getAsJsonObject("args").add("value", binding("name"));
        assertEquals("custom:One", surface.resolveString(join, "/products/0"));

        JsonObject openUrl = function("openUrl");
        openUrl.getAsJsonObject("args").addProperty("url", "https://example.com");
        A2uiEngineTest.expectException(() -> surface.resolve(openUrl, null));
    }

    static JsonObject binding(String path) {
        JsonObject binding = new JsonObject();
        binding.addProperty("path", path);
        return binding;
    }

    static JsonObject function(String name) {
        JsonObject function = new JsonObject();
        function.addProperty("call", name);
        function.add("args", new JsonObject());
        return function;
    }

    private static A2uiEngine engineWithData() {
        A2uiEngine engine = new A2uiEngine(null);
        A2uiEngineTest.create(engine, "s");
        engine.accept(A2uiEngineTest.update("s", null,
                "{\"name\":\"Root\",\"date\":\"2026-01-16\","
                        + "\"products\":[{\"name\":\"One\",\"price\":12.5,"
                        + "\"visible\":true,\"tags\":[\"a\",\"b\"]},"
                        + "{\"name\":\"Two\"}],"
                        + "\"a/b\":{\"~key\":[\"zero\"]},\"list\":[1,2]}", true));
        return engine;
    }
}
