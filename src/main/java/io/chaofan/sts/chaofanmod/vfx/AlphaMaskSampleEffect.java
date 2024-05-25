package io.chaofan.sts.chaofanmod.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class AlphaMaskSampleEffect extends AbstractGameEffect {

    private final Texture boxFrame;
    private final Texture boxGlow;
    private final Texture boxGlowMultiplier;
    private final ShaderProgram shader;
    private float timer = 0;

    public AlphaMaskSampleEffect() {
        this.boxFrame = ImageMaster.loadImage(getImagePath("ui/box_frame.png"));
        this.boxGlow = ImageMaster.loadImage(getImagePath("ui/box_glow.png"));
        this.boxGlowMultiplier = ImageMaster.loadImage(getImagePath("ui/box_glow_multiplier.png"));
        this.shader = new ShaderProgram(
                Gdx.files.internal(getShaderPath("common.vs")).readString(),
                Gdx.files.internal(getShaderPath("alphaMask.fs")).readString());
        if (!shader.isCompiled()) {
            throw new RuntimeException(shader.getLog());
        }
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        timer += Gdx.graphics.getDeltaTime();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sb.setColor(Color.WHITE);
        float x = Settings.WIDTH / 2f;
        float y = Settings.HEIGHT / 2f + MathUtils.sin(timer * 0.4f) * 30f;
        float rotation = 10 * MathUtils.sin(0.23f + timer * 0.3f);
        sb.draw(boxFrame,
                x - boxFrame.getWidth() / 2f,
                y - boxFrame.getHeight() / 2f,
                boxFrame.getWidth() / 2f,
                boxFrame.getHeight() / 2f,
                boxFrame.getWidth(),
                boxFrame.getHeight(),
                scale,
                scale,
                rotation,
                0,
                0,
                boxFrame.getWidth(),
                boxFrame.getHeight(),
                false,
                false);
        sb.end();
        sb.setShader(shader);
        boxGlowMultiplier.bind(1);
        boxGlow.bind(0);
        color.a = timer - MathUtils.floor(timer);
        sb.setColor(this.color);
        sb.begin();
        shader.setUniformi("u_mask", 1);
        sb.draw(boxGlow,
                x - boxGlow.getWidth() / 2f,
                y - boxGlow.getHeight() / 2f,
                boxGlow.getWidth() / 2f,
                boxGlow.getHeight() / 2f,
                boxGlow.getWidth(),
                boxGlow.getHeight(),
                scale,
                scale,
                rotation,
                0,
                0,
                boxGlow.getWidth(),
                boxGlow.getHeight(),
                false,
                false);
        sb.flush();
        sb.end();
        sb.setShader(null);
        sb.begin();
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    }

    @Override
    public void dispose() {
        this.boxFrame.dispose();
        this.boxGlow.dispose();
        this.boxGlowMultiplier.dispose();
        this.shader.dispose();
    }

    @Override
    protected void finalize() {
        dispose();
    }
}
