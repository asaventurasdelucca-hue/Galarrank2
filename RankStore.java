package dev.galar.rank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Guarda o ranking de cada jogador em <mundo>/galarrank.json */
public final class RankStore {
    public static final int START_RANK = 10000;

    public static class PlayerRank {
        public int rank = START_RANK;
        public int badges = 0;
        public Set<String> defeated = new HashSet<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, PlayerRank> data = new HashMap<>();
    private static Path file;

    private RankStore() {}

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("galarrank.json");
        data = new HashMap<>();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Type type = new TypeToken<Map<String, PlayerRank>>() {}.getType();
                Map<String, PlayerRank> loaded = GSON.fromJson(reader, type);
                if (loaded != null) data = loaded;
            } catch (Exception e) {
                GalarRank.LOGGER.error("[GalarRank] erro ao ler galarrank.json", e);
            }
        }
    }

    public static void save() {
        if (file == null) return;
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            GalarRank.LOGGER.error("[GalarRank] erro ao salvar galarrank.json", e);
        }
    }

    public static PlayerRank get(UUID id) {
        return data.computeIfAbsent(id.toString(), k -> new PlayerRank());
    }

    public static void reset(UUID id) {
        data.put(id.toString(), new PlayerRank());
        save();
    }
}
