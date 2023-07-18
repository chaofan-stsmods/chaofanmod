package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.stances.NeutralStance;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class EnterStance extends NoUpgradeProperty {
    private CharacterAnalyzer.StanceInfo stanceInfo;

    public EnterStance(FriendCard card) {
        super(card);
        canBePower = true;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.useStances.isEmpty() && (card.cost != 3 || random == null || random.nextBoolean());
    }

    @Override
    public int getScoreLose() {
        return stanceInfo.score;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        this.stanceInfo = CharacterAnalyzer.useStances.get(random.nextInt(CharacterAnalyzer.useStances.size()));
        if (score > this.stanceInfo.score) {
            return score - this.stanceInfo.score;
        } else {
            return score;
        }
    }

    @Override
    public String getDescription() {
        if (stanceInfo.stance instanceof NeutralStance) {
            return localize("ExitStance");
        }
        return localize("EnterStance {}").replace("{}", stanceInfo.stance.name == null ? stanceInfo.stance.ID : CharacterAnalyzer.getKeyword(stanceInfo.stance.name));
    }

    @Override
    public int applyPriority() {
        return 120;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChangeStanceAction(stanceInfo.newInstance()));
    }
}
