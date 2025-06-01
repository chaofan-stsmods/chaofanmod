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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ShaderHelper;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class DitheringEffect implements ScreenPostProcessor {
    private static final Texture ditheringPattern = new Texture(getImagePath("ui/dithering_pattern.png"));
    private static ShaderProgram shader;

    @Override
    public void postProcess(SpriteBatch sb, TextureRegion frameTexture, OrthographicCamera orthographicCamera) {
        if (shader == null) {
            shader = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("common.vs")).readString(),
                    Gdx.files.internal(getShaderPath("dithering.fs")).readString());
            if (!shader.isCompiled()) {
                throw new RuntimeException(shader.getLog());
            }

            ditheringPattern.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            ditheringPattern.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        }

        sb.end();

        sb.setShader(shader);
        ditheringPattern.bind(1);
        frameTexture.getTexture().bind(0);
        sb.setColor(Color.WHITE);
        sb.begin();
        shader.setUniformi("u_ditheringPattern", 1);
        shader.setUniformf("u_screenSize", Settings.WIDTH, Settings.HEIGHT);

        sb.draw(frameTexture, 0, 0);
        ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
    }
}
