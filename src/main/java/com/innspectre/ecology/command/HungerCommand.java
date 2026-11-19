
package com.innspectre.ecology.command;

import com.innspectre.ecology.need.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.sheep.Sheep;

public class HungerCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("hunger")
                .then(Commands.literal("get")
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();

                        var sheep = player.level().getEntitiesOfClass(
                            Sheep.class,
                            player.getBoundingBox().inflate(10)
                        );

                        if (sheep.isEmpty()) {
                            context.getSource().sendFailure(
                                Component.literal("No sheep found within 10 blocks.")
                            );
                            return 0;
                        }

                        Sheep target = sheep.getFirst();
                        int hunger = NeedManager.get(target, NeedType.HUNGER);

                        context.getSource().sendSuccess(
                            () -> Component.literal("Sheep hunger: " + hunger),
                            false
                        );

                        return 1;
                    })
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();

                            var sheep = player.level().getEntitiesOfClass(
                                Sheep.class,
                                player.getBoundingBox().inflate(10)
                            );

                            if (sheep.isEmpty()) {
                                context.getSource().sendFailure(
                                    Component.literal("No sheep found within 10 blocks.")
                                );
                                return 0;
                            }

                            Sheep target = sheep.getFirst();
                            int value = IntegerArgumentType.getInteger(context, "value");

                            NeedManager.set(target, NeedType.HUNGER, value);

                            context.getSource().sendSuccess(
                                () -> Component.literal("Set sheep hunger to " + value),
                                false
                            );

                            return 1;
                        })
                    )
                )
            );
        });
    }
}
