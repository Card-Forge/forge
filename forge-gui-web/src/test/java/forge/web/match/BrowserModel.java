package forge.web.match;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** The game's objects as the browser holds them. It applies the same rules as src/main/ts/model.ts, so tests can rebuild the page's model from the state messages sent. */
public final class BrowserModel {
    private final Map<Integer, JsonObject> objects = new LinkedHashMap<>();
    private int root = -1;

    public synchronized void applyStateMessage(final JsonObject msg) {
        if (msg.get("full").getAsBoolean()) {
            objects.clear();
            root = msg.get("root").getAsInt();
        }
        for (final Map.Entry<String, JsonElement> e : msg.getAsJsonObject("newObjects").entrySet()) {
            // A property at its default is left out of an object. A null in a new object means the same, so it is removed
            final JsonObject copy = e.getValue().getAsJsonObject().deepCopy();
            copy.entrySet().removeIf(field -> field.getValue().isJsonNull());
            objects.put(Integer.parseInt(e.getKey()), copy);
        }
        for (final Map.Entry<String, JsonElement> e : msg.getAsJsonObject("deltas").entrySet()) {
            final JsonObject target = objects.get(Integer.parseInt(e.getKey()));
            if (target == null) {
                continue;
            }
            for (final Map.Entry<String, JsonElement> field : e.getValue().getAsJsonObject().entrySet()) {
                if (field.getValue().isJsonNull()) {
                    target.remove(field.getKey());
                } else {
                    target.add(field.getKey(), field.getValue().deepCopy());
                }
            }
        }
        prune();
    }

    /** A copy, so a test can compare models without sharing their objects. */
    synchronized Map<Integer, JsonObject> objectsCopy() {
        final Map<Integer, JsonObject> copy = new LinkedHashMap<>();
        objects.forEach((k, v) -> copy.put(k, v.deepCopy()));
        return copy;
    }

    // Packets do not say when an object is removed. So any object the root no longer references is dropped here
    private void prune() {
        if (!objects.containsKey(root)) {
            return;
        }
        final Set<Integer> reachable = new HashSet<>();
        final Deque<Integer> todo = new ArrayDeque<>();
        todo.push(root);
        while (!todo.isEmpty()) {
            final int key = todo.pop();
            if (!reachable.add(key)) {
                continue;
            }
            final JsonObject o = objects.get(key);
            if (o != null) {
                collectRefs(o, todo);
            }
        }
        objects.keySet().retainAll(reachable);
    }

    private static void collectRefs(final JsonElement e, final Deque<Integer> out) {
        if (e.isJsonArray()) {
            for (final JsonElement item : e.getAsJsonArray()) {
                collectRefs(item, out);
            }
        } else if (e.isJsonObject()) {
            final JsonObject o = e.getAsJsonObject();
            if (o.size() == 1 && o.has("ref")) {
                out.push(o.get("ref").getAsInt());
                return;
            }
            for (final Map.Entry<String, JsonElement> field : o.entrySet()) {
                collectRefs(field.getValue(), out);
            }
        }
    }
}
