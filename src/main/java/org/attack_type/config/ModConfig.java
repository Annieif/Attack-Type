package org.attack_type.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;
import org.attack_type.Attack_type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 模组统一配置加载器。
 * 从 config.json 读取所有可调参数，暴露为静态字段。
 */
public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("attack_type");
    private static final Gson GSON = new Gson();
    private static final String CONFIG_PATH = "assets/attack_type/config.json";

    // ---- Fragment Costs ----
    public static int COST_LEVEL_1 = 40;
    public static int COST_LEVEL_2 = 70;
    public static int COST_LEVEL_3 = 100;

    // ---- Fragment Thresholds ----
    public static int OVERFLOW_THRESHOLD = 500;
    public static int KILL_THRESHOLD = 1000;

    // ---- Fragment Durations ----
    public static int DURATION_L1_TICKS = 80;
    public static int DURATION_L2_TICKS = 140;
    public static int DURATION_L3_TICKS = 200;

    // ---- Wrath ----
    public static int[] WRATH_KILL_CHAIN_BONUSES = {7, 5, 3, 1};
    public static int[] WRATH_KILL_CHAIN_INTERVALS = {20, 40, 100, 200};

    // ---- Lust ----
    public static int LUST_EGG_COOLDOWN_TICKS = 100;
    public static int LUST_EGG_AMOUNT = 1;
    public static int LUST_BREED_AMOUNT = 1;
    public static int LUST_CURE_AMOUNT = 10;
    public static int LUST_CONVERT_AMOUNT = 5;
    public static int LUST_DROWN_AMOUNT = 5;
    public static int LUST_LIGHTNING_AMOUNT = 5;

    // ---- Sloth ----
    public static int SLOTH_AFK_INTERVAL_TICKS = 1200;
    public static int SLOTH_AFK_AMOUNT = 3;
    public static int SLOTH_SLEEP_AMOUNT = 5;

    // ---- Gluttony ----
    public static int GLUTTONY_PER_FOOD = 1;

    // ---- Gloom ----
    public static int GLOOM_DAMAGE_PER_FRAGMENT = 10;
    public static int GLOOM_SELF_AMOUNT = 1;
    public static int GLOOM_WITNESS_AMOUNT = 1;

    // ---- Pride ----
    public static int PRIDE_CRAFT_THRESHOLD = 576;
    public static int PRIDE_ACHIEVEMENT_AMOUNT = 10;
    public static int PRIDE_PRODUCTION_AMOUNT = 2;

    // ---- Envy ----
    public static int ENVY_INTERVAL_TICKS = 1200;
    public static int ENVY_COMPARE_AMOUNT = 3;
    public static int ENVY_WITNESS_AMOUNT = 1;

    // ---- Total Product ----
    public static double TOTAL_PRODUCT_DECAY_RATE = 0.1;
    public static double TOTAL_PRODUCT_MIN = 0.1;

    private ModConfig() {}

    public static void load() {
        Path externalPath = FabricLoader.getInstance().getConfigDir().resolve("attack_type.json");

        try {
            if (Files.exists(externalPath)) {
                LOGGER.info("[AttackType] Loading config from external: {}", externalPath);
                try (Reader reader = Files.newBufferedReader(externalPath)) {
                    parse(JsonParser.parseReader(reader).getAsJsonObject());
                }
            } else {
                LOGGER.info("[AttackType] Loading default config from jar: {}", CONFIG_PATH);
                InputStream is = Attack_type.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
                if (is == null) {
                    LOGGER.warn("[AttackType] Config not found in jar, using defaults");
                    return;
                }
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    parse(JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        } catch (Exception e) {
            LOGGER.error("[AttackType] Failed to load config, using defaults", e);
        }
    }

    public static boolean loadPreset(String name) {
        String path = "assets/attack_type/presets/" + name + ".json";
        InputStream is = Attack_type.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            LOGGER.warn("[AttackType] Preset '{}' not found", name);
            return false;
        }
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            parse(JsonParser.parseReader(reader).getAsJsonObject());
            LOGGER.info("[AttackType] Loaded preset: {}", name);
            return true;
        } catch (Exception e) {
            LOGGER.error("[AttackType] Failed to load preset '{}'", name, e);
            return false;
        }
    }

    private static void parse(JsonObject root) {
        JsonObject costs = root.getAsJsonObject("fragmentCosts");
        if (costs != null) {
            COST_LEVEL_1 = getInt(costs, "level1", COST_LEVEL_1);
            COST_LEVEL_2 = getInt(costs, "level2", COST_LEVEL_2);
            COST_LEVEL_3 = getInt(costs, "level3", COST_LEVEL_3);
        }

        JsonObject thresholds = root.getAsJsonObject("fragmentThresholds");
        if (thresholds != null) {
            OVERFLOW_THRESHOLD = getInt(thresholds, "overflow", OVERFLOW_THRESHOLD);
            KILL_THRESHOLD = getInt(thresholds, "kill", KILL_THRESHOLD);
        }

        JsonObject durations = root.getAsJsonObject("fragmentDurations");
        if (durations != null) {
            DURATION_L1_TICKS = getInt(durations, "level1Ticks", DURATION_L1_TICKS);
            DURATION_L2_TICKS = getInt(durations, "level2Ticks", DURATION_L2_TICKS);
            DURATION_L3_TICKS = getInt(durations, "level3Ticks", DURATION_L3_TICKS);
        }

        JsonObject acq = root.getAsJsonObject("fragmentAcquisition");
        if (acq != null) {
            parseAcquisition(acq);
        }

        JsonObject tp = root.getAsJsonObject("totalProduct");
        if (tp != null) {
            TOTAL_PRODUCT_DECAY_RATE = getDouble(tp, "decayRate", TOTAL_PRODUCT_DECAY_RATE);
            TOTAL_PRODUCT_MIN = getDouble(tp, "minProduct", TOTAL_PRODUCT_MIN);
        }
    }

    private static void parseAcquisition(JsonObject acq) {
        JsonObject wrath = acq.getAsJsonObject("wrath");
        if (wrath != null) {
            WRATH_KILL_CHAIN_BONUSES = getIntArray(wrath, "killChainBonuses", WRATH_KILL_CHAIN_BONUSES);
            WRATH_KILL_CHAIN_INTERVALS = getIntArray(wrath, "killChainIntervals", WRATH_KILL_CHAIN_INTERVALS);
        }

        JsonObject lust = acq.getAsJsonObject("lust");
        if (lust != null) {
            LUST_EGG_COOLDOWN_TICKS = getInt(lust, "eggCooldownTicks", LUST_EGG_COOLDOWN_TICKS);
            LUST_EGG_AMOUNT = getInt(lust, "eggAmount", LUST_EGG_AMOUNT);
            LUST_BREED_AMOUNT = getInt(lust, "breedAmount", LUST_BREED_AMOUNT);
            LUST_CURE_AMOUNT = getInt(lust, "cureAmount", LUST_CURE_AMOUNT);
            LUST_CONVERT_AMOUNT = getInt(lust, "convertAmount", LUST_CONVERT_AMOUNT);
            LUST_DROWN_AMOUNT = getInt(lust, "drownAmount", LUST_DROWN_AMOUNT);
            LUST_LIGHTNING_AMOUNT = getInt(lust, "lightningAmount", LUST_LIGHTNING_AMOUNT);
        }

        JsonObject sloth = acq.getAsJsonObject("sloth");
        if (sloth != null) {
            SLOTH_AFK_INTERVAL_TICKS = getInt(sloth, "afkIntervalTicks", SLOTH_AFK_INTERVAL_TICKS);
            SLOTH_AFK_AMOUNT = getInt(sloth, "afkAmount", SLOTH_AFK_AMOUNT);
            SLOTH_SLEEP_AMOUNT = getInt(sloth, "sleepAmount", SLOTH_SLEEP_AMOUNT);
        }

        JsonObject gluttony = acq.getAsJsonObject("gluttony");
        if (gluttony != null) {
            GLUTTONY_PER_FOOD = getInt(gluttony, "perFood", GLUTTONY_PER_FOOD);
        }

        JsonObject gloom = acq.getAsJsonObject("gloom");
        if (gloom != null) {
            GLOOM_DAMAGE_PER_FRAGMENT = getInt(gloom, "damagePerFragment", GLOOM_DAMAGE_PER_FRAGMENT);
            GLOOM_SELF_AMOUNT = getInt(gloom, "selfAmount", GLOOM_SELF_AMOUNT);
            GLOOM_WITNESS_AMOUNT = getInt(gloom, "witnessAmount", GLOOM_WITNESS_AMOUNT);
        }

        JsonObject pride = acq.getAsJsonObject("pride");
        if (pride != null) {
            PRIDE_CRAFT_THRESHOLD = getInt(pride, "craftThreshold", PRIDE_CRAFT_THRESHOLD);
            PRIDE_ACHIEVEMENT_AMOUNT = getInt(pride, "achievementAmount", PRIDE_ACHIEVEMENT_AMOUNT);
            PRIDE_PRODUCTION_AMOUNT = getInt(pride, "productionAmount", PRIDE_PRODUCTION_AMOUNT);
        }

        JsonObject envy = acq.getAsJsonObject("envy");
        if (envy != null) {
            ENVY_INTERVAL_TICKS = getInt(envy, "intervalTicks", ENVY_INTERVAL_TICKS);
            ENVY_COMPARE_AMOUNT = getInt(envy, "compareAmount", ENVY_COMPARE_AMOUNT);
            ENVY_WITNESS_AMOUNT = getInt(envy, "witnessAmount", ENVY_WITNESS_AMOUNT);
        }
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        JsonElement elem = obj.get(key);
        return (elem != null && !elem.isJsonNull()) ? elem.getAsInt() : defaultValue;
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue) {
        JsonElement elem = obj.get(key);
        return (elem != null && !elem.isJsonNull()) ? elem.getAsDouble() : defaultValue;
    }

    private static int[] getIntArray(JsonObject obj, String key, int[] defaultValue) {
        JsonElement elem = obj.get(key);
        if (elem == null || !elem.isJsonArray()) return defaultValue;
        return GSON.fromJson(elem, int[].class);
    }
}