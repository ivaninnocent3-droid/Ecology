package com.innspectre.ecology.client.animation;

public interface AnimalActionRenderStateAccess {

    void ecology$setAction(byte action, float progress);

    byte ecology$getAction();

    float ecology$getActionProgress();
}