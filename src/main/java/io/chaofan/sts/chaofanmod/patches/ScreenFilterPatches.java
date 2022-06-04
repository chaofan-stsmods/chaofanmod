package io.chaofan.sts.chaofanmod.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import javassist.CtBehavior;

import java.util.ArrayList;
import java.util.List;

public class ScreenFilterPatches {
    public static boolean enable = true;

    public interface PostProcessor {
        void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera camera);
    }

    public static final List<PostProcessor> postProcessors = new ArrayList<>();

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class GameRenderPatch {
        private static FrameBuffer primaryFrameBuffer;
        private static FrameBuffer secondaryFrameBuffer;
        private static TextureRegion primaryFboRegion;
        private static TextureRegion secondaryFboRegion;
        private static boolean usingFbo = false;

        @SpireInsertPatch(locator = StartLocator.class)
        public static void BeforeSbStart(CardCrawlGame __instance, SpriteBatch ___sb) {
            if (!enable) {
                return;
            }

            if (primaryFrameBuffer == null) {
                initFrameBuffer();
            }

            if (CardCrawlGame.mode != CardCrawlGame.GameMode.GAMEPLAY && !postProcessors.isEmpty()) {
                postProcessors.clear();
            }

            if (!postProcessors.isEmpty()) {
                usingFbo = true;
                primaryFrameBuffer.begin();
            }
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
            primaryFrameBuffer.end();

            for (PostProcessor postProcessor : postProcessors) {
                FrameBuffer tempBuffer = primaryFrameBuffer;
                primaryFrameBuffer = secondaryFrameBuffer;
                secondaryFrameBuffer = tempBuffer;

                TextureRegion tempRegion = primaryFboRegion;
                primaryFboRegion = secondaryFboRegion;
                secondaryFboRegion = tempRegion;

                primaryFrameBuffer.begin();
                ___sb.begin();

                postProcessor.postProcess(___sb, secondaryFboRegion, ___camera);

                ___sb.end();
                primaryFrameBuffer.end();
            }

            ___sb.setShader(null);
            ___sb.begin();
            ___sb.setColor(Color.WHITE);

            ___sb.setProjectionMatrix(___camera.combined);
            ___sb.draw(primaryFboRegion, 0, 0, Settings.WIDTH, Settings.HEIGHT);

            usingFbo = false;
        }

        public static class EndLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(SpriteBatch.class, "end");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }

        private static void initFrameBuffer() {
            int width = Gdx.graphics.getWidth();
            int height = Gdx.graphics.getHeight();

            primaryFrameBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
            primaryFboRegion = new TextureRegion(primaryFrameBuffer.getColorBufferTexture());
            primaryFboRegion.flip(false, true);

            secondaryFrameBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
            secondaryFboRegion = new TextureRegion(secondaryFrameBuffer.getColorBufferTexture());
            secondaryFboRegion.flip(false, true);
        }
    }
}
