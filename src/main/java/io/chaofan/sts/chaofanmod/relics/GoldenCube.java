package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import basemod.interfaces.AlternateCardCostModifier;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class GoldenCube extends CustomRelic implements AlternateCardCostModifier {
    public static final String ID = makeId("relic.GoldenCube");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/golden_cube.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/golden_cube.png"));

    public GoldenCube() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public void onUseCard(AbstractCard targetCard, UseCardAction useCardAction) {
        if (EnergyPanel.totalCount < 0 ||
                (!targetCard.freeToPlay() &&
                    ((targetCard.cost == -1 && EnergyPanel.totalCount < 0) ||
                    (targetCard.cost >= 0 && EnergyPanel.totalCount - targetCard.costForTurn < 0)))) {
            flash();
        }
        AnonymousAction action = new AnonymousAction(() -> {
            if (EnergyPanel.totalCount < 0) {
                addToTop(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, -EnergyPanel.totalCount, AbstractGameAction.AttackEffect.FIRE));
                addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            }
        });
        action.actionType = AbstractGameAction.ActionType.DAMAGE;
        addToBot(action);
    }

    @Override
    public int getAlternateResource(AbstractCard card) {
        if (card.cost < 0) {
            return 0;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int spendAlternateCost(AbstractCard card, int i) {
        if (card.cost < 0) {
            return i;
        }
        EnergyPanel.useEnergy(i);
        return 0;
    }
}
