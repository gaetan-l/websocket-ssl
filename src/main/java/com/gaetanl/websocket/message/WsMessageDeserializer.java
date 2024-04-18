package com.gaetanl.websocket.message;

import java.lang.reflect.Type;

import com.google.gson.*;

public class WsMessageDeserializer implements JsonDeserializer<WsMessage> {
    @Override
    public WsMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            String type = jsonObject.get("type").getAsString();

			Class<?> clazz = Class.forName(type);
	        return context.deserialize(jsonObject, clazz);
		}
        catch (NullPointerException e) {
            throw new JsonParseException(e.getMessage());
        }
		catch (ClassNotFoundException e) {
			throw new JsonParseException(e.getMessage());
		}
    }
}
