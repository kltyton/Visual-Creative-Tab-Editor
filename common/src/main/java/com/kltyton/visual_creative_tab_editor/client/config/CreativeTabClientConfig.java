package com.kltyton.visual_creative_tab_editor.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Loads client display options during loader initialization. */
public final class CreativeTabClientConfig {
    private static boolean groupSimilarItems = true;

    private CreativeTabClientConfig() {
    }

    public static boolean groupSimilarItems() {
        return groupSimilarItems;
    }

    public static void load(Path configDirectory) {
        Path file = configDirectory.resolve(VisualCreativeTabEditorConstants.MOD_ID).resolve("client.json");
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "{\n  \"group_similar_items\": true\n}\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
                groupSimilarItems = true;
                return;
            }
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonElement value = root.get("group_similar_items");
                if (value != null && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())) {
                    throw new IllegalArgumentException("group_similar_items must be a boolean");
                }
                groupSimilarItems = value == null || value.getAsBoolean();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read client configuration: " + file, exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid client configuration: " + file, exception);
        }
    }
}
