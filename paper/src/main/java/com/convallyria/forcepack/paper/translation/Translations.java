package com.convallyria.forcepack.paper.translation;

import com.convallyria.forcepack.paper.ForcePackPaper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum Translations {
    DECLINED(Component.translatable("declined")),
    ACCEPTED(Component.translatable("accepted")),
    DOWNLOAD_START_TITLE(Component.translatable("download_start_title")),
    DOWNLOAD_START_SUBTITLE(Component.translatable("download_start_subtitle")),
    DOWNLOAD_FAILED(Component.translatable("download_failed")),
    PROMPT_TEXT(Component.translatable("prompt_text")),
    RELOADING(Component.translatable("reloading"));

    private final TranslatableComponent key;

    Translations(TranslatableComponent key) {
        this.key = key;
    }

    private String getPath() {
        return this.key.key();
    }

    public void send(Player player, Object... values) {
        get(values).forEach(component -> {
            ForcePackPaper.getInstance().adventure().player(player).sendMessage(component);
        });
    }

    public void send(Audience audience, Object... values) {
        get(values).forEach(audience::sendMessage);
    }

    public List<Component> get(Object... values) {
        // TODO: This is a temporary solution until locale is implemented
        final Object translationObject = defaultConfig.get(getPath());
        List<String> toTranslate = new ArrayList<>();
        if (translationObject instanceof String) {
            toTranslate.add(String.format((String) translationObject, values));
        } else if (translationObject instanceof List<?>) {
            for (String string : (List<String>) translationObject) {
                toTranslate.add(String.format(string, values));
            }
        }

        List<Component> components = new ArrayList<>(toTranslate.size());
        for (String string : toTranslate) {
            components.add(ForcePackPaper.getInstance().miniMessage().deserialize(string));
        }

        return components;
    }

    static FileConfiguration defaultConfig;

    public static void generateLang(ForcePackPaper plugin) {
        File lang = new File(plugin.getDataFolder() + "/lang/");
        lang.mkdirs();

        for (Locale locale : Locale.getAvailableLocales()) {
            final File target = new File(lang + File.separator + locale.toString() + ".yml");
            try {
                if (!target.exists()) {
                    plugin.saveResource("lang/" + locale + ".yml", false);
                }
                plugin.getLogger().info("Generated " + locale + ".yml");
            } catch (IllegalArgumentException ignored) { }

            if (target.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(target);
                for (Translations key : values()) {
                    if (config.get(key.getPath()) == null) {
                        plugin.getLogger().warning("No value in translation file for key "
                                + key + " was found. Regenerate language files?");
                    }
                }
            }
        }

        defaultConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/en_GB.yml"));
    }
}
