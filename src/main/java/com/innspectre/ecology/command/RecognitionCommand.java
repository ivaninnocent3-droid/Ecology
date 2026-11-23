
package com.innspectre.ecology.command;

import com.innspectre.ecology.recognition.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RecognitionCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("recognition")
                .then(Commands.literal("get")
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();

                        LivingEntity target = getTargetAnimal(player);

                            if (target == null) {
                                context.getSource().sendFailure(
                                    Component.literal("You are not looking at an animal within 10 blocks.")
                                );
                                return 0;
                            }

                        RecognitionState state = RecognitionManager.get(
                            target,
                            player.getUUID()
                        );

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Recognition: " + state.value()
                                + " | Relationship: " + state.relationship()
                            ),
                            false
                        );

                        return 1;
                    })
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();

                            LivingEntity target = getTargetAnimal(player);

                            if (target == null) {
                                context.getSource().sendFailure(
                                    Component.literal("You are not looking at an animal within 10 blocks.")
                                );
                                return 0;
                            }
                            int value = IntegerArgumentType.getInteger(context, "value");

                            RecognitionManager.set(
                                target,
                                player.getUUID(),
                                new RecognitionState(
                                    value,
                                    player.level().getGameTime(),
                                    RecognitionManager.get(target, player.getUUID()).relationship()
                                )
                            );

                            context.getSource().sendSuccess(
                                () -> Component.literal("Set recognition to " + value),
                                false
                            );

                            return 1;
                        })
                    )
                )
                .then(Commands.literal("relationship")
                    .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();

                            LivingEntity target = getTargetAnimal(player);

                            if (target == null) {
                                context.getSource().sendFailure(
                                    Component.literal("You are not looking at an animal within 10 blocks.")
                                );
                                return 0;
                            }
                            int value = IntegerArgumentType.getInteger(context, "value");

                            RecognitionState state = RecognitionManager.get(
                                target,
                                player.getUUID()
                            );

                            RecognitionManager.set(
                                target,
                                player.getUUID(),
                                new RecognitionState(
                                    state.value(),
                                    state.lastUpdated(),
                                    value
                                )
                            );

                            context.getSource().sendSuccess(
                                () -> Component.literal("Set relationship to " + value),
                                false
                            );

                            return 1;
                        })
                    )
                )
                .then(Commands.literal("interact")
                    .then(Commands.argument("interaction", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (RecognitionInteraction interaction : RecognitionInteraction.values()) {
                                builder.suggest(interaction.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();

                            LivingEntity target = getTargetAnimal(player);

                            if (target == null) {
                                context.getSource().sendFailure(
                                    Component.literal("You are not looking at an animal within 10 blocks.")
                                );
                                return 0;
                            }

                            String interactionName = StringArgumentType.getString(context, "interaction");

                            RecognitionInteraction interaction;

                            try {
                                interaction = RecognitionInteraction.valueOf(
                                    interactionName.toUpperCase()
                                );
                            } catch (IllegalArgumentException exception) {
                                context.getSource().sendFailure(
                                    Component.literal("Unknown interaction: " + interactionName)
                                );
                                return 0;
                            }

                            RecognitionManager.applyInteraction(
                                target,
                                player.getUUID(),
                                interaction
                            );

                            RecognitionState state = RecognitionManager.get(
                                target,
                                player.getUUID()
                            );

                            context.getSource().sendSuccess(
                                () -> Component.literal(
                                    "Applied " + interaction.name().toLowerCase()
                                    + " | Recognition: " + state.value()
                                    + " | Relationship: " + state.relationship()
                                ),
                                false
                            );

                            return 1;
                        })
                    )
                )
            );
        }
        );
    }
    private static LivingEntity getTargetAnimal(Player player) {

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(10));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(10))
                .inflate(1.0);

        var hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity && entity != player,
                100.0
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }
}

