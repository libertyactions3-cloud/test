package eu.kotori.justTeams.config;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Small dependency-free server configuration for JustTeams.
 *
 * The bank intentionally uses item IDs from this configuration rather than a
 * money balance. Unknown item IDs are ignored.
 */
public final class JustTeamsConfig {
    private static final String DEFAULT_CURRENCY_ITEMS =
            "minecraft:emerald,minecraft:emerald_block,minecraft:deepslate_emerald_ore";

    private final Path file;
    private final Properties properties = new Properties();
    private Set<Item> currencyItems = Set.of();

    public JustTeamsConfig(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        this.file = configDirectory.resolve("justteams.properties");
        load();
    }

    public void load() throws IOException {
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
        } else {
            properties.setProperty("bank.enabled", "true");
            properties.setProperty("bank.currency-items", DEFAULT_CURRENCY_ITEMS);
            save();
        }

        currencyItems = parseCurrencyItems(properties.getProperty("bank.currency-items", DEFAULT_CURRENCY_ITEMS));
    }

    public void save() throws IOException {
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "JustTeams Fabric configuration");
        }
    }

    public boolean isBankEnabled() {
        return Boolean.parseBoolean(properties.getProperty("bank.enabled", "true"));
    }

    public Set<Item> getCurrencyItems() {
        return currencyItems;
    }

    public Path getFile() {
        return file;
    }

    private static Set<Item> parseCurrencyItems(String value) {
        Set<Item> result = new LinkedHashSet<>();
        for (String rawId : value.split(",")) {
            String idText = rawId.trim();
            if (idText.isEmpty()) continue;

            Identifier id;
            try {
                id = Identifier.of(idText);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (!Registries.ITEM.containsId(id)) continue;
            result.add(Registries.ITEM.get(id));
        }
        return Set.copyOf(result);
    }
}
