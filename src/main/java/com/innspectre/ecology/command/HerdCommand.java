package com.innspectre.ecology.command;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.herd.HerdRegistry;

/**
 * Temporary development tool, like /hunger. Not meant to ship.
 *
 *   /herd form <radius>   make a herd from the nearest mob and its herdless neighbours
 *   /herd info            describe the herd of the nearest mob (within 16 blocks)
 *   /herd list            list every herd in this dimension
 *   /herd clear           dissolve every herd in this dimension
 *
 * No permission check is set, so anyone on the server can use it. Add one before sharing.
 */
public final class HerdCommand {

    private static final double INFO_RADIUS = 16.0;

    private HerdCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("herd")
                        .then(Commands.literal("form")
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 64.0))
                                        .executes(ctx -> form(
                                                ctx.getSource(),
                                                DoubleArgumentType.getDouble(ctx, "radius")))))
                        .then(Commands.literal("info")
                                .executes(ctx -> info(ctx.getSource())))
                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clear(ctx.getSource())))));
    }

    private static int form(CommandSourceStack source, double radius) {
        Optional<Mob> nearest = nearestMob(source, radius);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.literal("No mob within " + radius + " blocks."));
            return 0;
        }

        Optional<Herd> herd = HerdManager.formHerdAround(nearest.get(), radius);
        if (herd.isEmpty()) {
            source.sendFailure(Component.literal("Not enough herdless animals of that species nearby."));
            return 0;
        }

        send(source, "Herd ready: " + describe(source.getLevel(), herd.get()));
        return 1;
    }

    private static int info(CommandSourceStack source) {
        Optional<Mob> nearest = nearestMob(source, INFO_RADIUS);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.literal("No mob within " + INFO_RADIUS + " blocks."));
            return 0;
        }

        Optional<Herd> herd = HerdManager.getHerd(nearest.get());
        if (herd.isEmpty()) {
            send(source, "The nearest mob is not in a herd.");
            return 1;
        }

        send(source, describe(source.getLevel(), herd.get()));
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        List<Herd> herds = List.copyOf(HerdRegistry.get(level).herds());

        if (herds.isEmpty()) {
            send(source, "No herds in this dimension.");
            return 0;
        }

        send(source, herds.size() + " herd(s):");
        for (Herd herd : herds) {
            send(source, " - " + describe(level, herd));
        }
        return herds.size();
    }

    private static int clear(CommandSourceStack source) {
        HerdRegistry registry = HerdRegistry.get(source.getLevel());
        List<Herd> herds = List.copyOf(registry.herds());
        for (Herd herd : herds) {
            registry.dissolve(herd.id());
        }
        send(source, "Dissolved " + herds.size() + " herd(s).");
        return herds.size();
    }

    // ---- helpers ----

    private static Optional<Mob> nearestMob(CommandSourceStack source, double radius) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        return level.getEntitiesOfClass(Mob.class, new AABB(pos, pos).inflate(radius), Mob::isAlive)
                .stream()
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(pos)));
    }

    private static String describe(ServerLevel level, Herd herd) {
        String id = herd.id().toString().substring(0, 8);
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(herd.species()).toString();
        int loaded = HerdManager.loadedMembers(level, herd);
        String centre = HerdManager.centre(level, herd)
                .map(c -> String.format("%.0f, %.0f, %.0f", c.x, c.y, c.z))
                .orElse("unknown");
        return id + " | " + species + " | " + herd.size() + " members (" + loaded + " loaded) | centre " + centre;
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}