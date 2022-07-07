package io.chaofan.sts.chaofanmod.vfx;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.core.CardCrawlGame.ApplyScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.ArrayList;
import java.util.List;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class GlassEffect extends AbstractGameEffect {

    private static final Texture glass1 = ImageMaster.loadImage(getImagePath("vfx/glass1.png"));
    private static final Texture glass2 = ImageMaster.loadImage(getImagePath("vfx/glass2.png"));
    private static final Texture glass3 = ImageMaster.loadImage(getImagePath("vfx/glass3.png"));
    private static final Texture[] glasses = new Texture[] { glass1, glass2, glass3 };

    private static ShaderProgram glassShader;

    private final List<Glass> glassList = new ArrayList<>();

    public GlassEffect() {
        for (int y = 0; y < Settings.HEIGHT + 200; y += 200) {
            float ty = (float) (y - 50 + Math.random() * 200);
            for (int x = 0; x < Settings.WIDTH + 200; x += 200) {
                float tx = (float) (x - 50 + Math.random() * 200);
                Glass g = new Glass();
                g.x = tx;
                g.y = ty;
                g.rotation = (float) (Math.random() * 360);
                g.rotationSpeed = (float) ((Math.random() - 0.5) * 10);
                g.xSpeed = (float) ((Math.random() - 0.5) * 100);
                g.ySpeed = (float) ((Math.random() - 0.5) * 20);
                g.isScaleX = Math.random() > 0.5;
                g.scale = (float) (0.6 + (Math.random() - 0.5) * 0.2) * Settings.scale;
                g.scaleRotation = (float) (Math.random() * 360);
                g.scaleRotationSpeed = (float) ((Math.random() - 0.5) * 20);
                g.texture = new TextureRegion(glasses[(int) Math.floor(Math.random() * glasses.length)]);
                glassList.add(g);
            }
        }
    }

    @Override
    public void update() {
        for (Glass glass : glassList) {
            glass.update();
        }

        if (glassList.stream().allMatch(glass -> glass.y < -Settings.HEIGHT)) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (glassShader == null) {
            glassShader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("glass.vs")).readString(),
                    Gdx.files.internal(getShaderPath("glass.fs")).readString());
            if (!glassShader.isCompiled()) {
                throw new RuntimeException(glassShader.getLog());
            }
        }

        sb.end();

        TextureRegion region = getScreenAsTexture(sb);

        sb.setShader(glassShader);
        sb.setColor(Color.WHITE);
        sb.begin();

        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (Glass glass : glassList) {
            glass.render(sb, region);
        }

        ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);

/*
        sb.setShader(glassShader);
        glass.bind(1);

        region.getTexture().bind(0);
        sb.setColor(Color.WHITE);
        sb.begin();
        glassShader.setUniformi("u_glass", 1);
        sb.draw(region, 0, 0);
        ShaderHelper.setShader(sb, );
        sb.setShader(null);*/
    }

    @Override
    public void dispose() {

    }

    private TextureRegion getScreenAsTexture(SpriteBatch sb) {
        FrameBuffer buffer = ReflectionHacks.getPrivate(null, ApplyScreenPostProcessor.class, "secondaryFrameBuffer");
        buffer.begin();
        sb.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        sb.setColor(Color.WHITE);
        sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
        sb.draw(ReflectionHacks.<TextureRegion>getPrivate(null, ApplyScreenPostProcessor.class, "primaryFboRegion"), 0, 0);
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sb.end();
        buffer.end();
        return ReflectionHacks.getPrivate(null, ApplyScreenPostProcessor.class, "secondaryFboRegion");
    }

    private static class Glass {
        float x;
        float y;
        TextureRegion texture;
        float rotation;
        float scale;
        float scaleRotation;
        boolean isScaleX;
        float rotationSpeed;
        float scaleRotationSpeed;
        float xSpeed;
        float ySpeed;

        public void render(SpriteBatch sb, TextureRegion region) {
            sb.flush();
            region.getTexture().bind(1);
            texture.getTexture().bind(0);

            glassShader.setUniformi("u_background", 1);
            float scaleX = isScaleX ? MathUtils.sinDeg(scaleRotation) * scale : scale;
            float scaleY = isScaleX ? scale : MathUtils.sinDeg(scaleRotation) * scale;
            sb.draw(texture,
                    x - texture.getRegionWidth() / 2f,
                    y - texture.getRegionHeight() / 2f,
                    texture.getRegionWidth() / 2f,
                    texture.getRegionHeight() / 2f,
                    texture.getRegionWidth(),
                    texture.getRegionHeight(),
                    scaleX,
                    scaleY,
                    rotation);
        }

        public void update() {
            float deltaTime = Gdx.graphics.getDeltaTime();
            x += xSpeed * deltaTime;
            y += ySpeed * deltaTime;
            ySpeed -= 2000 * deltaTime;
            rotation += rotationSpeed;
            scaleRotation += scaleRotationSpeed;
        }
    }
}
