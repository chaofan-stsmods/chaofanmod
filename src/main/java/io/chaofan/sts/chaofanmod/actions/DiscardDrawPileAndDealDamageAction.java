package io.chaofan.sts.chaofanmod.actions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.ArrayList;
import java.util.List;

public class DiscardDrawPileAndDealDamageAction extends AbstractGameAction {

    private static final float effectDuration = 0.9f;
    private static final float effectMidTime = 0.5f;

    private final int damage;
    private final int numCard;
    private final List<AbstractCard> affectedCards = new ArrayList<>();
    private final List<AbstractGameEffect> relatedEffects = new ArrayList<>();

    public DiscardDrawPileAndDealDamageAction(AbstractCreature target, int damage, int numCard) {
        this.damage = damage;
        this.numCard = numCard;
        this.target = target;
        this.duration = this.startDuration = effectDuration - 0.1f;
    }

    @Override
    public void update() {
        if (this.duration != this.startDuration) {
            tickDuration();
            return;
        }
        tickDuration();

        this.affectedCards.addAll(AbstractDungeon.player.drawPile.group);
        int damageCount = this.affectedCards.size() / this.numCard;
        addToTop(new TriggerDiscardAction());
        for (int i = 0; i < damageCount; i++) {
            addToTop(new DamageAction(this.target, new DamageInfo(AbstractDungeon.player, this.damage), AttackEffect.BLUNT_HEAVY, true));
        }

        AbstractDungeon.effectsQueue.add(new DiscardAndAttackEffect(damageCount));
    }

    private class TriggerDiscardAction extends AbstractGameAction {
        @Override
        public void update() {
            relatedEffects.removeIf(effect -> effect.isDone);
            if (relatedEffects.isEmpty()) {
                for (AbstractCard affectedCard : affectedCards) {
                    GameActionManager.incrementDiscard(false);
                    affectedCard.triggerOnManualDiscard();
                }
                isDone = true;
            }
        }
    }

    private class DiscardAndAttackEffect extends AbstractGameEffect {
        private int count;
        private float timer = 0.0F;

        public DiscardAndAttackEffect(int count) {
            this.count = count;
            relatedEffects.add(this);
        }

        public void update() {
            this.timer -= Gdx.graphics.getDeltaTime();
            if (this.timer < 0.0f) {
                --this.count;
                if (this.count <= 0) {
                    for (AbstractCard card : new ArrayList<>(AbstractDungeon.player.drawPile.group)) {
                        AbstractDungeon.player.drawPile.moveToDiscardPile(card);
                    }
                    this.isDone = true;
                    return;
                }
                this.timer += 0.1f;
                AbstractDungeon.effectsQueue.add(new DiscardAndAttackParticleEffect(target.hb.cX, target.hb.cY));
            }
        }

        @Override
        public void render(SpriteBatch spriteBatch) {
        }

        @Override
        public void dispose() {
        }
    }

    private class DiscardAndAttackParticleEffect extends AbstractGameEffect {
        private final List<AbstractCard> cards = new ArrayList<>();
        private float x = 0;
        private float y = 0;
        private final float midTx;
        private final float midTy;
        private boolean enterFinal = false;
        private float finalSx;
        private float finalSy;
        private final float finalTx;
        private final float finalTy;

        public DiscardAndAttackParticleEffect(float x, float y) {
            relatedEffects.add(this);
            for (int i = 0; i < numCard; i++) {
                AbstractCard card = AbstractDungeon.player.drawPile.getTopCard();
                AbstractDungeon.player.drawPile.removeCard(card);
                cards.add(card);
            }
            this.duration = this.startingDuration = effectDuration;
            this.finalTx = x;
            this.finalTy = y;
            this.scale = 0;
            this.midTx = (MathUtils.random() - 0.4f) * 300f + AbstractDungeon.player.hb.cX;
            this.midTy = (MathUtils.random() - 0.2f) * 500f + AbstractDungeon.player.hb.cY;
            this.color = Color.WHITE.cpy();
        }

        @Override
        public void update() {
            if (this.duration > effectMidTime) {
                this.x = Interpolation.exp5Out.apply(0, this.midTx, (effectDuration - this.duration) / (effectDuration - effectMidTime));
                this.y = Interpolation.exp5Out.apply(0, this.midTy, (effectDuration - this.duration) / (effectDuration - effectMidTime));
                this.scale = Interpolation.exp5Out.apply(0, 2, (effectDuration - this.duration) / (effectDuration - effectMidTime));
            } else {
                if (!this.enterFinal) {
                    this.enterFinal = true;
                    this.finalSx = this.x;
                    this.finalSy = this.y;
                }
                this.x = Interpolation.swingOut.apply(this.finalTx, this.finalSx, this.duration / effectMidTime);
                this.y = Interpolation.swingOut.apply(this.finalTy, this.finalSy, this.duration / effectMidTime);
                this.scale = 2;
            }

            this.duration -= Gdx.graphics.getDeltaTime();
            if (this.duration < 0.0F) {
                this.isDone = true;
            }

            if (this.isDone) {
                for (AbstractCard card : this.cards) {
                    card.current_x = card.target_x = this.x;
                    card.current_y = card.target_y = this.y;
                    AbstractDungeon.player.limbo.addToTop(card);
                    AbstractDungeon.player.limbo.moveToDiscardPile(card);
                }
            }
            this.rotation = this.rotation + 0.1f;
        }

        @Override
        public void render(SpriteBatch sb) {
            float scale = Settings.scale * this.scale;
            TextureRegion texture = ImageMaster.CARD_BLUE_ORB;
            sb.setColor(this.color);
            sb.draw(texture,
                    this.x - texture.getRegionWidth() / 2f,
                    this.y - texture.getRegionHeight() / 2f,
                    texture.getRegionWidth() / 2f,
                    texture.getRegionHeight() / 2f,
                    texture.getRegionWidth(),
                    texture.getRegionHeight(),
                    scale,
                    scale,
                    this.rotation);
        }

        @Override
        public void dispose() {

        }
    }
}
