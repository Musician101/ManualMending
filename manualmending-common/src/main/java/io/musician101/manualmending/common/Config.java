package io.musician101.manualmending.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.annotation.Nonnull;

public class Config {

    private boolean repairAll = false;
    private final File configDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Config(@Nonnull File configDir) {
        this.configDir = configDir;
    }

    @Nonnull
    private File getFile() {
        return new File(configDir, "config.json");
    }

    public boolean repairAll() {
        return repairAll;
    }

    public void load() throws IOException {
        File file = getFile();
        if (!file.exists()) {
            configDir.mkdirs();
            file.createNewFile();
            try (FileWriter fw = new FileWriter(file)) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("repairAll", false);
                gson.toJson(jsonObject, fw);
            }
        }

        try (FileReader fr = new FileReader(file)) {
            repairAll = gson.fromJson(fr, JsonObject.class).get("repairAll").getAsBoolean();
        }
    }
}
