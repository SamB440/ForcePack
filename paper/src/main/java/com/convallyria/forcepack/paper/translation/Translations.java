package com.convallyria.forcepack.paper.translation;

import com.convallyria.forcepack.paper.ForcePackPaper;
import com.convallyria.languagy.api.language.Language;
import com.convallyria.languagy.api.language.key.TranslationKey;
import com.convallyria.languagy.api.language.translation.Translation;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public enum Translations {
    DECLINED(TranslationKey.of("declined")),
    ACCEPTED(TranslationKey.of("accepted")),
    DOWNLOAD_START_TITLE(TranslationKey.of("download_start_title")),
    DOWNLOAD_START_SUBTITLE(TranslationKey.of("download_start_subtitle")),
    DOWNLOAD_FAILED(TranslationKey.of("download_failed")),
    PROMPT_TEXT(TranslationKey.of("prompt_text")),
    RELOADING(TranslationKey.of("reloading"));

    private final TranslationKey key;

    Translations(TranslationKey key) {
        this.key = key;
    }

    public TranslationKey getKey() {
        return key;
    }

    public void send(Player player, Object... values) {
        Translation message = getTranslation(player);
        message.format(values);
        message.send();
    }

    public List<Component> getProper(Player player) {
        return getTranslation(player).colour();
    }

    @Deprecated
    public String get(Player player) {
        return BukkitComponentSerializer.legacy().serialize(getTranslation(player).colour().get(0));
    }

    public Translation getTranslation(Player player) {
        return ForcePackPaper.getInstance().getTranslator().getTranslationFor(player, key);
    }

    public static void generateLang(ForcePackPaper plugin) {
        File lang = new File(plugin.getDataFolder() + "/lang/");
        lang.mkdirs();

        for (Language language : Language.values()) {
            try {
                if (!new File(lang + File.separator + language.getKey().getCode() + ".yml").exists()) {
                    plugin.saveResource("lang/" + language.getKey().getCode() + ".yml", false);
                }
                plugin.getLogger().info("Generated " + language.getKey().getCode() + ".yml");
            } catch (IllegalArgumentException ignored) { }

            File file = new File(plugin.getDataFolder() + "/lang/" + language.getKey().getCode() + ".yml");
            if (file.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (Translations key : values()) {
                    if (config.get(key.toString().toLowerCase()) == null) {
                        plugin.getLogger().warning("No value in translation file for key "
                                + key + " was found. Regenerate language files?");
                    }
                }
            }
        }
    }
}
