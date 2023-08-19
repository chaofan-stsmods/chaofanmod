package io.chaofan.sts.chaofanmod.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import io.chaofan.sts.chaofanmod.actions.DiscardDrawPileAndDealDamageAction;

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
        addToBot(new DiscardDrawPileAndDealDamageAction(abstractMonster, 10, 1));
        huihun(p, 3, () -> {
            addToBot(new DrawCardAction(1));
        });
        huihun(p, 6, false, () -> {
            addToBot(new GainEnergyAction(1));
        });
    }

    public static void huihun(AbstractPlayer player, int num, Runnable callback) {
        huihun(player, num, true, callback);
    }

    public static void huihun(AbstractPlayer player, int num, boolean triggerPower, Runnable callback) {
        if (player.discardPile.size() >= num) {
            callback.run();
            if (triggerPower) {
                for (AbstractPower power : player.powers) {
                    if (power instanceof IHuihunSubscriber) {
                        ((IHuihunSubscriber) power).onHuihun();
                    }
                }
            }
        }
        if (player.hasPower("susha") && player.discardPile.size() >= num * 2) {
            callback.run();
        }
    }

    public interface IHuihunSubscriber {
        void onHuihun();
    }
}
