package io.chaofan.sts.chaofanmod.vfx;

import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ShaderHelper;

import java.util.Arrays;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class RetroEffect implements ScreenPostProcessor {

    private static ShaderProgram retroShader;

    @Override
    public void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera orthographicCamera) {

        if (retroShader == null) {
            retroShader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("retro.vs")).readString(),
                    Gdx.files.internal(getShaderPath("retro.fs")).readString());
            if (!retroShader.isCompiled()) {
                throw new RuntimeException(retroShader.getLog());
            }
        }

        float deltaTime = Gdx.graphics.getDeltaTime();

        sb.end();

        sb.setShader(retroShader);
        sb.setColor(Color.WHITE);
        sb.begin();

        /*
uniform vec2 curvature;
uniform vec2 screenResolution;
uniform vec2 scanLineOpacity;
uniform float vignetteOpacity;
uniform float brightness;
uniform float vignetteRoundness;
         */

        int width = 800;
        retroShader.setUniformf("curvature", 3.0f, 3.0f);
        retroShader.setUniformf("screenResolution", width, (float) (width * Settings.HEIGHT / Settings.WIDTH));
        retroShader.setUniformf("scanLineOpacity", 1, 1);
        retroShader.setUniformf("vignetteOpacity", 1);
        retroShader.setUniformf("brightness", 2);
        retroShader.setUniformf("vignetteRoundness", 2.0f);
        sb.draw(frameTexture, 0, 0);
        ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
    }
}
