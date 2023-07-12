package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;

public class GainEnergy extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 5, 9, 17, 31 };

    public GainEnergy(FriendCard card) {
        super(card, scoreNeeded);
    }

    @Override
    public String getDescription() {
        String result = localize("GainEnergy {}");
        int value = getValueMayUpgrade();
        if (value < 4) {
            return result.replace("{}", StringUtils.repeat(" [E]", value).trim());
        } else {
            return result.replace("{}", value + " [E]");
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new GainEnergyAction(getValueMayUpgrade()));
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextInt(5) == 0) {
            return new GainEnergyNextTurn(card);
        } else {
            return this;
        }
    }
}
