package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import basemod.interfaces.ScreenPostProcessor;
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
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.patches.MsWrithingPatches;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.*;

public class SpotLight extends CustomRelic implements MsWrithingPatches.DisableRelic {
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
        ChaofanMod.registerPostProcessor(new SpotLightPostProcessor());
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
        ChaofanMod.removePostProcessor(SpotLightPostProcessor.class);
    }

    @Override
    public void disableByMsWrithing() {
        --AbstractDungeon.player.energy.energy;
        ChaofanMod.removePostProcessor(SpotLightPostProcessor.class);
    }

    @Override
    public void enableByMsWrithing() {
        ChaofanMod.registerPostProcessor(new SpotLightPostProcessor());
    }

    public static class SpotLightPostProcessor implements ScreenPostProcessor {
        private static final ShaderProgram shader;

        static {
            shader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("common.vs")).readString(),
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
