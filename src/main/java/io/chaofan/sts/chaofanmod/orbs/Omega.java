package io.chaofan.sts.chaofanmod.orbs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class Omega extends AbstractOrb {

    public static final int DAMAGE = 50;
    public static String ORB_ID = makeId("orb." + Omega.class.getSimpleName());
    public static final OrbStrings ORB_STRING = CardCrawlGame.languagePack.getOrbString(ORB_ID);

    public Omega() {
        this.ID = ORB_ID;
        this.img = ImageMaster.ORB_PLASMA;
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
        AbstractDungeon.actionManager.addToBottom(new DamageAllEnemiesAction(AbstractDungeon.player, DAMAGE, DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.BLUNT_HEAVY));
    }

    @Override
    public AbstractOrb makeCopy() {
        return new Omega();
    }

    @Override
    public void render(SpriteBatch spriteBatch) {

    }

    @Override
    public void playChannelSFX() {

    }
}
