package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class GainPower extends ScoreNeededListProperty {
    private CharacterAnalyzer.PowerInfo powerInfo;

    public GainPower(FriendCard card) {
        super(card, new int[] { 0 });
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.gainPowers.isEmpty();
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        powerInfo = CharacterAnalyzer.gainPowers.get(random.nextInt(CharacterAnalyzer.gainPowers.size()));
        scoreNeeded = powerInfo.scores;
        return super.tryApplyScore(score, random);
    }

    @Override
    public void modifyCard() {
        super.modifyCard();
        addSelfTarget();
    }

    @Override
    public String getDescription() {
        return localize(
                localize("Gain {} {power}").replace("{power}", CharacterAnalyzer.getKeyword(powerInfo.name)),
                getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        addToBot(new ApplyPowerAction(p, p, powerInfo.newInstance(p, monster, getValueMayUpgrade())));
    }
}
