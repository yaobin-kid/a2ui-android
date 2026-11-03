package org.a2ui.android.v0_9_1.core;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class A2uiEngineTest {
    @Test
    public void handlesAllMessageTypesAndIncrementalComponents() {
        RecordingListener listener = new RecordingListener();
        A2uiEngine engine = new A2uiEngine(listener);
        String surfaceId = " surface/01 ";

        engine.accept("{\"version\":\"v0.9.1\",\"createSurface\":{"
                + "\"surfaceId\":\"" + surfaceId + "\",\"catalogId\":\"catalog\","
                + "\"theme\":{\"primaryColor\":\"#fff\"},\"sendDataModel\":true}}");
        SurfaceState surface = engine.getSurface(surfaceId);
        assertNotNull(surface);
        assertEquals(surfaceId, surface.getSurfaceId());
        assertEquals("catalog", surface.getCatalogId());
        assertEquals("#fff", surface.getTheme().get("primaryColor").getAsString());
        assertTrue(surface.getSendDataModel());
        assertNull(engine.getSurface("surface/01"));

        engine.accept("{\"version\":\"v0.9.1\",\"updateComponents\":{"
                + "\"surfaceId\":\"" + surfaceId + "\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Column\",\"children\":[\"title\"]},"
                + "{\"id\":\"title\",\"component\":\"Text\",\"text\":\"old\"}]}}");
        engine.accept("{\"version\":\"v0.9.1\",\"updateComponents\":{"
                + "\"surfaceId\":\"" + surfaceId + "\",\"components\":["
                + "{\"id\":\"title\",\"component\":\"Text\",\"text\":\"new\"}]}}");
        assertEquals(2, surface.getComponents().size());
        assertEquals("new", surface.getComponent("title").get("text").getAsString());
        assertNotNull(surface.getComponent("root"));

        engine.accept("{\"version\":\"v0.9.1\",\"updateDataModel\":{"
                + "\"surfaceId\":\"" + surfaceId + "\",\"value\":{\"count\":1}}}");
        assertEquals(1, surface.getDataModel().get("count").getAsInt());
        assertEquals(Arrays.asList(surfaceId, surfaceId, surfaceId, surfaceId), listener.changed);

        engine.accept("{\"version\":\"v0.9.1\",\"deleteSurface\":{"
                + "\"surfaceId\":\"" + surfaceId + "\"}}");
        assertNull(engine.getSurface(surfaceId));
        assertEquals(Arrays.asList(surfaceId), listener.deleted);
    }

    @Test
    public void acceptsArraysAndJsonLines() {
        A2uiEngine engine = new A2uiEngine(null);
        engine.accept("["
                + "{\"version\":\"v0.9.1\",\"createSurface\":{"
                + "\"surfaceId\":\"a\",\"catalogId\":\"catalog\"}},"
                + "{\"version\":\"v0.9.1\",\"updateDataModel\":{"
                + "\"surfaceId\":\"a\",\"value\":{\"fromArray\":true}}}"
                + "]");
        assertTrue(engine.getSurface("a").getDataModel().get("fromArray").getAsBoolean());

        engine.accept("{\"version\":\"v0.9.1\",\"createSurface\":{"
                + "\"surfaceId\":\"b\",\"catalogId\":\"catalog\"}}\n"
                + "{\"version\":\"v0.9.1\",\"updateComponents\":{"
                + "\"surfaceId\":\"b\",\"components\":["
                + "{\"id\":\"root\",\"component\":\"Text\"}]}}\n");
        assertEquals(2, engine.getSurfaces().size());
        assertNotNull(engine.getSurface("b").getComponent("root"));
    }

    @Test
    public void replacesAndDeletesRootAndNestedData() {
        A2uiEngine engine = new A2uiEngine(null);
        create(engine, "s");
        engine.accept(update("s", null, "{\"user\":{\"name\":\"A\"},\"stale\":1}", true));
        engine.accept(update("s", "/", "{\"user\":{\"name\":\"B\"}}", true));
        assertFalse(engine.getSurface("s").getDataModel().has("stale"));

        engine.accept(update("s", "/user/name", "\"C\"", true));
        engine.accept(update("s", "/new/deep/value", "42", true));
        SurfaceState surface = engine.getSurface("s");
        assertEquals("C", surface.getValue("/user/name", null).getAsString());
        assertEquals(42, surface.getValue("/new/deep/value", null).getAsInt());

        engine.accept(update("s", "/user/name", null, false));
        assertNull(surface.getValue("/user/name", null));
        engine.accept(update("s", null, null, false));
        assertEquals(0, surface.getDataModel().entrySet().size());
    }

    @Test
    public void rejectsInvalidVersionAndUnknownSurface() {
        A2uiEngine engine = new A2uiEngine(null);
        expectException(() -> engine.accept("{\"version\":\"v0.9\",\"createSurface\":{"
                + "\"surfaceId\":\"s\",\"catalogId\":\"c\"}}"));
        expectException(() -> engine.accept("{\"version\":\"v0.9.1\",\"updateDataModel\":{"
                + "\"surfaceId\":\"missing\",\"value\":{}}}"));
        expectException(() -> engine.accept("{\"version\":\"v0.9.1\",\"deleteSurface\":{"
                + "\"surfaceId\":\"missing\"}}"));
    }

    @Test
    public void rejectsDuplicateCreateAndMultipleOperations() {
        A2uiEngine engine = new A2uiEngine(null);
        create(engine, "s");
        expectException(() -> create(engine, "s"));
        expectException(() -> engine.accept("{\"version\":\"v0.9.1\","
                + "\"createSurface\":{\"surfaceId\":\"x\",\"catalogId\":\"c\"},"
                + "\"deleteSurface\":{\"surfaceId\":\"s\"}}"));
    }

    @Test
    public void rejectsNonObjectRootData() {
        A2uiEngine engine = new A2uiEngine(null);
        create(engine, "s");
        expectException(() -> engine.accept(update("s", null, "[]", true)));
    }

    static void create(A2uiEngine engine, String surfaceId) {
        engine.accept("{\"version\":\"v0.9.1\",\"createSurface\":{"
                + "\"surfaceId\":\"" + surfaceId + "\",\"catalogId\":\"catalog\"}}");
    }

    static String update(String surfaceId, String path, String value, boolean hasValue) {
        StringBuilder json = new StringBuilder("{\"version\":\"v0.9.1\",\"updateDataModel\":{"
                + "\"surfaceId\":\"").append(surfaceId).append("\"");
        if (path != null) {
            json.append(",\"path\":\"").append(path).append("\"");
        }
        if (hasValue) {
            json.append(",\"value\":").append(value);
        }
        return json.append("}}").toString();
    }

    static void expectException(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected A2uiException");
        } catch (A2uiException expected) {
            // Expected.
        }
    }

    private static final class RecordingListener implements A2uiEngine.Listener {
        private final List<String> changed = new ArrayList<String>();
        private final List<String> deleted = new ArrayList<String>();

        @Override
        public void onSurfaceChanged(String surfaceId) {
            changed.add(surfaceId);
        }

        @Override
        public void onSurfaceDeleted(String surfaceId) {
            deleted.add(surfaceId);
        }
    }
}
