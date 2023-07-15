package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class ChannelOrbs extends FriendCardProperty {
    private int scorePerOrb;
    private AbstractOrb orb;

    public ChannelOrbs(FriendCard card) {
        super(card);
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.useOrbs.isEmpty();
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public int getScoreLose() {
        return (int) value * scorePerOrb;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        orb = CharacterAnalyzer.useOrbs.get(random.nextInt(CharacterAnalyzer.useOrbs.size()));
        scorePerOrb = CharacterAnalyzer.orbScores.get(orb.getClass());
        value = (int) (score / scorePerOrb);
        return (int) (score - value * scorePerOrb);
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        upgradeValue = (int) (additionalScore / scorePerOrb);
        return (int) (additionalScore - upgradeValue * scorePerOrb);
    }

    @Override
    public void multiplyValues(float scale) {
        super.multiplyValues(scale);
    }

    @Override
    public String getDescription() {
        return localize(
                localize("ChannelOrbs {} {orb}").replace("{orb}", CharacterAnalyzer.getKeyword(orb.name)),
                getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        int count = getValueMayUpgrade();
        for (int i = 0; i < count; i++) {
            this.addToBot(new ChannelAction(orb.makeCopy()));
        }
    }
}
