package io.chaofan.sts.chaofanmod.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import javassist.CtBehavior;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class ScreenFilterPatches {
    public static boolean enable = false;

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class GameRenderPatch {
        private static FrameBuffer fbo;
        private static TextureRegion fboRegion;
        private static Texture screen;
        private static Texture screenHighlight;
        private static Matrix4 projMat;
        private static ShaderProgram shader;
        private static boolean usingFbo = false;

        @SpireInsertPatch(locator = StartLocator.class)
        public static void BeforeSbStart(CardCrawlGame __instance, SpriteBatch ___sb) {
            if (!enable) {
                return;
            }

            int width = Gdx.graphics.getWidth();
            int height = Gdx.graphics.getHeight();

            if (fbo == null) {
                fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
                fboRegion = new TextureRegion(fbo.getColorBufferTexture());
                fboRegion.flip(false, true);

                screen = ImageMaster.loadImage(getImagePath("ui/screen.png"));
                screenHighlight = ImageMaster.loadImage(getImagePath("ui/screen_highlight.png"));
                shader = new ShaderProgram(
                        Gdx.files.internal(getShaderPath("screen/vertexShader.vs")).readString(),
                        Gdx.files.internal(getShaderPath("screen/fragShader.fs")).readString());
                if (!shader.isCompiled()) {
                    throw new RuntimeException("Shader error");
                }
                projMat = createProjMatrix();
            }

            usingFbo = true;
            fbo.begin();
        }

        public static class StartLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(SpriteBatch.class, "begin");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }
        @SpireInsertPatch(locator = EndLocator.class)
        public static void BeforeSbEnd(CardCrawlGame __instance, SpriteBatch ___sb, OrthographicCamera ___camera) {
            if (!usingFbo) {
                return;
            }

            ___sb.end();
            fbo.end();

            ___sb.setShader(shader);
            ___sb.begin();
            ___sb.setColor(Color.WHITE);

            ___sb.setProjectionMatrix(projMat);
            ___sb.draw(fboRegion, 0, 0);

            ShaderHelper.setShader(___sb, ShaderHelper.Shader.DEFAULT);
            ___sb.setProjectionMatrix(___camera.combined);
            ___sb.draw(screen, 0, 0, Settings.WIDTH, Settings.HEIGHT);

            ___sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            ___sb.draw(screenHighlight, 0, 0, Settings.WIDTH, Settings.HEIGHT);
            ___sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            usingFbo = false;
        }

        public static class EndLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(SpriteBatch.class, "end");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }

        public static Matrix4 createProjMatrix() {
            // LT 0, 38   RT 1918, -18
            // LB 126,913 RT 1747, 1154
            float[] u = {0, 1918, 126, 1747};
            float[] v = {38, -18, 913, 1154};
            //float[] u = {0, 1920, 0, 1920};
            //float[] v = {0, 0, 1080, 1080};
            float[] x = {0, Settings.WIDTH, 0, Settings.WIDTH};
            float[] y = {Settings.HEIGHT, Settings.HEIGHT, 0, 0};

            for (int i = 0; i < 4; i++) {
                u[i] = u[i] / 1920 * 2 - 1;
                v[i] = (1080 - v[i]) / 1080 * 2 - 1;
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
    }

    @SpirePatch(clz = ShaderHelper.class, method = "initializeShaders")
    public static class ShaderHelperInitializeShadersPatch {
        @SpirePostfixPatch
        public static void Postfix() {
        }
    }
}
