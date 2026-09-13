package com.esezak.superupgrader.config;

import com.esezak.superupgrader.SuperUpgrader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE = new ModConfig();

    /** Hard-set the value of specific items. Overrides recipe calculation. */
    public Map<String, Double> valueOverrides = new HashMap<>();

    /** Global multiplier applied to ALL computed values. Default 1.0 */
    public double globalValueMultiplier = 1.0;

    /** Per-item value multipliers. Stacks with globalValueMultiplier. */
    public Map<String, Double> valueMultipliers = new HashMap<>();

    /** Items banned from input AND target list. */
    public List<String> blacklist = List.of();

    /** Cooldown in seconds after each wager. 0 = no cooldown (default). */
    public int attemptCooldownSeconds = 0;

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("superupgrader.json");

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                if (INSTANCE == null) INSTANCE = new ModConfig();
                if (INSTANCE.valueOverrides == null) INSTANCE.valueOverrides = new HashMap<>();
                if (INSTANCE.valueMultipliers == null) INSTANCE.valueMultipliers = new HashMap<>();
                if (INSTANCE.blacklist == null) INSTANCE.blacklist = List.of();
                if (!Double.isFinite(INSTANCE.globalValueMultiplier) || INSTANCE.globalValueMultiplier <= 0) {
                    INSTANCE.globalValueMultiplier = 1;
                }
                SuperUpgrader.LOGGER.info("Loaded config from {}", configFile);
            } catch (IOException | com.google.gson.JsonParseException e) {
                SuperUpgrader.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
            SuperUpgrader.LOGGER.info("Created default config at {}", configFile);
        }
    }

    public static void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("superupgrader.json");

        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            SuperUpgrader.LOGGER.error("Failed to save config", e);
        }
    }
}
