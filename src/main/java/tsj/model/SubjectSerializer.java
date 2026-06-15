package tsj.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class SubjectSerializer implements JsonSerializer<Subject> {
    @Override
    public JsonElement serialize(Subject subject, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(subject.id);
    }
}