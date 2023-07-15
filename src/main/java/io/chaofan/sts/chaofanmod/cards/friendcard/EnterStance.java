package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.stances.NeutralStance;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class EnterStance extends NoUpgradeProperty {
    private int score;
    private AbstractStance stance;

    public EnterStance(FriendCard card) {
        super(card);
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.useStances.isEmpty() && (card.cost != 3 || random == null || random.nextBoolean());
    }

    @Override
    public int getScoreLose() {
        return score;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        stance = CharacterAnalyzer.useStances.get(random.nextInt(CharacterAnalyzer.useStances.size()));
        this.score = CharacterAnalyzer.stanceScores.get(stance.getClass());
        if (score > this.score) {
            return score - this.score;
        } else {
            return score;
        }
    }

    @Override
    public String getDescription() {
        if (stance instanceof NeutralStance) {
            return localize("ExitStance");
        }
        return localize("EnterStance {}").replace("{}", stance.name == null ? stance.ID : CharacterAnalyzer.getKeyword(stance.name));
    }

    @Override
    public int applyPriority() {
        return 120;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        try {
            this.addToBot(new ChangeStanceAction(stance.getClass().newInstance()));
        } catch (InstantiationException | IllegalAccessException e) {
            this.addToBot(new ChangeStanceAction(stance));
        }
    }
}
