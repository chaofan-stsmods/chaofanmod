package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.watcher.PressEndTurnButtonAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class EndYourTurn extends NoUpgradeProperty {
    private int scoreGain;

    public EndYourTurn(FriendCard card) {
        super(card);
        isActionableEffect = false;
        gainScores = true;
    }

    @Override
    public int getScoreLose() {
        return -scoreGain;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.cost >= 0 && card.cost <= 3 &&
                card.properties.stream().noneMatch(p -> (p instanceof GainDexterity && ((GainDexterity) p).temp) ||
                        (p instanceof GainStrength && ((GainStrength) p).temp) ||
                        p instanceof DrawCard || p instanceof GainEnergy);
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        scoreGain = costScoreMap[card.cost] / 2;
        return score + scoreGain;
    }

    @Override
    public String getDescription() {
        return localize("EndYourTurn");
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new PressEndTurnButtonAction());
    }

    @Override
    public int applyPriority() {
        return 200; // larger than x card.
    }
}
