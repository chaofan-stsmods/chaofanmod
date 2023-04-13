package io.chaofan.sts.chaofanmod.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Berserk;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import io.chaofan.sts.chaofanmod.cards.ManualOfMerchantCard;
import io.chaofan.sts.chaofanmod.relics.ManualOfMerchant;

import java.util.ArrayList;

public class ManualOfMerchantAction extends AbstractGameAction {
    private boolean retrieveCard;

    public ManualOfMerchantAction() {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (AbstractDungeon.player.gold < ManualOfMerchant.GOLD_AMOUNT) {
                AbstractRelic relic = AbstractDungeon.player.getRelic(ManualOfMerchant.ID);
                if (relic != null) {
                    relic.stopPulse();
                    relic.grayscale = true;
                }
                isDone = true;
                return;
            }

            AbstractDungeon.cardRewardScreen.customCombatOpen(this.generateCardChoices(), ManualOfMerchant.uiDescription, true);
            this.tickDuration();
        } else {
            if (!this.retrieveCard) {
                AbstractRelic relic = AbstractDungeon.player.getRelic(ManualOfMerchant.ID);
                if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                    AbstractDungeon.cardRewardScreen.discoveryCard = null;
                    if (relic != null) {
                        relic.beginLongPulse();
                        relic.grayscale = false;
                        relic.flash();
                        this.addToTop(new GainEnergyAction(1));
                        AbstractDungeon.player.loseGold(ManualOfMerchant.GOLD_AMOUNT);
                        for (int i = 0; i < ManualOfMerchant.GOLD_AMOUNT; i++) {
                            AbstractDungeon.effectList.add(new GainPennyEffect(
                                    AbstractDungeon.player,
                                    AbstractDungeon.player.hb.cX,
                                    AbstractDungeon.player.hb.cY,
                                    relic.hb.cX,
                                    relic.hb.cY,
                                    false));
                        }
                    }
                } else {
                    if (relic != null) {
                        relic.stopPulse();
                        relic.grayscale = true;
                    }
                }

                this.retrieveCard = true;
            }

            this.tickDuration();
        }
    }

    private ArrayList<AbstractCard> generateCardChoices() {
        ArrayList<AbstractCard> result = new ArrayList<>();
        result.add(new ManualOfMerchantCard());
        return result;
    }
}
