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
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.*;

public class OldPhone extends CustomRelic {
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
        ChaofanMod.registerPostProcessor(new OldPhonePostProcessor());
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
        ChaofanMod.removePostProcessor(OldPhonePostProcessor.class);
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
                    Gdx.files.internal(getShaderPath("screen.vs")).readString(),
                    Gdx.files.internal(getShaderPath("screen.fs")).readString());
            if (!shader.isCompiled()) {
                throw new RuntimeException(shader.getLog());
            }
            // LT 0, 38   RT 1918, -18
            // LB 126,913 RT 1747, 1154
            projMat = createProjMatrix(
                    screen.getWidth(), screen.getHeight(),
                    0, 38,
                    1918, -18,
                    126, 913,
                    1747, 1154
            );
        }

        public static Matrix4 createProjMatrix(
                float imageWidth, float imageHeight,
                float leftTopX, float leftTopY,
                float rightTopX, float rightTopY,
                float leftBottomX, float leftBottomY,
                float rightBottomX, float rightBottomY) {
            float[] u = {leftTopX, rightTopX, leftBottomX, rightBottomX};
            float[] v = {leftTopY, rightTopY, leftBottomY, rightBottomY};
            //float[] u = {0, 1920, 0, 1920};
            //float[] v = {0, 0, 1080, 1080};
            float[] x = {0, Settings.WIDTH, 0, Settings.WIDTH};
            float[] y = {Settings.HEIGHT, Settings.HEIGHT, 0, 0};

            for (int i = 0; i < 4; i++) {
                u[i] = u[i] / imageWidth * 2 - 1;
                v[i] = (imageHeight - v[i]) / imageHeight * 2 - 1;
            }

            float[][] mat = new float[12][12];
            float[] result = new float[12];
            for (int i = 0, j = 0; i < 4; i++, j+=3) {
                mat[j][0] = x[i];
                mat[j][1] = y[i];
                mat[j][2] = 1;
                mat[j][8 + i] = -u[i];
                mat[j + 1][3] = x[i];
                mat[j + 1][4] = y[i];
                mat[j + 1][5] = 1;
                mat[j + 1][8 + i] = -v[i];
                mat[j + 2][6] = x[i];
                mat[j + 2][7] = y[i];
                mat[j + 2][8 + i] = -1;
                result[j + 2] = -1;
            }

            for (int i = 0; i < mat.length; i++) {
                if (mat[i][i] == 0) {
                    for (int j = i + 1; j < mat.length; j++) {
                        if (mat[j][i] != 0) {
                            float[] tmp = mat[i];
                            mat[i] = mat[j];
                            mat[j] = tmp;
                            float tmp2 = result[i];
                            result[i] = result[j];
                            result[j] = tmp2;
                            break;
                        }
                    }
                }
                for (int j = i + 1; j < mat.length; j++) {
                    if (mat[j][i] == 0) {
                        continue;
                    }

                    float scale = mat[j][i] / mat[i][i];
                    for (int k = 0; k < mat[j].length; k++) {
                        mat[j][k] = mat[j][k] - scale * mat[i][k];
                    }
                    result[j] = result[j] - scale * result[i];
                }
            }

            for (int i = mat.length - 1; i >= 0; i--) {
                for (int j = i - 1; j >= 0; j--) {
                    if (mat[j][i] == 0) {
                        continue;
                    }

                    float scale = mat[j][i] / mat[i][i];
                    for (int k = 0; k < mat[j].length; k++) {
                        mat[j][k] = mat[j][k] - scale * mat[i][k];
                    }
                    result[j] = result[j] - scale * result[i];
                }
            }

            for (int i = mat.length - 1; i >= 0; i--) {
                result[i] /= mat[i][i];
                mat[i][i] = 1;
            }

            return new Matrix4(new float[] {
                    result[0], result[3], 0, result[6],
                    result[1], result[4], 0, result[7],
                    0, 0, 0, 0,
                    result[2], result[5], 0, 1
            });
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

            ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            sb.setProjectionMatrix(camera.combined);
            sb.draw(screen, 0, 0, Settings.WIDTH, Settings.HEIGHT);

            if (screenHighlight != null) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                sb.draw(screenHighlight, 0, 0, Settings.WIDTH, Settings.HEIGHT);
            }
            sb.setBlendFunction(oldSrcFunc, oldDstFunc);
        }
    }
}
