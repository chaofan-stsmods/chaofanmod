# Overview
Screen post processors take effect after evething is rendered.
It inputs your screen as a texture and you may re-render
it to screen with any transformation.

## Implement post processor

A basic no-op post processor is like this:

```java
import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class NoOpScreenPostProcessor implements ScreenPostProcessor {
    @Override
    public void postProcess(SpriteBatch sb, TextureRegion textureRegion, OrthographicCamera camera) {
        sb.setColor(Color.WHITE);
        sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
        sb.draw(textureRegion, 0, 0);   // Render frame as it is
    }
}
```

You may want to transform the frame, like this example flips screen horizontally.

```java
import basemod.interfaces.ScreenPostProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SampleScreenPostProcessor implements ScreenPostProcessor {
    @Override
    public void postProcess(SpriteBatch sb, TextureRegion textureRegion, OrthographicCamera camera) {
        sb.setColor(Color.WHITE);
        sb.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);

        textureRegion.flip(true, false);
        // Render flipped screen
        sb.draw(textureRegion, 0, 0);
        textureRegion.flip(true, false);
    }

    // This method is optional.
    // Post processors with higher priority run after those with lower priority. 
    @Override
    public int priority() {
        return 50;
    }
}
```

## Register post processor

Run following code anywhere you want. It will take effect in current frame.

```java
ScreenPostProcessor postProcessor = new SampleScreenPostProcessor();
ScreenPostProcessorManager.addPostProcessor(postProcessor);
```

## Remove post processor

When the post processor is not needed. Don't forget to clean it up.

```java
ScreenPostProcessorManager.removePostProcessor(postProcessor);
```

