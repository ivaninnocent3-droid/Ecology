
package com.innspectre.ecology.command;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.Vec3;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.herd.HerdRegistry;

public final class HerdDebugCommand {

    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

    private HerdDebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerCommand(dispatcher)
        );

        ServerTickEvents.END_SERVER_TICK.register(
                HerdDebugCommand::onServerTick
        );
    }

    private static void registerCommand(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("herd")
                        .then(Commands.literal("debug")
                                .executes(context -> toggleDebug(
                                        context.getSource().getPlayerOrException()
                                ))
                        )
        );
    }

    private static int toggleDebug(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (ENABLED_PLAYERS.remove(playerId)) {
            player.sendSystemMessage(
                    Component.literal("Herd debug highlighting disabled.")
            );
        } else {
            ENABLED_PLAYERS.add(playerId);
            player.sendSystemMessage(
                    Component.literal("Herd debug highlighting enabled.")
            );
        }

        return 1;
    }

    private static void onServerTick(MinecraftServer server) {
        // Refresh particles every 10 ticks rather than every tick.
        if (server.overworld().getGameTime() % 10 != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ENABLED_PLAYERS.contains(player.getUUID())) {
                continue;
            }

            highlightNearestHerd(player);
        }
    }

    private static void highlightNearestHerd(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        HerdRegistry registry = HerdRegistry.get(level);

        Herd nearestHerd = null;
        double nearestDistance = Double.MAX_VALUE;

        // Find the sheep herd whose centre is closest to the player.
        for (Herd herd : registry.herds()) {
            if (herd.species() != EntityTypes.SHEEP) {
                continue;
            }

            Optional<Vec3> centre = HerdManager.centre(level, herd);

            if (centre.isEmpty()) {
                continue;
            }

            double distance = player.position().distanceToSqr(centre.get());

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestHerd = herd;
            }
        }

        if (nearestHerd == null) {
            return;
        }

        // Highlight every currently loaded member of that herd.
        for (UUID memberId : nearestHerd.members()) {
            Entity entity = level.getEntity(memberId);

            if (!(entity instanceof Sheep sheep) || !sheep.isAlive()) {
                continue;
            }

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    false,
                    false,
                    sheep.getX(),
                    sheep.getY() + sheep.getBbHeight() + 0.2,
                    sheep.getZ(),
                    2,
                    0.2,
                    0.15,
                    0.2,
                    0.01
            );
        }
    }
}
