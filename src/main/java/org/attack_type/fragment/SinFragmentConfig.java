package org.attack_type.fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.attack_type.api.SinType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SinFragmentConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("attack_type.json");

    @SerializedName("auto_trigger_enabled")
    public boolean autoTriggerEnabled = false;

    @SerializedName("auto_trigger_threshold")
    public int autoTriggerThreshold = 200;

    @SerializedName("preferred_sins")
    public List<String> preferredSins = new ArrayList<>();

    @SerializedName("auto_trigger_level")
    public int autoTriggerLevel = 1;

    public static SinFragmentConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, SinFragmentConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SinFragmentConfig config = new SinFragmentConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSinPreferred(SinType type) {
        if (preferredSins.isEmpty()) return true;
        return preferredSins.contains(type.name());
    }
}