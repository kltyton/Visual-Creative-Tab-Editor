package com.kltyton.visual_creative_tab_editor.data.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

/** Strict field readers shared by tab documents and the native NBT item format. */
public final class JsonFields {
    private JsonFields() {}


    public static int readInt(JsonObject object, String field) {
        JsonElement element = requireValue(object, field);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be an integer");
        }
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException(field + " must be a 32-bit integer", exception);
        }
    }

    public static String readString(JsonObject object, String field) {
        JsonElement element = requireValue(object, field);
        if (!element.isJsonPrimitive()) {
            throw new JsonParseException(field + " must be a string");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new JsonParseException(field + " must be a string");
        }
        return primitive.getAsString();
    }

    public static JsonElement requireValue(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull()) {
            throw new JsonParseException(field + " must not be null");
        }
        return element;
    }
}
