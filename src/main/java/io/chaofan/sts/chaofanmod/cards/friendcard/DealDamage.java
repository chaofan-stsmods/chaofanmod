package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.DamageRandomEnemyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class DealDamage extends FriendCardProperty {

    // randomEnemy take effects only toAllEnemy is enabled.
    private boolean randomEnemy;
    private int attackCount = 1;
    private AbstractGameAction.AttackEffect attackEffect = AbstractGameAction.AttackEffect.FIRE;

    public DealDamage(FriendCard card) {
        super(card);
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.type == AbstractCard.CardType.ATTACK;
    }

    @Override
    public void setToAllEnemies() {
        super.setToAllEnemies();
        if (!randomEnemy) {
            multiplyValues(0.8f);
        } else {
            multiplyValues(1.5f);
        }
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public int getScoreLose() {
        return (int) (value < 5 ? value / 2 : value - 2);
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        value = score < 3 ? score * 2 : score + 2;
        if (random.nextInt(5) == 0) {
            if (random.nextBoolean()) {
                attackCount = 2;
            } else if (random.nextInt(3) != 0) {
                attackCount = 3;
            } else if (card.cost > 1) {
                attackCount = random.nextBoolean() ? 4 : 5;
            }
        }
        if (attackCount > 1) {
            randomEnemy = random.nextInt(4) != 0;
        }
        return 0;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        upgradeValue = additionalScore;
        return 0;
    }

    @Override
    public void multiplyValues(float scale) {
        super.multiplyValues(scale);
        card.baseDamage = card.damage = getBaseDamage();
    }

    @Override
    public void modifyCard() {
        card.baseDamage = card.damage = getBaseDamage();
        if (toAllEnemies) {
            card.setMultiDamage(true);
        }
        addEnemyTarget();
    }

    @Override
    public String getDescription() {
        if (attackCount > 1) {
            if (toAllEnemies) {
                return randomEnemy ? localize("DealDamageRandomMultipleTimes", attackCount) : localize("DealDamageAllMultipleTimes", attackCount);
            } else {
                return localize("DealDamageMultipleTimes", attackCount);
            }
        }
        return localize("DealDamage", "DealDamageAll");
    }

    @Override
    public int applyPriority() {
        return 0;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < attackCount; i++) {
            if (randomEnemy && toAllEnemies) {
                this.addToBot(new DamageRandomEnemyAction(new DamageInfo(p, card.damage), attackEffect));
            } else if (toAllEnemies) {
                this.addToBot(new DamageAllEnemiesAction(p, card.multiDamage, DamageInfo.DamageType.NORMAL, attackEffect));
            } else {
                this.addToBot(new DamageAction(m, new DamageInfo(p, card.damage), attackEffect));
            }
        }
    }

    @Override
    public void upgrade() {
        card.upgradeDamage(getUpgradeDamage());
    }

    private int getBaseDamage() {
        float value = this.value;
        if (attackCount > 3) {
            value *= 0.85;
        }
        value /= attackCount;
        if (value < 1) {
            value = 1;
        }
        return (int) value;
    }

    private int getUpgradeDamage() {
        float value = this.upgradeValue;
        if (attackCount > 3) {
            value *= 0.85;
        }
        value /= attackCount;
        if (this.upgradeValue != 0 && value < 1) {
            value = 1;
        }
        return (int) value;
    }
}
