package io.chaofan.sts.chaofanmod.cards;

import basemod.ReflectionHacks;
import basemod.abstracts.CustomCard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatchExtension;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.vfx.AlphaMaskSampleEffect;
import io.chaofan.sts.chaofanmod.vfx.CutEffect;
import io.chaofan.sts.chaofanmod.vfx.GlassEffect;

import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

public class SampleEffectCard extends CustomCard {
    public static final String ID = makeCardId(SampleEffectCard.class.getSimpleName());

    public SampleEffectCard() {
        super(ID, "Sample effect", new RegionName("red/attack/bash"), 0, "Sample effect", CardType.ATTACK, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.ALL_ENEMY);
    }

    @Override
    public void upgrade() {

    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
        addToBot(new VFXAction(new CutEffect(), CutEffect.DURATION - 0.25f));
        DamageAllEnemiesAction dmgAction = new DamageAllEnemiesAction(abstractPlayer, 0, DamageInfo.DamageType.NORMAL, AbstractGameAction.AttackEffect.NONE);
        ReflectionHacks.setPrivate(dmgAction, DamageAllEnemiesAction.class, "duration", 0);
        addToBot(dmgAction);
        addToBot(new VFXAction(new GlassEffect(), 0));
    }
}
