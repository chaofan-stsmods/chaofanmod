package io.chaofan.sts.chaofanmod.cards;

import basemod.BaseMod;
import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import io.chaofan.sts.chaofanmod.actions.DiscardDrawPileAndDealDamageAction;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;
import io.chaofan.sts.chaofanmod.actions.common.SelectFromWheelAction;
import io.chaofan.sts.chaofanmod.ui.WheelSelectScreen;

import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

public class SampleEffectCard extends CustomCard {
    public static final String ID = makeCardId(SampleEffectCard.class.getSimpleName());

    public SampleEffectCard() {
        super(ID, "Sample effect", new RegionName("red/attack/bash"), 0, "Sample effect", CardType.ATTACK, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.ENEMY);
    }

    @Override
    public void upgrade() {

    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster abstractMonster) {
        addToBot(new SelectFromWheelAction());
    }
}
