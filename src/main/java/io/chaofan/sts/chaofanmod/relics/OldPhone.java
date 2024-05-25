package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.patches.MsWrithingPatches;
import io.chaofan.sts.chaofanmod.utils.MatrixHelper;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;
import io.chaofan.sts.chaofanmod.vfx.OldPhoneEffectV2;

import static io.chaofan.sts.chaofanmod.ChaofanMod.*;

public class OldPhone extends CustomRelic implements MsWrithingPatches.DisableRelic {
    public static final String ID = makeId("relic.OldPhone");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/old_phone.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/old_phone.png"));

    public OldPhone() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
        ChaofanMod.registerPostProcessor(createEffect(true));
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
        ChaofanMod.removePostProcessor(getEffectClass());
    }

    @Override
    public void disableByMsWrithing() {
        --AbstractDungeon.player.energy.energy;
        ChaofanMod.removePostProcessor(getEffectClass());
    }

    @Override
    public void enableByMsWrithing() {
        ChaofanMod.registerPostProcessor(createEffect(true));
    }

    public static ScreenPostProcessor createEffect(boolean waitForLoading) {
        if (useOldPhoneV2) {
            return new OldPhoneEffectV2(waitForLoading);
        } else {
            return new OldPhonePostProcessor();
        }
    }

    public static Class<? extends ScreenPostProcessor> getEffectClass() {
        if (useOldPhoneV2) {
            return OldPhoneEffectV2.class;
        } else {
            return OldPhonePostProcessor.class;
        }
    }

    public static class OldPhonePostProcessor implements ScreenPostProcessor {
        private static final Texture screen;
        private static final Texture screenHighlight;
        private static final Matrix4 projMat;
        private static final ShaderProgram shader;

        static {
            screen = ImageMaster.loadImage(getImagePath("ui/screen.png"));
            screenHighlight = ImageMaster.loadImage(getImagePath("ui/screen_highlight.png"));
            shader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("common.vs")).readString(),
                    Gdx.files.internal(getShaderPath("screen.fs")).readString());
            if (!shader.isCompiled()) {
                throw new RuntimeException(shader.getLog());
            }
            // LT 0, 38   RT 1918, -18
            // LB 126,913 RT 1747, 1154
            projMat = MatrixHelper.createProjMatrix(
                    screen.getWidth(), screen.getHeight(),
                    Settings.WIDTH, Settings.HEIGHT,
                    0, 38,
                    1918, -18,
                    126, 913,
                    1747, 1154
            );
        }

        @Override
        public void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera camera) {
            sb.end();

            int oldSrcFunc = sb.getBlendSrcFunc();
            int oldDstFunc = sb.getBlendDstFunc();

            sb.setShader(shader);
            sb.begin();
            sb.setColor(Color.WHITE);

            sb.setProjectionMatrix(projMat);
            sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
            sb.draw(frameTexture, 0, 0);

            if (screenHighlight != null) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                sb.draw(screenHighlight, 0, 0, Settings.WIDTH, Settings.HEIGHT);
            }

            ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            sb.setProjectionMatrix(camera.combined);
            sb.draw(screen, 0, 0, Settings.WIDTH, Settings.HEIGHT);

            sb.setBlendFunction(oldSrcFunc, oldDstFunc);
        }
    }
}
