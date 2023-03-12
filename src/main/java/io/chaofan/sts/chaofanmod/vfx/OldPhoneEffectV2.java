package io.chaofan.sts.chaofanmod.vfx;

import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class OldPhoneEffectV2 implements ScreenPostProcessor {

    private static final Texture screenHighlight;
    static {
        screenHighlight = ImageMaster.loadImage(getImagePath("ui/screen_highlight.png"));
    }

    private ModelBatch modelBatch;
    private Environment environment;
    private PerspectiveCamera cam;
    private AssetManager assets;
    private final Array<ModelInstance> instances = new Array<>();
    private volatile boolean loading;
    private volatile boolean waitForLoading;

    private float currentX = 0;
    private float currentY = 0;

    public OldPhoneEffectV2() {
        this(true);
    }

    public OldPhoneEffectV2(boolean waitForLoading) {
        create();
        this.waitForLoading = waitForLoading;
    }

    public void create() {
        modelBatch = new ModelBatch(new MyShaderProvider(this));
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.2f, -1f, -1f));

        cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0.7f, 5.7f, 3.3f);
        cam.lookAt(0.3f,5.0f, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        assets = new AssetManager();
        assets.load("chaofanmod/models/monitor/monitor.obj", Model.class);
        loading = true;
    }

    private void doneLoading() {
        Model ship = assets.get("chaofanmod/models/monitor/monitor.obj", Model.class);
        ModelInstance shipInstance = new ModelInstance(ship);
        instances.add(shipInstance);
        loading = false;
        waitForLoading = false;
    }

    private static float lerp(float start, float target) {
        if (start != target) {
            start = MathUtils.lerp(start, target, Gdx.graphics.getDeltaTime() * 9.0F);
            if (Math.abs(start - target) < 0.0005) {
                start = target;
            }
        }

        return start;
    }

    @Override
    public void postProcess(SpriteBatch sb, TextureRegion textureRegion, OrthographicCamera orthographicCamera) {
        sb.end();

        float x = (float)InputHelper.mX / Gdx.graphics.getWidth() * 2 - 1;
        float y = (float)InputHelper.mY / Gdx.graphics.getHeight() * 2 - 1;

        currentX = lerp(currentX, x);
        currentY = lerp(currentY, y);

        cam.position.set(0.7f - 0.1f * currentX, 5.7f - 0.07f * currentY, 3.3f);
        cam.lookAt(0.3f + 0.1f * currentX,5.0f + 0.07f * currentY, 0);
        cam.update();

        if (loading && assets.update()) {
            doneLoading();
        }

        if (waitForLoading) {
            sb.begin();
            sb.draw(textureRegion, 0, 0, Settings.WIDTH, Settings.HEIGHT);
            return;
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);

        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        if (instances.size > 0) {
            ModelInstance model = instances.get(0);
            for (Material m : model.materials) {
                if (m.id.equals("screen-show")) {
                    for (Attribute attribute : m) {
                        if (attribute.type == TextureAttribute.Diffuse) {
                            ((TextureAttribute) attribute).set(textureRegion);
                        }
                    }
                }
            }
        }

        modelBatch.begin(cam);
        modelBatch.render(instances, environment);
        modelBatch.end();

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        sb.begin();
    }

    static class MyShader extends DefaultShader {
        private final OldPhoneEffectV2 monitorRenderer;

        public MyShader(Renderable renderable, Config config, ShaderProgram shaderProgram, OldPhoneEffectV2 monitorRenderer) {
            super(renderable, config, shaderProgram);
            this.monitorRenderer = monitorRenderer;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            super.begin(camera, context);

            screenHighlight.bind(3);
            this.program.setUniformf("u_highLightOffset", this.monitorRenderer.currentX, this.monitorRenderer.currentY);
            this.program.setUniformi("u_highlightTexture", 3);
        }
    }

    static class MyShaderProvider extends DefaultShaderProvider {
        static ShaderProgram shaderProgram;
        static {
            shaderProgram = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("screen-3d.vs")).readString(),
                    Gdx.files.internal(getShaderPath("screen-3d.fs")).readString());
            if (!shaderProgram.isCompiled()) {
                throw new RuntimeException(shaderProgram.getLog());
            }
        }

        private final OldPhoneEffectV2 monitorRenderer;

        public MyShaderProvider(OldPhoneEffectV2 monitorRenderer) {
            this.monitorRenderer = monitorRenderer;
        }

        @Override
        public Shader getShader(Renderable renderable) {
            if (renderable.material.id.equals("screen-show")) {
                if (renderable.shader != null) {
                    return renderable.shader;
                }
                renderable.shader = new MyShader(renderable, this.config, shaderProgram, monitorRenderer);
                renderable.shader.init();
                return renderable.shader;
            }

            return super.getShader(renderable);
        }
    }
}
