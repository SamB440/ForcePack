package com.convallyria.forcepack.velocity.config;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class VelocityConfig {

    private final CommentedConfig config;
    private final File file;

    public VelocityConfig(final ForcePackVelocity plugin) {
        final File file = new File(plugin.getDataDirectory() + File.separator + "config.toml");
        CommentedFileConfig conf = CommentedFileConfig.of(file, TomlFormat.instance());
        conf.load();
        conf.close();

        this.config = conf;
        this.file = file;
    }

    public VelocityConfig(CommentedConfig config, File file) {
        this.config = config;
        this.file = file;
    }

    @Nullable
    public Object get(String path) {
        Object obj = config.get(path);
        if (obj instanceof CommentedConfig) {
            return getConfig(path);
        }
        return obj;
    }

    public CommentedConfig getConfig() {
        return config;
    }

    public VelocityConfig getConfig(String path) {
        return config.get(path) == null ? null : new VelocityConfig(config.get(path), file);
    }

    public List<String> getStringList(String path) {
        return config.getOrElse(path, new ArrayList<>());
    }

    public Map<String, Object> getMap(String path) {
        CommentedConfig section = config.get(path);
        if (section == null) return new HashMap<>();
        return section.valueMap();
    }

    public void setUnsafe(String path, Object value) {
        config.set(path, value);
    }

    public void remove(String path) {
        config.remove(path);
    }

    @Nullable
    public String getString(String path) {
        return config.get(path);
    }

    public String getString(String path, String def) {
        return config.getOrElse(path, def);
    }

    public boolean getBoolean(String path) {
        return config.get(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getOrElse(path, def);
    }

    public int getInt(String path) {
        return config.getOrElse(path, -1);
    }

    public long getLong(String path) {
        return config.get(path);
    }

    public long getLong(String path, long def) {
        return config.getOrElse(path, def);
    }

    public Collection<? extends String> getKeys() {
        return new HashSet<>(config.valueMap().keySet());
    }
}
