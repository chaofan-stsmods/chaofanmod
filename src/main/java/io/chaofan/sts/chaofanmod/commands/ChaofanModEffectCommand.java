package io.chaofan.sts.chaofanmod.commands;

import basemod.BaseMod;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import basemod.helpers.ScreenPostProcessorManager;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import io.chaofan.sts.chaofanmod.vfx.DitheringEffect;
import io.chaofan.sts.chaofanmod.vfx.RetroEffect;
import io.chaofan.sts.chaofanmod.vfx.ShaderFadeEffect;

import java.util.*;

public class ChaofanModEffectCommand extends ConsoleCommand {
    private static boolean initialized = false;
    private static final Map<String, ScreenPostProcessor> effects = new HashMap<>();
    private static final Set<String> enabledEffects = new HashSet<>();

    public ChaofanModEffectCommand() {
        maxExtraTokens = 1;
        minExtraTokens = 1;
        simpleCheck = true;

        if (!initialized) {
            initialized = true;
            effects.put("dithering", new DitheringEffect());
            effects.put("retro", new RetroEffect());
        }
    }

    @Override
    protected ArrayList<String> extraOptions(String[] tokens, int depth) {
        return new ArrayList<>(effects.keySet());
    }

    @Override
    protected void execute(String[] args, int depth) {
        if (args.length > depth) {
            String effectName = args[depth];
            ScreenPostProcessor postProcessor = effects.get(effectName);
            if (postProcessor != null) {
                if (enabledEffects.contains(effectName)) {
                    enabledEffects.remove(effectName);
                    ScreenPostProcessorManager.removePostProcessor(postProcessor);
                    DevConsole.log("Effect " + effectName + " disabled.");
                } else {
                    enabledEffects.add(effectName);
                    ScreenPostProcessorManager.addPostProcessor(postProcessor);
                    DevConsole.log("Effect " + effectName + " enabled.");
                }
            } else if (effectName.equals("fade")) {
                ShaderFadeEffect screenProcessor = new ShaderFadeEffect();
                ScreenPostProcessorManager.addPostProcessor(screenProcessor);
                BaseMod.subscribe(new PostUpdateSubscriber() {
                    float progress = 0;
                    @Override
                    public void receivePostUpdate() {
                        progress += Gdx.graphics.getDeltaTime() / 2f;
                        if (progress < 1f) {
                            ShaderFadeEffect.progress = 1 - progress;
                        } else if (progress < 2f) {
                            ShaderFadeEffect.progress = 0f;
                        } else if (progress < 3f) {
                            ShaderFadeEffect.progress = progress - 2;
                        } else {
                            ShaderFadeEffect.progress = 1f;
                            ScreenPostProcessorManager.removePostProcessor(screenProcessor);
                            BaseMod.unsubscribeLater(this);
                        }
                    }
                });
            }
        }
    }
}
