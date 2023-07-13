package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class Condition extends NoUpgradeProperty {
    protected FriendCardProperty actionProperty;
    protected Type type;
    protected int scoreGain = 0;

    public Condition(FriendCard card) {
        super(card);
        isActionableEffect = false;
        gainScores = true;
    }

    @Override
    public boolean canUse(Random random) {
        if (card.properties.size() > 0) {
            FriendCardProperty lastProperty;
            if (card.properties.contains(this)) {
                lastProperty = card.properties.get(card.properties.indexOf(this) - 1);
            } else {
                lastProperty = card.properties.get(card.properties.size() - 1);
            }
            return super.canUse(random) && !lastProperty.gainScores && lastProperty.isActionableEffect && lastProperty.getScoreLose() >= 2;
        }
        return false;
    }

    @Override
    public int getScoreLose() {
        return -scoreGain;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        Type[] types = Type.values();
        int tryCount = 100;
        do {
            type = types[random.nextInt(types.length)];
            tryCount--;
            if (tryCount == 0) {
                return score;
            }
        } while (!type.conditionCheck.apply(this));

        FriendCardProperty lastProperty = card.properties.get(card.properties.size() - 1);
        this.actionProperty = lastProperty;
        boolean increaseScore = lastProperty.getScoreLose() < 4 || random.nextBoolean();
        if (increaseScore) {
            int remainingScore = lastProperty.tryApplyScore((int) (lastProperty.getScoreLose() / type.multiplier), random);
            scoreGain = Math.min(1, (int) Math.ceil(remainingScore * type.multiplier));
        } else {
            scoreGain = (int) Math.ceil(lastProperty.getScoreLose() * type.multiplier);
        }
        return score + scoreGain;
    }

    @Override
    public void modifyCard() {
        super.modifyCard();
        actionProperty.shouldUse = false;
        actionProperty.shouldShowDescription = false;
    }

    @Override
    public String getDescription() {
        return localize("Condition " + type.toString() + " {}").replace("{}", toLowerPrefix(actionProperty.getDescription()));
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (type.useCheck.apply(this)) {
            actionProperty.use(p, m);
        }
    }

    @Override
    public boolean glowCheck() {
        return type.glowCheck.apply(this);
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextBoolean()) {
            return new ConditionDamageOrBlock(card);
        } else {
            return this;
        }
    }

    private static <T> Optional<T> getLast(List<T> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(list.size() - 1));
    }

    private static <T> Optional<T> getLastButOne(List<T> list) {
        if (list.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(list.get(list.size() - 2));
    }

    protected enum Type {
        PLAYED_ATTACK(0.66f,
                (c) -> true,
                (c) -> getLast(AbstractDungeon.actionManager.cardsPlayedThisCombat).map(card -> card.type == AbstractCard.CardType.ATTACK).orElse(false),
                (c) -> getLastButOne(AbstractDungeon.actionManager.cardsPlayedThisCombat).map(card -> card.type == AbstractCard.CardType.ATTACK).orElse(false)),
        PLAYED_SKILL(0.66f,
                (c) -> true,
                (c) -> getLast(AbstractDungeon.actionManager.cardsPlayedThisCombat).map(card -> card.type == AbstractCard.CardType.SKILL).orElse(false),
                (c) -> getLastButOne(AbstractDungeon.actionManager.cardsPlayedThisCombat).map(card -> card.type == AbstractCard.CardType.SKILL).orElse(false)),
        FIRST_CARD(0.8f,
                (c) -> true,
                (c) -> AbstractDungeon.actionManager.cardsPlayedThisTurn.isEmpty(),
                (c) -> AbstractDungeon.actionManager.cardsPlayedThisTurn.size() == 1),
        ATTACK_ONLY(0.5f,
                (c) -> c.card.type == AbstractCard.CardType.ATTACK,
                (c) -> AbstractDungeon.player.hand.group.stream().allMatch(card -> card.type == AbstractCard.CardType.ATTACK),
                (c) -> AbstractDungeon.player.hand.group.stream().allMatch(card -> card.type == AbstractCard.CardType.ATTACK)),
        SINGLE_ATTACK(0.5f,
                (c) -> c.card.type == AbstractCard.CardType.ATTACK,
                (c) -> AbstractDungeon.player.hand.group.stream().allMatch(card -> card == c.card || card.type != AbstractCard.CardType.ATTACK),
                (c) -> AbstractDungeon.player.hand.group.stream().allMatch(card -> card == c.card || card.type != AbstractCard.CardType.ATTACK))
        ;

        final Function<Condition, Boolean> conditionCheck;
        final Function<Condition, Boolean> glowCheck;
        final float multiplier;
        final Function<Condition, Boolean> useCheck;

        Type(float multiplier, Function<Condition, Boolean> conditionCheck, Function<Condition, Boolean> glowCheck, Function<Condition, Boolean> useCheck) {
            this.conditionCheck = conditionCheck;
            this.glowCheck = glowCheck;
            this.multiplier = multiplier;
            this.useCheck = useCheck;
        }
    }
}
