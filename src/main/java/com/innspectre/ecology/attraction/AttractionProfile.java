package com.innspectre.ecology.attraction;

import net.minecraft.world.item.Item;

import com.innspectre.ecology.Ecology;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

public record AttractionProfile(
    TagKey<Item> foodTag,
    int naturalRecognition,
    int socialRecognition
) {
    public static AttractionProfile sheep() {
        return new AttractionProfile(
            TagKey.create(Registries.ITEM, Ecology.id("sheep_food")),
            20,
            60
        );
    }
}