package net.Gabou.identity2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;

public final class IdentityConfigManager {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = Identity2.MOD_ID + ".json";
    private static final Set<String> HIDDEN_KEYS = Set.of(
        "EnableMorphCharges",
        "EnableSoulJars",
        "EnablePermanentJarMorphs",
        "EnableSoulAbsorption",
        "disableMorphLossOnDeath"
    );
    private static boolean initialized = false;

    private IdentityConfigManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        load();
        save();
    }

    public static synchronized void load() {
        Path configPath = identity2$resolveConfigPath();
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            identity2$applyLegacyAliases(root);
            for (Field field : IdentitySettings.class.getFields()) {
                if (!identity2$isSupportedConfigField(field, false)) {
                    continue;
                }
                JsonElement value = root.get(field.getName());
                if (value == null || value.isJsonNull()) {
                    continue;
                }
                identity2$applyFieldValue(field, value);
            }
            identity2$normalizeSettings();
        } catch (Throwable throwable) {
            Identity2.LOGGER.error("Failed to load config from {}", configPath, throwable);
        }
    }

    public static synchronized void save() {
        identity2$normalizeSettings();
        JsonObject root = new JsonObject();
        for (Field field : IdentitySettings.class.getFields()) {
            if (!identity2$isSupportedConfigField(field, true)) {
                continue;
            }
            try {
                Class<?> type = field.getType();
                Object value = field.get(null);
                if (type == boolean.class) {
                    root.addProperty(field.getName(), (Boolean) value);
                } else if (type == int.class) {
                    root.addProperty(field.getName(), (Integer) value);
                } else if (type == float.class) {
                    root.addProperty(field.getName(), (Float) value);
                } else if (type == double.class) {
                    root.addProperty(field.getName(), (Double) value);
                } else if (type == String.class) {
                    root.addProperty(field.getName(), (String) value);
                } else if (List.class.isAssignableFrom(type)) {
                    JsonArray array = new JsonArray();
                    if (value instanceof List<?> list) {
                        for (Object entry : list) {
                            if (entry != null) {
                                array.add(String.valueOf(entry));
                            }
                        }
                    }
                    root.add(field.getName(), array);
                }
            } catch (Throwable throwable) {
                Identity2.LOGGER.warn("Failed to serialize config key {}", field.getName(), throwable);
            }
        }

        Path configPath = identity2$resolveConfigPath();
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                configPath,
                GSON.toJson(root),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (Throwable throwable) {
            Identity2.LOGGER.error("Failed to save config to {}", configPath, throwable);
        }
    }

    private static Path identity2$resolveConfigPath() {
        Path configFolder = null;
        try {
            configFolder = Platform.getConfigFolder();
        } catch (Throwable ignored) {
        }
        if (configFolder == null) {
            configFolder = Path.of("config");
        }
        return configFolder.resolve(CONFIG_FILE_NAME);
    }

    private static boolean identity2$isSupportedConfigField(Field field, boolean forSave) {
        int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }
        if (forSave && HIDDEN_KEYS.contains(field.getName())) {
            return false;
        }
        Class<?> type = field.getType();
        return type == boolean.class
            || type == int.class
            || type == float.class
            || type == double.class
            || type == String.class
            || List.class.isAssignableFrom(type);
    }

    private static void identity2$applyLegacyAliases(JsonObject root) {
        if (root == null) {
            return;
        }
        if (!root.has("enableMorphChargeSystem") && root.has("EnableMorphCharges")) {
            root.add("enableMorphChargeSystem", root.get("EnableMorphCharges"));
        }
        if (!root.has("enableSoulJarSystem") && root.has("EnableSoulJars")) {
            root.add("enableSoulJarSystem", root.get("EnableSoulJars"));
        }
        if (!root.has("enablePermanentMorphs") && root.has("EnablePermanentJarMorphs")) {
            root.add("enablePermanentMorphs", root.get("EnablePermanentJarMorphs"));
        }
        if (!root.has("enableSoulAbsorption") && root.has("EnableSoulAbsorption")) {
            root.add("enableSoulAbsorption", root.get("EnableSoulAbsorption"));
        }
        if (!root.has("loseAllMorphsOnDeath") && root.has("disableMorphLossOnDeath")) {
            JsonElement disable = root.get("disableMorphLossOnDeath");
            if (disable != null && disable.isJsonPrimitive()) {
                root.addProperty("loseAllMorphsOnDeath", !disable.getAsBoolean());
            }
        }
    }

    private static void identity2$normalizeSettings() {
        IdentitySettings.EnableMorphCharges = IdentitySettings.enableMorphChargeSystem;
        IdentitySettings.EnableSoulJars = IdentitySettings.enableSoulJarSystem;
        IdentitySettings.EnablePermanentJarMorphs = IdentitySettings.enablePermanentMorphs;
        IdentitySettings.EnableSoulAbsorption = IdentitySettings.enableSoulAbsorption;
    }

    private static void identity2$applyFieldValue(Field field, JsonElement value) {
        try {
            Class<?> type = field.getType();
            if (type == boolean.class && value.isJsonPrimitive()) {
                field.setBoolean(null, value.getAsBoolean());
                return;
            }
            if (type == int.class && value.isJsonPrimitive()) {
                field.setInt(null, value.getAsInt());
                return;
            }
            if (type == float.class && value.isJsonPrimitive()) {
                field.setFloat(null, (float) value.getAsDouble());
                return;
            }
            if (type == double.class && value.isJsonPrimitive()) {
                field.setDouble(null, value.getAsDouble());
                return;
            }
            if (type == String.class && value.isJsonPrimitive()) {
                field.set(null, value.getAsString());
                return;
            }
            if (List.class.isAssignableFrom(type) && value.isJsonArray()) {
                ArrayList<String> out = new ArrayList<>();
                for (JsonElement element : value.getAsJsonArray()) {
                    if (element != null && element.isJsonPrimitive()) {
                        out.add(element.getAsString());
                    }
                }
                field.set(null, out);
            }
        } catch (Throwable throwable) {
            Identity2.LOGGER.warn("Failed to apply config key {}", field.getName(), throwable);
        }
    }
}
