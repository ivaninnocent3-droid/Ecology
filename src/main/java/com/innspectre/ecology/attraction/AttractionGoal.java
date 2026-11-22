
package com.innspectre.ecology.attraction;

import java.util.EnumSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.innspectre.ecology.Ecology;
import com.innspectre.ecology.attraction.AttractionManager.Signal;
import com.innspectre.ecology.attraction.AttractionManager.Type;
import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionState;
import com.innspectre.ecology.herd.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

public class AttractionGoal extends Goal {

    private static final double DETECTION_RANGE = 16.0;
    private static final double FOLLOW_SPEED = 1.1;

    private final PathfinderMob animal;
    private final AttractionProfile profile;

    private Player target;
    private AttractionManager.Type signalType;

    private boolean directAttraction;
    private boolean broadcasting;

    private long nextCheckTime;
    private long nextPathUpdate;

    public AttractionGoal(PathfinderMob animal, AttractionProfile profile) {
        this.animal = animal;
        this.profile = profile;
    }

    @Override
    public boolean canUse() {
        long now = animal.level().getGameTime();

        if (now < nextCheckTime) {
            return false;
        }

        nextCheckTime = now + 10L;

        if (!(animal.level() instanceof ServerLevel level)) {
            return false;
        }

        // First check for a player the animal can see itself.
        Player visiblePlayer = findVisiblePlayer(level);

        if (visiblePlayer != null) {
            AttractionManager.Type type =
                    getDirectAttractionType(visiblePlayer);

            if (type != null) {
                target = visiblePlayer;
                signalType = type;
                directAttraction = true;

                Optional<Herd> herd = HerdManager.getHerd(animal);

                broadcasting = herd.isPresent()
                        && type != null;

                return true;
            }
        }

        // Otherwise, check whether another animal has attracted the herd.
        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isEmpty()) {
            return false;
        }

        List<AttractionManager.Signal> signals =
                AttractionManager.getActiveSignals(level, herd.get());

        for (AttractionManager.Signal signal : signals) {
            Player player = level.getPlayerByUUID(signal.playerId());

            if (player == null || !canRespondToSignal(player, signal.type())) {
                continue;
            }

            target = player;
            signalType = signal.type();
            directAttraction = false;
            broadcasting = false;

            return true;
        }

        return false;
    }

    @Override
    public void start() {
        nextPathUpdate = 0L;
        updateMovement();
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()
                || target.level() != animal.level()) {
            return false;
        }

        if (directAttraction) {
            return animal.distanceToSqr(target)
                    <= DETECTION_RANGE * DETECTION_RANGE
                    && animal.hasLineOfSight(target)
                    && getDirectAttractionType(target) == signalType;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return false;
        }

        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isEmpty()) {
            return false;
        }

        return AttractionManager.isActive(
                level,
                herd.get(),
                target.getUUID(),
                signalType
        ) && canRespondToSignal(target, signalType);
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        animal.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Only the animal that noticed the player broadcasts the signal.
        if (directAttraction && broadcasting
                && animal.level() instanceof ServerLevel level) {

            HerdManager.getHerd(animal).ifPresent(herd ->
                    AttractionManager.publish(
                            level,
                            herd,
                            target.getUUID(),
                            animal.getUUID(),
                            signalType
                    )
            );
        }

        if (animal.level().getGameTime() >= nextPathUpdate) {
            updateMovement();
        }
    }

    @Override
    public void stop() {
        if (broadcasting) {
            AttractionManager.removeSource(animal.getUUID());
        }

        animal.getNavigation().stop();

        target = null;
        signalType = null;
        directAttraction = false;
        broadcasting = false;
    }

    private void updateMovement() {
        if (target == null) {
            return;
        }

        animal.getNavigation().moveTo(target, FOLLOW_SPEED);
        nextPathUpdate = animal.level().getGameTime() + 10L;
    }

    private Player findVisiblePlayer(ServerLevel level) {
        return level.getEntitiesOfClass(
                        Player.class,
                        animal.getBoundingBox().inflate(DETECTION_RANGE),
                        player -> player.isAlive()
                                && !player.isSpectator()
                                && animal.hasLineOfSight(player)
                )
                .stream()
                .min(Comparator.comparingDouble(animal::distanceToSqr))
                .orElse(null);
    }

    /**
     * Returns the attraction type if this animal should approach
     * a player directly, otherwise null.
     */
    private AttractionManager.Type getDirectAttractionType(
            Player player
    ) {
        RecognitionState state =
                RecognitionManager.get(animal, player.getUUID());

        if (isHoldinganimalFood(player) && acceptsFoodAttraction(state)) {
            return AttractionManager.Type.FOOD;
        }

        if (meetsThreshold(state, profile.socialRecognition())) {
            return AttractionManager.Type.SOCIAL;
        }

        if (meetsThreshold(state, profile.naturalRecognition())) {
            return AttractionManager.Type.SOCIAL;
        }

        return null;
    }

    private boolean canRespondToSignal(
            Player player,
            AttractionManager.Type type
    ) {
        RecognitionState state =
                RecognitionManager.get(animal, player.getUUID());

        return switch (type) {
            case FOOD -> acceptsFoodAttraction(state);
            case SOCIAL -> meetsThreshold(state, profile.naturalRecognition());
        };
    }

    private boolean acceptsFoodAttraction(RecognitionState state) {
        // An unfamiliar animal can be attracted, as can one
        // that has a neutral or positive relationship.
        return state.value() == 0 || state.relationship() >= 0;
    }

    private boolean meetsThreshold(
            RecognitionState state,
            int threshold
    ) {
        return state.value() >= threshold
                && state.relationship() >= threshold;
    }

    private boolean isHoldinganimalFood(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.is(profile.foodTag())
                || offHand.is(profile.foodTag());
    }
}
