
package com.innspectre.ecology.command;

import com.innspectre.ecology.need.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.wolf.Wolf;

public class NeedCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("need")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (NeedType type : NeedType.values()) {
                            builder.suggest(type.id());
                        }
                        return builder.buildFuture();
                    })
                    .then(Commands.literal("get")
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();

                            var wolves = player.level().getEntitiesOfClass(
                                Wolf.class,
                                player.getBoundingBox().inflate(10)
                            );

                            if (wolves.isEmpty()) {
                                context.getSource().sendFailure(
                                    Component.literal("No wolves found within 10 blocks.")
                                );
                                return 0;
                            }

                            Wolf target = wolves.getFirst();

                            
                            NeedType type = getNeedType(
                                StringArgumentType.getString(context, "type")
                            );

                            int value = NeedManager.get(target, type);

                            context.getSource().sendSuccess(
                                () -> Component.literal(
                                    "Wolf " + type.id() + ": " + value
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

                                var wolves = player.level().getEntitiesOfClass(
                                    Wolf.class,
                                    player.getBoundingBox().inflate(10)
                                );

                                if (wolves.isEmpty()) {
                                    context.getSource().sendFailure(
                                        Component.literal("No wolves found within 10 blocks.")
                                    );
                                    return 0;
                                }

                                Wolf target = wolves.getFirst();

                                NeedType type = getNeedType(
                                    StringArgumentType.getString(context, "type")
                                );

                                int value = IntegerArgumentType.getInteger(context, "value");

                                NeedManager.set(target, type, value);

                                context.getSource().sendSuccess(
                                    () -> Component.literal(
                                        "Set wolf " + type.id() + " to " + value
                                    ),
                                    false
                                );

                                return 1;
                            })
                        )
                    )
                )
            );
        });
    }

    private static NeedType getNeedType(String id) {
        for (NeedType type : NeedType.values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown need: " + id);
    }
}
