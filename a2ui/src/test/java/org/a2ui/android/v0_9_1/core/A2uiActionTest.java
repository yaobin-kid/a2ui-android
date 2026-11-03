package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class A2uiActionTest {
    @Test
    public void createsCanonicalActionWithResolvedContextAndDataSnapshot() {
        A2uiEngine engine = new A2uiEngine(null);
        engine.accept("{\"version\":\"v0.9.1\",\"createSurface\":{"
                + "\"surfaceId\":\"surface/raw\",\"catalogId\":\"catalog\",\"sendDataModel\":true}}");
        engine.accept(A2uiEngineTest.update("surface/raw", null,
                "{\"products\":[{\"name\":\"One\"}],\"staticValue\":7}", true));
        SurfaceState surface = engine.getSurface("surface/raw");

        JsonObject context = new JsonObject();
        context.add("product", SurfaceStateTest.binding("name"));
        context.addProperty("literal", true);
        JsonObject required = SurfaceStateTest.function("required");
        required.getAsJsonObject("args").add("value", SurfaceStateTest.binding("name"));
        context.add("required", required);
        JsonObject event = new JsonObject();
        event.addProperty("name", "submit");
        event.add("context", context);
        JsonObject actionDefinition = new JsonObject();
        actionDefinition.add("event", event);

        A2uiAction action = A2uiAction.fromEvent(surface, "button-1", actionDefinition,
                "/products/0");
        assertEquals("submit", action.getName());
        assertEquals("surface/raw", action.getSurfaceId());
        assertEquals("button-1", action.getSourceComponentId());
        Instant.parse(action.getTimestamp());
        assertEquals("One", action.getContext().get("product").getAsString());
        assertTrue(action.getContext().get("literal").getAsBoolean());
        assertTrue(action.getContext().get("required").getAsBoolean());

        JsonObject json = action.toJson();
        assertEquals("v0.9.1", json.get("version").getAsString());
        JsonObject body = json.getAsJsonObject("action");
        assertEquals("surface/raw", body.get("surfaceId").getAsString());
        assertEquals("button-1", body.get("sourceComponentId").getAsString());
        assertEquals(action.getTimestamp(), body.get("timestamp").getAsString());
        assertEquals("One", body.getAsJsonObject("context").get("product").getAsString());
        assertFalse(body.has("dataModel"));

        JsonObject snapshot = action.getDataModel();
        snapshot.addProperty("staticValue", 99);
        assertEquals(7, surface.getDataModel().get("staticValue").getAsInt());
        assertEquals(7, action.getDataModel().get("staticValue").getAsInt());
    }

    @Test
    public void omitsDataModelWhenNotRequestedAndDefaultsContext() {
        A2uiEngine engine = new A2uiEngine(null);
        A2uiEngineTest.create(engine, "s");
        JsonObject event = new JsonObject();
        event.addProperty("name", "refresh");
        JsonObject definition = new JsonObject();
        definition.add("event", event);

        A2uiAction action = A2uiAction.fromEvent(engine.getSurface("s"), "root", definition, null);
        assertNull(action.getDataModel());
        assertEquals(0, action.getContext().entrySet().size());
        assertEquals(0, action.toJson().getAsJsonObject("action")
                .getAsJsonObject("context").entrySet().size());
    }
}
