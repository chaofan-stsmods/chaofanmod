package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ExhaustAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class GainArtifact extends NoUpgradeProperty {

    public GainArtifact(FriendCard card) {
        super(card);
        canBePower = true;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.properties.stream().anyMatch(p -> p instanceof Exhaust);
    }

    @Override
    public int getScoreLose() {
        return 5;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        if (score > 5) {
            return score - 5;
        } else {
            return score;
        }
    }

    @Override
    public String getDescription() {
        return localize("GainArtifact");
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new ArtifactPower(p, 1)));
    }
}
