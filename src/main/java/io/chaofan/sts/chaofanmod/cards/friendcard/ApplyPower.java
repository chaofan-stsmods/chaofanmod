package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class ApplyPower extends ScoreNeededListProperty {
    private CharacterAnalyzer.PowerInfo powerInfo;

    public ApplyPower(FriendCard card) {
        super(card, new int[] { 0 });
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.applyPowers.isEmpty();
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        powerInfo = CharacterAnalyzer.applyPowers.get(random.nextInt(CharacterAnalyzer.applyPowers.size()));
        scoreNeeded = powerInfo.scores;
        return super.tryApplyScore(score, random);
    }

    @Override
    public void setToAllEnemies() {
        super.setToAllEnemies();
        multiplyValues(0.8f);
    }

    @Override
    public void modifyCard() {
        super.modifyCard();
        addEnemyTarget();
    }

    @Override
    public String getDescription() {
        return localize(
                localize("Apply {} {power}", "ApplyAll {} {power}")
                        .replace("{power}", CharacterAnalyzer.getKeyword(powerInfo.name)),
                getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        forEnemyOrAllEnemies(monster, m -> {
            if (m != null) {
                addToBot(new ApplyPowerAction(m, p, powerInfo.newInstance(p, m, getValueMayUpgrade())));
            }
        });
    }
}
