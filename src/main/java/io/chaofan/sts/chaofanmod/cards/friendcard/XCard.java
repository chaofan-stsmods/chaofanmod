package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.actions.common.XCardAction;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.*;

public class XCard extends FriendCardProperty {
    private final List<FriendCardProperty> affectedProperties = new ArrayList<>();
    private final Map<FriendCardProperty, Boolean> originalShouldUse = new HashMap<>();

    public XCard(FriendCard card) {
        super(card);
        isActionableEffect = false;
    }

    @Override
    public boolean canUse(Random random) {
        return card.cost == 1 && card.properties.stream().noneMatch(p -> p.isNegative || p instanceof DrawCard);
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public int getScoreLose() {
        return -1;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        // Modify score otherwise it's ignored.
        return score + 1;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        if (additionalScore >= 4 && random.nextBoolean()) {
            return additionalScore - 4;
        }
        return additionalScore;
    }

    @Override
    public void modifyCard() {
        card.cost = card.costForTurn = -1;
        for (FriendCardProperty property : card.properties) {
            if (property == this) {
                break;
            }
            affectedProperties.add(property);
            originalShouldUse.put(property, property.shouldUse);
            property.shouldUse = false;
            property.multiplyValues(0.7f);
        }
    }

    @Override
    public String getDescription() {
        return shouldUpgrade && card.upgraded ? localize("XCard+") : localize("XCard");
    }

    @Override
    public int applyPriority() {
        return 190;
    }

    @Override
    public void use(AbstractPlayer player, AbstractMonster m) {
        this.addToBot(new XCardAction(
                AbstractGameAction.ActionType.DAMAGE, // Use damage to make sure it won't be cleaned.
                player,
                card.freeToPlayOnce,
                card.energyOnUse,
                (amount) -> {
                    affectedProperties.forEach(p -> p.shouldUse = originalShouldUse.get(p));
                    for (int i = shouldUpgrade && card.upgraded ? -1 : 0; i < amount; i++) {
                        FriendCardProperty.use(affectedProperties, player, m);
                    }
                    affectedProperties.forEach(p -> p.shouldUse = false);
                }));
    }
}
