package io.chaofan.sts.chaofanmod.vfx;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.core.CardCrawlGame.ApplyScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class CutEffect extends AbstractGameEffect {

    public static final float DURATION = 1.3f;

    private static ShaderProgram cutShader;
    private final List<Cut> cuts = new ArrayList<>();

    private final float[] points = new float[20];
    private final float[] direction = new float[10];
    private final float[] progress = new float[10];

    private static class Cut {
        float x;
        float y;
        float direction;
        float speed;
        float progress;
    }

    public CutEffect() {
        this.duration = this.startingDuration = DURATION;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        if (Math.random() < 0.1f + (this.startingDuration - this.duration) && cuts.size() < 10) {
            Cut cut = new Cut();
            cut.speed = (float) (1.0 + (Math.random() - 0.5) * 0.3);
            cut.x = (float) (Math.random() * Settings.WIDTH / 2f + Settings.WIDTH / 2f);
            cut.y = (float) (Math.random() * Settings.HEIGHT);
            cut.direction = (float) (Math.random() * Math.PI * 2);
            cut.progress = 0;
            cuts.add(cut);
        }

        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        if (cutShader == null) {
            cutShader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("cut.vs")).readString(),
                    Gdx.files.internal(getShaderPath("cuts.fs")).readString());
            if (!cutShader.isCompiled()) {
                throw new RuntimeException(cutShader.getLog());
            }
        }

        float deltaTime = Gdx.graphics.getDeltaTime();

        Arrays.fill(direction, -1);

        for (int i = 0; i < cuts.size(); i++) {
            Cut cut = cuts.get(i);
            points[2 * i] = cut.x;
            points[2 * i + 1] = cut.y;
            direction[i] = cut.direction;
            progress[i] = cut.progress;
            cut.progress += deltaTime * cut.speed;
            if (cut.progress > 1) {
                cut.progress = 1;
            }
        }

        sb.end();

        TextureRegion region = getScreenAsTexture(sb);

        sb.setShader(cutShader);
        sb.setColor(Color.WHITE);
        sb.begin();
        cutShader.setUniform2fv("u_point", points, 0, 20);
        cutShader.setUniform1fv("u_direction", direction, 0, 10);
        cutShader.setUniform1fv("u_progress", progress, 0, 10);
        sb.draw(region, 0, 0);
        ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
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
}
