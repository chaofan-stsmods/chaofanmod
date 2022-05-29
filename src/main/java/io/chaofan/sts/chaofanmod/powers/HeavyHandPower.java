package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.HashMap;
import java.util.Map;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class HeavyHandPower extends AbstractPower {
    public final static String POWER_ID = makeId("power.HeavyHand");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;

    private final Map<AbstractCard, Integer> originalCost = new HashMap<>();
    private final Map<AbstractCard, Integer> costAddedByCard = new HashMap<>();

    public HeavyHandPower(AbstractCreature owner, int amount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.BUFF;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("accuracy");
        this.canGoNegative = true;
    }

    public void updateDescription() {
        this.description = String.format(DESCRIPTIONS[0], this.amount);
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0F;
        this.amount += stackAmount;
        updateCardsCost();
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.addToTop(new RemoveSpecificPowerAction(owner, owner, POWER_ID));
        for (Map.Entry<AbstractCard, Integer> entry : costAddedByCard.entrySet()) {
            AbstractCard card = entry.getKey();
            card.updateCost(-entry.getValue());
            Integer originalCost = this.originalCost.get(card);
            if (originalCost != null && card.cost == originalCost) {
                card.isCostModified = false;
            }
        }
    }

    @Override
    public void onInitialApplication() {
        updateCardsCost();
    }

    @Override
    public void onDrawOrDiscard() {
        updateCardsCost();
    }

    private void updateCardsCost() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || owner != player) {
            return;
        }

        for (AbstractCard c : player.hand.group) {
            Integer costAdded = costAddedByCard.get(c);
            if (c.cost < 0) {
                continue;
            }
            if (c.cost == 0 && this.amount < 0) {
                continue;
            }
            if (costAdded == null) {
                int oldCost = c.cost;
                if (!c.isCostModified) {
                    originalCost.put(c, c.cost);
                }
                c.updateCost(amount);
                int newCost = c.cost;
                costAddedByCard.put(c, newCost - oldCost);
            } else if (costAdded != amount) {
                int oldCost = c.cost;
                c.updateCost(amount - costAdded);
                int newCost = c.cost;
                costAddedByCard.put(c, costAdded + newCost - oldCost);
            }
        }
    }
}
