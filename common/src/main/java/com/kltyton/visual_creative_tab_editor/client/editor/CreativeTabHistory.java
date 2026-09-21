package com.kltyton.visual_creative_tab_editor.client.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

/** A world's committed edit history, copied into each editing session. */
public final class CreativeTabHistory {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MAX_STATES = 64;
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final long MAX_RETAINED_ITEMS = 4_194_304L;
    private final Path file;
    private final Deque<State> undo = new ArrayDeque<>();
    private final Deque<State> redo = new ArrayDeque<>();
    private State anchor;
    private State pending;
    private boolean preservePrevious;
    private boolean readable = true;

    public record State(CreativeTabCatalog catalog, Identifier currentTab) { }

    private CreativeTabHistory(Path file, State anchor) {
        this.file = file;
        this.anchor = anchor;
    }

    public static CreativeTabHistory open(Path file, State current, HolderLookup.Provider registries) {
        CreativeTabHistory history = new CreativeTabHistory(file, current);
        if (!Files.isRegularFile(file)) {
            return history;
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IOException("History exceeds its size limit");
            }
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("format").getAsInt() != 1) {
                throw new IOException("Unsupported history format");
            }
            CreativeTabCatalog saved = CreativeTabCatalogJson.decode(GSON.toJson(root.get("anchor")), registries);
            if (!sameContent(saved, current.catalog())) {
                history.preservePrevious = true;
                return history;
            }
            history.anchor = new State(saved, current.currentTab());
            history.readStates(root.getAsJsonArray("undo"), history.undo, saved, registries);
            history.readStates(root.getAsJsonArray("redo"), history.redo, saved, registries);
            history.trim();
        } catch (IOException | RuntimeException exception) {
            history.undo.clear();
            history.redo.clear();
            history.readable = false;
            VisualCreativeTabEditorConstants.LOGGER.warn("Could not load creative tab history; preserving {}", file, exception);
        }
        return history;
    }

    public State anchor() { return this.anchor; }
    public boolean hasUndo() { return !this.undo.isEmpty(); }
    public boolean hasRedo() { return !this.redo.isEmpty(); }

    public void beforeChange(State state) {
        if (this.pending == null) {
            this.pending = state;
        }
    }

    public void afterChange(CreativeTabCatalog current) {
        if (this.pending == null) {
            return;
        }
        if (!sameContent(this.pending.catalog(), current)) {
            this.undo.addLast(this.pending);
            this.redo.clear();
            trim();
        }
        this.pending = null;
    }

    public State undo(State current) {
        afterChange(current.catalog());
        if (this.undo.isEmpty()) {
            return current;
        }
        this.redo.addLast(current);
        return this.undo.removeLast();
    }

    public State redo(State current) {
        afterChange(current.catalog());
        if (this.redo.isEmpty()) {
            return current;
        }
        this.undo.addLast(current);
        return this.redo.removeLast();
    }

    /** Called only after the normal local/server save has succeeded. */
    public boolean commit(State current, HolderLookup.Provider registries) {
        afterChange(current.catalog());
        if (!this.readable) {
            return false;
        }
        try {
            byte[] bytes;
            while (true) {
                JsonObject root = new JsonObject();
                root.addProperty("format", 1);
                root.add("anchor", JsonParser.parseString(CreativeTabCatalogJson.encode(current.catalog(), registries)));
                root.add("undo", writeStates(this.undo, current.catalog(), registries));
                root.add("redo", writeStates(this.redo, current.catalog(), registries));
                bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
                if (bytes.length <= MAX_BYTES) {
                    break;
                }
                if (!dropOldest()) {
                    throw new IOException("Current creative tab catalog exceeds history capacity");
                }
            }
            Files.createDirectories(this.file.getParent());
            Path pendingFile = Files.createTempFile(this.file.getParent(), "history-", ".pending");
            try {
                Files.write(pendingFile, bytes);
                if (this.preservePrevious && Files.isRegularFile(this.file)) {
                    Files.copy(this.file, this.file.resolveSibling(this.file.getFileName() + ".previous"), StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.move(pendingFile, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(pendingFile, this.file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(pendingFile);
            }
            this.anchor = current;
            this.preservePrevious = false;
            return true;
        } catch (IOException | RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.warn("Layout saved, but creative tab history could not be written", exception);
            return false;
        }
    }

    private JsonArray writeStates(Deque<State> states, CreativeTabCatalog base, HolderLookup.Provider registries) {
        JsonArray result = new JsonArray();
        for (State state : states) {
            JsonObject entry = new JsonObject();
            if (state.currentTab() != null) {
                entry.addProperty("selected", state.currentTab().toString());
            }
            JsonArray tabs = new JsonArray();
            for (CreativeTabDefinition definition : state.catalog().orderedDefinitions()) {
                JsonObject tab = new JsonObject();
                tab.addProperty("id", definition.id().toString());
                CreativeTabDefinition original = base.definition(definition.id()).orElse(null);
                CreativeTabPatch patch = original == null ? CreativeTabPatch.full(definition) : CreativeTabPatch.diff(original, definition);
                if (!patch.isEmpty()) {
                    tab.add("data", CreativeTabJsonCodec.encodePatch(patch, registries));
                }
                tabs.add(tab);
            }
            entry.add("tabs", tabs);
            result.add(entry);
        }
        return result;
    }

    private void readStates(JsonArray entries, Deque<State> destination, CreativeTabCatalog base,
                            HolderLookup.Provider registries) throws IOException {
        if (entries.size() > MAX_STATES) {
            throw new IOException("Too many history states");
        }
        long items = 0;
        for (var element : entries) {
            JsonObject entry = element.getAsJsonObject();
            JsonArray tabs = entry.getAsJsonArray("tabs");
            if (tabs.size() > CreativeTabValidation.MAX_TABS) {
                throw new IOException("Too many history tabs");
            }
            List<CreativeTabDefinition> definitions = new ArrayList<>();
            for (var value : tabs) {
                JsonObject tab = value.getAsJsonObject();
                Identifier id = Objects.requireNonNull(Identifier.tryParse(tab.get("id").getAsString()));
                CreativeTabDefinition definition = base.definition(id).orElse(null);
                if (tab.has("data")) {
                    CreativeTabPatch patch = CreativeTabJsonCodec.decodePatch(tab.get("data"), registries);
                    definition = definition == null ? CreativeTabDefinition.resolve(id, List.of(patch)) : definition.apply(patch);
                }
                definitions.add(Objects.requireNonNull(definition));
                items += (long) definition.itemCount() + definition.searchItemCount();
                if (items > MAX_RETAINED_ITEMS) {
                    throw new IOException("History exceeds retained item capacity");
                }
            }
            Identifier selected = entry.has("selected") ? Identifier.tryParse(entry.get("selected").getAsString()) : null;
            destination.addLast(new State(new CreativeTabCatalog(definitions), selected));
        }
    }

    private void trim() {
        long items = 0;
        for (Deque<State> states : List.of(this.undo, this.redo)) {
            for (State state : states) {
                for (CreativeTabDefinition definition : state.catalog().orderedDefinitions()) {
                    items += (long) definition.itemCount() + definition.searchItemCount();
                }
            }
        }
        while (this.undo.size() + this.redo.size() > 1
                && (this.undo.size() + this.redo.size() > MAX_STATES || items > MAX_RETAINED_ITEMS)) {
            State removed = !this.undo.isEmpty() ? this.undo.removeFirst() : this.redo.removeFirst();
            for (CreativeTabDefinition definition : removed.catalog().orderedDefinitions()) {
                items -= (long) definition.itemCount() + definition.searchItemCount();
            }
        }
    }

    private boolean dropOldest() {
        if (!this.undo.isEmpty()) { this.undo.removeFirst(); return true; }
        if (!this.redo.isEmpty()) { this.redo.removeFirst(); return true; }
        return false;
    }

    public static boolean sameContent(CreativeTabCatalog left, CreativeTabCatalog right) {
        List<CreativeTabDefinition> a = left.orderedDefinitions();
        List<CreativeTabDefinition> b = right.orderedDefinitions();
        if (a.size() != b.size()) { return false; }
        for (int index = 0; index < a.size(); index++) {
            if (a.get(index) != b.get(index)
                    && (!a.get(index).id().equals(b.get(index).id()) || !CreativeTabPatch.diff(a.get(index), b.get(index)).isEmpty())) {
                return false;
            }
        }
        return true;
    }
}
