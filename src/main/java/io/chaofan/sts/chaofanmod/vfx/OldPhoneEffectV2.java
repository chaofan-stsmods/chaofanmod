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
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import java.lang.ref.WeakReference;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;

public class OldPhoneEffectV2 implements ScreenPostProcessor {

    private static final Texture screenHighlight;
    static {
        screenHighlight = ImageMaster.loadImage(getImagePath("ui/screen_highlight.png"));
    }

    private static final FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Settings.WIDTH, Settings.HEIGHT, true, false);
    private static final TextureRegion frameBufferTexture = new TextureRegion(frameBuffer.getColorBufferTexture());
    public static final ShaderProgram glareShader;
    private static final ShaderProgram fxaaShader;
    static {
        frameBufferTexture.flip(false, true);

        glareShader = new ShaderProgram(
                Gdx.files.internal(getShaderPath("common.vs")).readString(),
                Gdx.files.internal(getShaderPath("glare.fs")).readString());
        if (!glareShader.isCompiled()) {
            throw new RuntimeException(glareShader.getLog());
        }

        fxaaShader = new ShaderProgram(
                Gdx.files.internal(getShaderPath("common.vs")).readString(),
                Gdx.files.internal(getShaderPath("fxaa.fs")).readString());
        if (!fxaaShader.isCompiled()) {
            throw new RuntimeException(fxaaShader.getLog());
        }
    }

    private EnvironmentProvider ep;
    private ModelBatch mainModelBatch;
    private ModelBatch secondaryModelBatch;
    private Environment environment;
    private PerspectiveCamera cam;
    private volatile boolean waitForLoading;
    private float currentX = 0;
    private float currentY = 0;

    private float positionX = 0;
    private float positionY = 0;
    private float lookAtX = 0;
    private float lookAtY = 0;

    public OldPhoneEffectV2(boolean waitForLoading) {
        create();
        this.waitForLoading = waitForLoading;
    }

    public void create() {
        ep = EnvironmentProvider.getInstance();

        mainModelBatch = new ModelBatch(new MainShaderProvider(this));
        secondaryModelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.2f, -1f, -1f));

        cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0.7f, 5.7f, 3.3f);
        cam.lookAt(0.3f,5.0f, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();
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

        float positionXTarget = 0.7f - 0.1f * x;
        float positionYTarget = 5.7f - 0.07f * y;
        float lookAtXTarget = 0.3f + 0.1f * x;
        float lookAtYTarget = 5.0f + 0.07f * y;

        currentX = lerp(currentX, x);
        currentY = lerp(currentY, y);
        positionX = lerp(positionX, positionXTarget);
        positionY = lerp(positionY, positionYTarget);
        lookAtX = lerp(lookAtX, lookAtXTarget);
        lookAtY = lerp(lookAtY, lookAtYTarget);

        if (ep.update() && waitForLoading) {
            waitForLoading = false;

            currentX = 2;
            currentY = 5;
            positionX = lookAtX = 0;
            positionY = lookAtY = 5;
        }

        cam.position.set(positionX, positionY, 3.3f);
        cam.lookAt(lookAtX, lookAtY, 0);
        cam.update();

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

        // Step 1. Monitor
        frameBuffer.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        frameBuffer.end();

        setModelTexture(frameBufferTexture, 0xffffffff);

        frameBuffer.begin();
        secondaryModelBatch.begin(cam);
        secondaryModelBatch.render(ep.instances, environment);
        secondaryModelBatch.end();
        frameBuffer.end();

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        sb.setShader(fxaaShader);
        sb.setColor(Color.WHITE);
        sb.begin();
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fxaaShader.setUniformf("resolution", Settings.WIDTH, Settings.HEIGHT);
        sb.draw(frameBufferTexture, 0, 0, Settings.WIDTH, Settings.HEIGHT);
        sb.end();

        // Step 2. Screen
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        setModelTexture(textureRegion, 0xFF);

        frameBuffer.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mainModelBatch.begin(cam);
        mainModelBatch.render(ep.instances, environment);
        mainModelBatch.end();
        frameBuffer.end();

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        sb.setShader(null);
        sb.setColor(Color.WHITE);
        sb.begin();
        sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        sb.draw(frameBufferTexture, 0, 0, Settings.WIDTH, Settings.HEIGHT);
        sb.end();

        // Step 3. Glare
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        setModelTexture(textureRegion, 0xff);

        frameBuffer.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        secondaryModelBatch.begin(cam);
        secondaryModelBatch.render(ep.instances, environment);
        secondaryModelBatch.end();
        frameBuffer.end();

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        sb.setShader(glareShader);
        sb.setColor(Color.WHITE);
        sb.begin();
        sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        glareShader.setUniformf("resolution", Settings.WIDTH, Settings.HEIGHT);
        sb.draw(frameBufferTexture, 0, 0, Settings.WIDTH, Settings.HEIGHT);

        sb.end();

        // Reset sprite batch
        sb.setShader(null);
        sb.begin();
    }

    private void setModelTexture(TextureRegion screenTexture, int remainingOther) {
        if (ep.instances.size > 0) {
            ModelInstance model = ep.instances.get(0);
            for (Material m : model.materials) {
                if (m.id.equals("screen-show")) {
                    for (Attribute attribute : m) {
                        if (attribute.type == TextureAttribute.Diffuse) {
                            ((TextureAttribute) attribute).set(screenTexture);
                        }
                    }
                } else {
                    for (Attribute attribute : m) {
                        if (attribute.type == ColorAttribute.Diffuse) {
                            Color color = ((ColorAttribute) attribute).color;
                            color.set(remainingOther);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        mainModelBatch.dispose();
        secondaryModelBatch.dispose();
    }

    static class EnvironmentProvider {
        private static WeakReference<EnvironmentProvider> weakInstance = new WeakReference<>(null);

        public static synchronized EnvironmentProvider getInstance() {
            EnvironmentProvider result = weakInstance.get();
            if (result == null) {
                result = new EnvironmentProvider();
                weakInstance = new WeakReference<>(result);
            }
            return result;
        }

        public AssetManager assets;
        public final Array<ModelInstance> instances = new Array<>();
        public volatile boolean loading;

        public EnvironmentProvider() {
            assets = new AssetManager();
            assets.load("chaofanmod/models/monitor/monitor.obj", Model.class);
            loading = true;
        }

        public boolean update() {
            if (loading && assets.update()) {
                Model monitor = assets.get("chaofanmod/models/monitor/monitor.obj", Model.class);
                ModelInstance shipInstance = new ModelInstance(monitor);
                instances.add(shipInstance);
                loading = false;
                return true;
            }

            return !loading;
        }

        @Override
        protected void finalize() throws Throwable {
            assets.unload("chaofanmod/models/monitor/monitor.obj");
            assets.dispose();
        }
    }

    static class MyShader extends DefaultShader {
        private final Runnable onBegin;

        public MyShader(Renderable renderable, Config config, ShaderProgram shaderProgram, Runnable onBegin) {
            super(renderable, config, shaderProgram);
            this.onBegin = onBegin;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            super.begin(camera, context);
            this.onBegin.run();
        }
    }

    static class MainShaderProvider extends DefaultShaderProvider {
        private final OldPhoneEffectV2 monitorRenderer;
        private final ShaderProgram shaderProgram;
        private MyShader shader;

        public MainShaderProvider(OldPhoneEffectV2 monitorRenderer) {
            this.monitorRenderer = monitorRenderer;

            shaderProgram = new ShaderProgram(
                    Gdx.files.internal(getShaderPath("screen-3d.vs")).readString(),
                    Gdx.files.internal(getShaderPath("screen-3d.fs")).readString());
            if (!shaderProgram.isCompiled()) {
                throw new RuntimeException(shaderProgram.getLog());
            }
        }

        @Override
        public Shader getShader(Renderable renderable) {
            if (renderable.material.id.equals("screen-show")) {
                if (renderable.shader != null) {
                    return renderable.shader;
                }

                if (shader == null) {
                    shader = new MyShader(renderable, this.config, shaderProgram, () -> {
                        screenHighlight.bind(3);
                        shaderProgram.setUniformf("u_highLightOffset", this.monitorRenderer.currentX, this.monitorRenderer.currentY);
                        shaderProgram.setUniformi("u_highlightTexture", 3);
                    });
                    shader.init();
                }

                renderable.shader = shader;
                return renderable.shader;
            }

            return super.getShader(renderable);
        }
    }
}
