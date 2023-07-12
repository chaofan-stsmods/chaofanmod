package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.EnergizedBluePower;
import com.megacrit.cardcrawl.powers.EnergizedPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import org.apache.commons.lang3.StringUtils;

public class GainEnergyNextTurn extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 4, 7, 13, 28 };

    public GainEnergyNextTurn(FriendCard card) {
        super(card, scoreNeeded);
        alternateOf = GainEnergy.class;
    }

    @Override
    public String getDescription() {
        String result = localize("GainEnergyNextTurn {}");
        int value = getValueMayUpgrade();
        if (value < 4) {
            return result.replace("{}", StringUtils.repeat(" [E]", value).trim());
        } else {
            return result.replace("{}", value + " [E]");
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new EnergizedBluePower(p, getValueMayUpgrade())));
    }
}
