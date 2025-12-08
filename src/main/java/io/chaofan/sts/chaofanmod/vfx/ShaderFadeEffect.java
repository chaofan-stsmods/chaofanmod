package io.chaofan.sts.chaofanmod.vfx;

import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ShaderHelper;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class ShaderFadeEffect implements ScreenPostProcessor {
    private static final Texture noise = new Texture(getImagePath("ui/noise.png"));
    private static ShaderProgram shader;

    public static float progress = 0;

    @Override
    public void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera orthographicCamera) {
        if (shader == null) {
            shader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("common.vs")).readString(),
                    Gdx.files.internal(getShaderPath("fade.fs")).readString());
            if (!shader.isCompiled()) {
                throw new RuntimeException(shader.getLog());
            }

            noise.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            noise.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        }

        sb.end();

        sb.setShader(shader);
        noise.bind(1);
        frameTexture.getTexture().bind(0);
        sb.setColor(Color.WHITE);
        sb.begin();
        shader.setUniformi("u_noise", 1);
        shader.setUniformf("u_progress", progress);
        shader.setUniformf("u_screenSize", Settings.WIDTH, Settings.HEIGHT);

        sb.draw(frameTexture, 0, 0);
        ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
    }
}
