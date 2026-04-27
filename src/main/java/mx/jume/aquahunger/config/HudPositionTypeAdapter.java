package mx.jume.aquahunger.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class HudPositionTypeAdapter extends TypeAdapter<HudPosition> {

    @Override
    public void write(JsonWriter out, HudPosition value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.name());
        }
    }

    @Override
    public HudPosition read(JsonReader in) throws IOException {
        String name = in.nextString();
        HudPosition pos = HudPosition.valueOf(name);
        return pos != null ? pos : HudPosition.pluginDefault();
    }
}
