package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import io.chaofan.sts.chaofanmod.patches.ScreenFilterPatches;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import java.util.Iterator;

import static io.chaofan.sts.chaofanmod.ChaofanMod.*;

public class SpotLight extends CustomRelic {
    public static final String ID = makeId("relic.SpotLight");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/spot_light.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/spot_light.png"));

    public SpotLight() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
        ScreenFilterPatches.postProcessors.add(new SpotLightPostProcessor());
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
        for (Iterator<ScreenFilterPatches.PostProcessor> iterator = ScreenFilterPatches.postProcessors.iterator(); iterator.hasNext(); ) {
            ScreenFilterPatches.PostProcessor postProcessor = iterator.next();
            if (postProcessor instanceof SpotLightPostProcessor) {
                iterator.remove();
                break;
            }
        }
    }

    public static class SpotLightPostProcessor implements ScreenFilterPatches.PostProcessor {
        private static final ShaderProgram shader;

        static {
            shader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("spotlight.vs")).readString(),
                    Gdx.files.internal(getShaderPath("spotlight.fs")).readString());
            if (!shader.isCompiled()) {
                throw new RuntimeException(shader.getLog());
            }
        }

        @Override
        public void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera camera) {
            sb.end();

            sb.setShader(shader);
            sb.begin();
            shader.setUniform2fv("u_mouse", new float[] {InputHelper.mX, InputHelper.mY}, 0, 2);
            sb.setColor(Color.WHITE);

            sb.draw(frameTexture, 0, 0);
            ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
        }
    }
}
