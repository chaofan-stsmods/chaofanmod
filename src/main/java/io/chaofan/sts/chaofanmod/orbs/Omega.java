package io.chaofan.sts.chaofanmod.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.combat.OmegaFlashEffect;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class Omega extends AbstractOrb {

    public static final int DAMAGE = 50;
    public static String ORB_ID = makeId("orb." + Omega.class.getSimpleName());
    public static final OrbStrings ORB_STRING = CardCrawlGame.languagePack.getOrbString(ORB_ID);

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("orbs/omega.png"));

    public Omega() {
        this.ID = ORB_ID;
        this.img = IMG;
        this.name = ORB_STRING.NAME;
        this.baseEvokeAmount = 0;
        this.evokeAmount = this.baseEvokeAmount;
        this.basePassiveAmount = 0;
        this.passiveAmount = this.basePassiveAmount;
        this.updateDescription();
        this.angle = MathUtils.random(360.0F);
        this.channelAnimTimer = 0.5F;
    }

    @Override
    public void updateDescription() {
        this.applyFocus();
        this.description = String.format(ORB_STRING.DESCRIPTION[0], DAMAGE);
    }

    @Override
    public void onEvoke() {
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m != null && !m.isDeadOrEscaped()) {
                if (Settings.FAST_MODE) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new OmegaFlashEffect(m.hb.cX, m.hb.cY)));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new OmegaFlashEffect(m.hb.cX, m.hb.cY), 0.2F));
                }
            }
        }

        AbstractDungeon.actionManager.addToBottom(new DamageAllEnemiesAction(AbstractDungeon.player, DamageInfo.createDamageMatrix(DAMAGE, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.FIRE, true));
    }

    @Override
    public AbstractOrb makeCopy() {
        return new Omega();
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        this.angle += Gdx.graphics.getDeltaTime() * 180.0F;
    }

    @Override
    public void render(SpriteBatch sb) {
        TextureAtlas.AtlasRegion glow = ImageMaster.EXHAUST_S;
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        sb.setColor(0.90f, 0.27f, 0.21f, 1f);
        sb.draw(glow, this.cX - 48.0F, this.cY - 48.0F + this.bobEffect.y + MathUtils.sin(this.angle / 12.566371F) * 8f, 48.0F, 48.0F, 96.0F, 96.0F, this.scale * 0.66f + MathUtils.sin(this.angle / 12.566371F) * 0.05F + 0.19634955F, this.scale * 0.8F, this.angle);
        sb.draw(glow, this.cX - 48.0F, this.cY - 48.0F + this.bobEffect.y, 48.0F, 48.0F, 96.0F, 96.0F, this.scale * 0.8F, this.scale * 0.66f + MathUtils.sin(this.angle / 12.566371F) * 0.05F + 0.19634955F, -this.angle);
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sb.setColor(Color.WHITE);
        sb.draw(this.img, this.cX - 48.0F, this.cY - 48.0F + this.bobEffect.y, 48.0F, 48.0F, 96.0F, 96.0F, this.scale, this.scale, 0, 0, 0, 96, 96, false, false);
        this.hb.render(sb);
    }

    @Override
    public void playChannelSFX() {
        CardCrawlGame.sound.play("ORB_DARK_CHANNEL", 0.1F);
    }
}
