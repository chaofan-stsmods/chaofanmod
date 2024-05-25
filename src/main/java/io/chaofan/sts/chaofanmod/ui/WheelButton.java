package io.chaofan.sts.chaofanmod.ui;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatchExtension;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputListener;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

public class WheelButton {
    protected static final int SIZE = 600;
    private static final Color TRANSPARENT = new Color(0);
    private final static TextureRegion wheelTexture = new TextureRegion(TextureLoader.getTexture(ChaofanMod.getImagePath("ui/wheel.png")));

    public float startRotation;
    public float endRotation;
    public boolean enabled = true;
    public float scale;
    public float targetScale;
    public Color color;
    public Color idleColor;
    public Color hoverColor;
    public float centerX;
    public float centerY;
    public boolean isHovered;
    public boolean clickStarted;
    public float iconCenterX;
    public float iconCenterY;
    public WheelSelectScreen parent;

    public void update() {
        scale = MathHelper.scaleLerpSnap(scale, targetScale);

        isHovered = false;
        if (enabled) {
            if (!Settings.isControllerMode) {
                float diffX = InputHelper.mX - centerX;
                float diffY = InputHelper.mY - centerY;
                float sqrDistance = (diffX * diffX + diffY * diffY) / Settings.scale / Settings.scale * 4;
                if (sqrDistance <= SIZE * SIZE && sqrDistance >= SIZE * SIZE / 4f) {
                    float angle = MathUtils.atan2(diffY, diffX) * MathUtils.radiansToDegrees;
                    if (angle > startRotation && angle < endRotation) {
                        isHovered = true;
                    }
                    angle = angle + 360;
                    if (angle > startRotation && angle < endRotation) {
                        isHovered = true;
                    }
                }
            } else  {
                float[] axisValues = ReflectionHacks.getPrivate(CInputHelper.listener, CInputListener.class, "axisValues");
                float y = -axisValues[0];
                float x = axisValues[1];
                float sqrAxisDistance = x * x + y * y;
                if (sqrAxisDistance > 0.25f) {
                    float angle = MathUtils.atan2(y, x) * MathUtils.radiansToDegrees;
                    if (angle > startRotation && angle < endRotation) {
                        isHovered = true;
                    }
                    angle = angle + 360;
                    if (angle > startRotation && angle < endRotation) {
                        isHovered = true;
                    }
                }

                if (isHovered) {
                    Gdx.input.setCursorPosition((int) iconCenterX, Settings.HEIGHT - (int) iconCenterY);
                }
            }
        }

        iconCenterX = centerX + MathUtils.cosDeg((startRotation + endRotation) / 2) * SIZE * 3 / 8 * scale * Settings.scale;
        iconCenterY = centerY + MathUtils.sinDeg((startRotation + endRotation) / 2) * SIZE * 3 / 8 * scale * Settings.scale;

        boolean closing = parent.closing;
        Color targetColor = closing ? TRANSPARENT : (isHovered ? hoverColor : idleColor);
        targetScale = closing ? 0.01f : (isHovered ? 1.1f : 1.0f);
        color.a = MathHelper.scaleLerpSnap(color.a, targetColor.a);
        color.r = MathHelper.scaleLerpSnap(color.r, targetColor.r);
        color.g = MathHelper.scaleLerpSnap(color.g, targetColor.g);
        color.b = MathHelper.scaleLerpSnap(color.b, targetColor.b);

        if (isHovered && (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed())) {
            clickStarted = true;
        }
        if (clickStarted && (InputHelper.justReleasedClickLeft || CInputActionSet.select.isJustReleased())) {
            if (isHovered && !closing) {
                onClick();
            }
            clickStarted = false;
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(color);
        float size = SIZE * scale;
        SpriteBatchExtension.drawProgress(
                sb,
                wheelTexture,
                centerX - size / 2 * Settings.scale,
                centerY - size / 2 * Settings.scale,
                size * Settings.scale,
                size * Settings.scale,
                startRotation,
                endRotation);

        if (parent.closing) {
            renderIcon(sb, color.a);
        } else {
            renderIcon(sb, 1);
        }
    }

    public void renderIcon(SpriteBatch sb, float alpha) {
        sb.setColor(1, 1, 1, alpha);
        int imgSize = ImageMaster.INTENT_ATK_1.getWidth();
        sb.draw(ImageMaster.INTENT_ATK_1,
                iconCenterX - imgSize / 2f,
                iconCenterY - imgSize / 2f,
                imgSize / 2f,
                imgSize / 2f,
                imgSize,
                imgSize,
                scale * Settings.scale,
                scale * Settings.scale,
                0,
                0,
                0,
                imgSize,
                imgSize,
                false,
                false);
        FontHelper.cardEnergyFont_L.getData().setScale(scale);
        FontHelper.renderFontCentered(sb, FontHelper.topPanelInfoFont, "?", iconCenterX + 20 * scale * Settings.scale, iconCenterY - 20 * scale * Settings.scale, Color.WHITE);
        FontHelper.cardEnergyFont_L.getData().setScale(1);
    }

    public void onClick() {
        parent.closing = true;
        parent.closeTimer = 0.3f;
        parent.clickedButton = this;
    }
}
