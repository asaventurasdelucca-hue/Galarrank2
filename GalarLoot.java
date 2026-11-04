package dev.galar.rank;

import com.cobblemon.mod.common.api.events.drops.LootDroppedEvent;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Only cancels drops of battle copies made for GalarRank NPCs; wild loot stays untouched. */
public final class GalarLoot {
    private static final long TTL_MS = 2L * 60 * 60 * 1000;
    private static final Map<UUID, Long> TRAINER_POKEMON = new ConcurrentHashMap<>();

    public GalarLoot() {}

    public static void mark(List<BattlePokemon> team) {
        long now = System.currentTimeMillis();
        TRAINER_POKEMON.entrySet().removeIf(e -> now - e.getValue() > TTL_MS);
        for (BattlePokemon battlePokemon : team) {
            TRAINER_POKEMON.put(battlePokemon.getEffectedPokemon().getUuid(), now);
            // Depending on the Cobblemon build, a faint event may reference the original copy.
            TRAINER_POKEMON.put(battlePokemon.getOriginalPokemon().getUuid(), now);
        }
    }

    public static void unmark(List<BattlePokemon> team) {
        for (BattlePokemon b : team) {
            TRAINER_POKEMON.remove(b.getEffectedPokemon().getUuid());
            TRAINER_POKEMON.remove(b.getOriginalPokemon().getUuid());
        }
    }

    private static boolean isTrainerPokemon(PokemonEntity entity) {
        UUID id = entity.getPokemon().getUuid();
        Long when = TRAINER_POKEMON.get(id);
        if (when == null) return false;
        if (System.currentTimeMillis() - when > TTL_MS) {
            TRAINER_POKEMON.remove(id);
            return false;
        }
        return true;
    }

    /** Cobblemon custom loot table items. */
    public static void cobblemonLoot(LootDroppedEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemon && isTrainerPokemon(pokemon)) {
            event.cancel();
        }
    }

    /** Extra protection for vanilla/other mod drops tied to the same battle entity. */
    @SubscribeEvent
    public void livingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemon && isTrainerPokemon(pokemon)) {
            event.setCanceled(true);
        }
    }

    public static void clear() { TRAINER_POKEMON.clear(); }
}
