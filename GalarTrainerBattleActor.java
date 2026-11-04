package dev.galar.rank;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * A real, positioned NPC battle actor for the Cobblemon battle engine.
 * TrainerBattleActor has no EntityBackedBattleActor interface, so its
 * ActiveBattlePokemon#getSendOutPosition returns null, leaving its Pokemon
 * without a physical send-out position in the world.
 */
public final class GalarTrainerBattleActor extends AIBattleActor
        implements EntityBackedBattleActor<LivingEntity> {

    private final String trainerName;
    private final LivingEntity entity;
    private final Vec3 initialPos;

    public GalarTrainerBattleActor(String trainerName, UUID uuid,
                                   List<BattlePokemon> pokemon, BattleAI ai,
                                   LivingEntity entity, Vec3 initialPos) {
        super(uuid, pokemon, ai);
        this.trainerName = trainerName;
        this.entity = entity;
        this.initialPos = initialPos;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public Vec3 getInitialPos() {
        return initialPos;
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(trainerName);
    }

    @Override
    public MutableComponent nameOwned(String name) {
        return Component.literal(trainerName + " - " + name);
    }

    @Override
    public ActorType getType() {
        return ActorType.NPC;
    }
}
