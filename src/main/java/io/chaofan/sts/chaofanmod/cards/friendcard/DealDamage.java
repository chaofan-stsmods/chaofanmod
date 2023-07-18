package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.DamageRandomEnemyAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.HemokinesisEffect;
import com.megacrit.cardcrawl.vfx.combat.MindblastEffect;
import com.megacrit.cardcrawl.vfx.combat.WeightyImpactEffect;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class DealDamage extends FriendCardProperty {

    private static final AbstractGameAction.AttackEffect[] attackEffects = new AbstractGameAction.AttackEffect[] {
            AbstractGameAction.AttackEffect.BLUNT_LIGHT,
            AbstractGameAction.AttackEffect.BLUNT_HEAVY,
            AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
            AbstractGameAction.AttackEffect.SMASH,
            AbstractGameAction.AttackEffect.SLASH_HEAVY,
            AbstractGameAction.AttackEffect.SLASH_HORIZONTAL,
            AbstractGameAction.AttackEffect.SLASH_VERTICAL,
            AbstractGameAction.AttackEffect.FIRE,
    };

    // randomEnemy take effects only toAllEnemy is enabled.
    private boolean randomEnemy;
    private int attackCount = 1;
    private int upgradeAttackCount = 0;
    private AbstractGameAction.AttackEffect attackEffect = AbstractGameAction.AttackEffect.FIRE;

    public DealDamage(FriendCard card) {
        this(card, false);
    }

    public DealDamage(FriendCard card, boolean secondary) {
        super(card);
        isAttack = true;
        useSecondaryDamage = secondary;
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
            } else if (score >= 14) {
                attackCount = random.nextBoolean() ? 4 : 5;
            }
        }
        if (attackCount > 1) {
            randomEnemy = random.nextInt(4) != 0;
        }
        attackEffect = attackEffects[random.nextInt(attackEffects.length)];
        return 0;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        upgradeValue = additionalScore;
        if (attackCount > 1 && random.nextBoolean() && additionalScore < value / additionalScore * 2) {
            upgradeValue = 0;
            upgradeAttackCount = 1;
        }
        return 0;
    }

    @Override
    public void multiplyValues(float scale) {
        super.multiplyValues(scale);
        if (useSecondaryDamage) {
            card.baseSecondaryDamage = card.secondaryDamage = getBaseDamage();
        } else {
            card.baseDamage = card.damage = getBaseDamage();
        }
    }

    @Override
    public void modifyCard() {
        if (useSecondaryDamage) {
            card.baseSecondaryDamage = card.secondaryDamage = getBaseDamage();
        } else {
            card.baseDamage = card.damage = getBaseDamage();
        }
        if (toAllEnemies) {
            card.setMultiDamage(true);
        }
        addEnemyTarget();
    }

    @Override
    public String getDescription() {
        String result;
        if (attackCount > 1) {
            if (toAllEnemies) {
                result = randomEnemy ? localize("DealDamageRandomMultipleTimes", getAttackCountMayUpgrade()) :
                        localize("DealDamageAllMultipleTimes", getAttackCountMayUpgrade());
            } else {
                result = localize("DealDamageMultipleTimes", getAttackCountMayUpgrade());
            }
        } else {
            result = localize("DealDamage", "DealDamageAll");
        }
        if (useSecondaryDamage) {
            return result.replace("!D!", "!chaofanmod:var.SecondaryDamage!");
        } else {
            return result;
        }
    }

    @Override
    public int applyPriority() {
        return 0;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        int attackCount = getAttackCountMayUpgrade();
        AbstractGameAction.AttackEffect attackEffect = this.attackEffect;

        // Hemokinesis
        if (attackCount == 1 && !toAllEnemies && card.properties.stream().anyMatch(p1 -> p1 instanceof LoseHp) && m != null) {
            addToBot(new VFXAction(new HemokinesisEffect(p.hb.cX, p.hb.cY, m.hb.cX, m.hb.cY), 0.5F));
            attackEffect = AbstractGameAction.AttackEffect.BLUNT_HEAVY;
        }
        // Bludgeon
        else if (attackCount == 1 && !toAllEnemies && getBaseDamage() >= 25 && m != null) {
            addToBot(new VFXAction(new WeightyImpactEffect(m.hb.cX, m.hb.cY)));
            addToBot(new WaitAction(0.8F));
            attackEffect = AbstractGameAction.AttackEffect.NONE;
        }
        // Hyper beam
        else if (attackCount == 1 && toAllEnemies && !randomEnemy && card.properties.stream().anyMatch(p1 -> p1 instanceof GainFocus && p1.isNegative)) {
            addToBot(new SFXAction("ATTACK_HEAVY"));
            addToBot(new VFXAction(p, new MindblastEffect(p.dialogX, p.dialogY, p.flipHorizontal), 0.1F));
            attackEffect = AbstractGameAction.AttackEffect.NONE;
        }


        for (int i = 0; i < attackCount; i++) {
            if (randomEnemy && toAllEnemies) {
                this.addToBot(new DamageRandomEnemyAction(new DamageInfo(p, useSecondaryDamage ? card.secondaryDamage : card.damage), attackEffect));
            } else if (toAllEnemies) {
                this.addToBot(new DamageAllEnemiesAction(p, useSecondaryDamage ? card.secondaryMultiDamage : card.multiDamage, DamageInfo.DamageType.NORMAL, attackEffect));
            } else {
                this.addToBot(new DamageAction(m, new DamageInfo(p, useSecondaryDamage ? card.secondaryDamage : card.damage), attackEffect));
            }
        }
    }

    @Override
    public void upgrade() {
        if (useSecondaryDamage) {
            card.upgradeSecondaryDamage(getUpgradeDamage());
        } else {
            card.upgradeDamage(getUpgradeDamage());
        }
    }

    private int getAttackCountMayUpgrade() {
        return attackCount + (card.upgraded ? upgradeAttackCount : 0);
    }

    private int getBaseDamage() {
        float value = this.value;
        if (attackCount == 4) {
            value *= 0.7;
        } else if (attackCount == 5) {
            value *= 0.6;
        }
        value /= attackCount;
        if (value < 1) {
            value = 1;
        }
        return (int) value;
    }

    private int getUpgradeDamage() {
        float value = this.upgradeValue;
        if (attackCount == 4) {
            value *= 0.7;
        } else if (attackCount == 5) {
            value *= 0.6;
        }
        value /= attackCount;
        if (this.upgradeValue != 0 && value < 1) {
            value = 1;
        }
        return (int) value;
    }
}
