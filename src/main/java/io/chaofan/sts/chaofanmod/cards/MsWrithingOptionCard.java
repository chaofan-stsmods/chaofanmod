package io.chaofan.sts.chaofanmod.cards;

import basemod.ReflectionHacks;
import basemod.abstracts.CustomCard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.evacipated.cardcrawl.modthespire.lib.SpireOverride;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

public class MsWrithingOptionCard extends CustomCard {
    public static final String ID = makeCardId(MsWrithingOptionCard.class.getSimpleName());

    private static final ShaderProgram shader;

    static {
        shader = new ShaderProgram(
                Gdx.files.internal(getShaderPath("friendcard.vs")).readString(),
                Gdx.files.internal(getShaderPath("friendcard.fs")).readString());
        if (!shader.isCompiled()) {
            throw new RuntimeException(shader.getLog());
        }
    }

    public final AbstractRelic relic;
    private final Texture backgroundImg;

    public MsWrithingOptionCard(AbstractRelic relic) {
        super(ID, "", getImagePath("cards/defaultAvatar.jpg"), -2, "", CardType.POWER, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.NONE);
        this.relic = relic;
        this.name = relic.name;
        this.rawDescription = relic.description.replaceAll("(?<= |^)#[rgbyw]", "");
        this.initializeDescription();
        this.backgroundImg = TextureLoader.getTexture(getImagePath("cards/power_bg.png"));
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        keywords.remove("[W]");
    }

    @Override
    public void upgrade() {

    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {

    }

    @Override
    public AbstractCard makeCopy() {
        return new MsWrithingOptionCard(relic);
    }

    @SpireOverride
    protected void renderPortrait(SpriteBatch sb) {
        Color color = ReflectionHacks.getPrivate(this, AbstractCard.class, "renderColor");
        float drawX = this.current_x - 125.0F;
        float drawY = this.current_y - 95.0F;

        if (!this.isLocked) {
            sb.setColor(Color.DARK_GRAY);
            sb.draw(this.backgroundImg,
                    drawX,
                    drawY + 72.0F,
                    125.0F,
                    23.0F,
                    250.0F,
                    190.0F,
                    this.drawScale * Settings.scale,
                    this.drawScale * Settings.scale,
                    this.angle,
                    0,
                    0,
                    250,
                    190,
                    false,
                    false);
            Texture img = this.relic.img;
            Texture outlineImg = this.relic.outlineImg;
            drawX = this.current_x - img.getWidth() / 2.0F;
            drawY = this.current_y - img.getHeight() / 2.0F;
            float scale = 250f / img.getWidth();
            sb.setColor(Color.BLACK);
            sb.draw(outlineImg,
                    drawX,
                    drawY + 80.0F / scale,
                    (float)outlineImg.getWidth() / 2.0F,
                    (float)outlineImg.getHeight() / 2.0F - 80.0F / scale,
                    (float)outlineImg.getWidth(),
                    (float)outlineImg.getHeight(),
                    this.drawScale * Settings.scale * scale,
                    this.drawScale * Settings.scale * scale,
                    this.angle,
                    0,
                    0,
                    outlineImg.getWidth(),
                    outlineImg.getHeight(),
                    false,
                    false);
            sb.setColor(color);
            sb.draw(img,
                    drawX,
                    drawY + 80.0F / scale,
                    (float)img.getWidth() / 2.0F,
                    (float)img.getHeight() / 2.0F - 80.0F / scale,
                    (float)img.getWidth(),
                    (float)img.getHeight(),
                    this.drawScale * Settings.scale * scale,
                    this.drawScale * Settings.scale * scale,
                    this.angle,
                    0,
                    0,
                    img.getWidth(),
                    img.getHeight(),
                    false,
                    false);
        } else {
            sb.draw(this.portraitImg, drawX, drawY + 72.0F, 125.0F, 23.0F, 250.0F, 190.0F, this.drawScale * Settings.scale, this.drawScale * Settings.scale, this.angle, 0, 0, 250, 190, false, false);
        }
    }
}
