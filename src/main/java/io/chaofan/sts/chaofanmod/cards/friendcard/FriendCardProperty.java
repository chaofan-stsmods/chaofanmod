package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.chaofan.sts.CommonModUtils.getLocalizationFilePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.MOD_ID;

public abstract class FriendCardProperty {
    protected static final int[] costScoreMap = CharacterAnalyzer.costScoreMap;
    private static final int[] costScoreUpgradeMap = CharacterAnalyzer.costScoreUpgradeMap;
    private static final TreeMap<Integer, Class<? extends FriendCardProperty>> allCardProperties = new TreeMap<>();
    private static int allCardPropertiesPowerSum;

    protected static Map<String, String> friendCardStrings;
    static {
        Gson gson = new Gson();
        String json = Gdx.files.internal(getLocalizationFilePath(MOD_ID, "friendcard.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        Type configType = (new TypeToken<Map<String, String>>() {}).getType();
        friendCardStrings = gson.fromJson(json, configType);

        registerProperty(DrawCard.class, 100);
        registerProperty(GainEnergy.class, 100);
        registerProperty(ApplyVulnerable.class, 100);
        registerProperty(ApplyWeak.class, 100);
        registerProperty(LoseHp.class, 50);
        registerProperty(AddStatus.class, 100);
        registerProperty(XCard.class, 100);
        registerProperty(GainStrength.class, 100);
        registerProperty(GainDexterity.class, 100);
        registerProperty(ExhaustHand.class, 80);
        registerProperty(Discard.class, 100);
        registerProperty(GainArtifact.class, 50);
        registerProperty(Heal.class, 50);
        registerProperty(EnemyLoseStrength.class, 50);
        registerProperty(Condition.class, 150);
        registerProperty(GainBlockNextTurn.class, 100);
        registerProperty(EachEnemy.class, 100);
        registerProperty(ChannelOrbs.class, 100);
        registerProperty(GainOrbSlot.class, 100);
        registerProperty(GainFocus.class, 100);
        registerProperty(EndYourTurn.class, 80);
        registerProperty(EnterStance.class, 100);
        registerProperty(EachOrb.class, 100);
        registerProperty(ApplyPower.class, 150);
        registerProperty(GainPower.class, 150);
    }

    protected boolean shouldUpgrade;
    protected boolean shouldUse = true;
    protected boolean shouldShowDescription = true;
    protected final FriendCard card;
    protected float value;
    protected float upgradeValue;
    protected boolean isNegative;
    protected boolean toAllEnemies;
    protected boolean gainScores;
    protected Class<? extends FriendCardProperty> alternateOf;
    protected boolean isActionableEffect = true;
    protected boolean isAttack;
    protected boolean useSecondaryDamage;
    protected boolean useSecondaryBlock;
    protected int weight = 1;
    protected boolean canBePower = false;

    public FriendCardProperty(FriendCard card) {
        this.card = card;
    }

    public int multiplyScore(int score) {
        return score;
    }

    public void multiplyValues(float scale) {
        value = multiplyValue(value, scale);
        upgradeValue = multiplyValue(upgradeValue, scale);
    }

    public boolean canUse(Random random) {
        if (isNegative) {
            int negativeCount = (int) card.properties.stream().filter(p -> p.isNegative).count();
            if (negativeCount == 0) {
                return true;
            } else if (negativeCount == 1) {
                return random == null || random.nextInt(4) == 0;
            } else {
                return false;
            }
        }

        return true;
    }

    public void setToAllEnemies() {
        toAllEnemies = true;
    }

    public abstract int getScoreLose();
    public abstract boolean canUpgrade();
    public abstract int tryApplyScore(int score, Random random);
    public abstract int tryApplyUpgradeScore(int additionalScore, Random random);
    public abstract String getDescription();
    public abstract void use(AbstractPlayer p, AbstractMonster m);

    public FriendCardProperty makeAlternateProperty(Random random) {
        return this;
    }

    public void modifyCard() {
    }

    public void upgrade() {
    }

    public int applyPriority() {
        return 100;
    }

    public int descriptionPriority() {
        return applyPriority();
    }

    public boolean glowCheck() {
        return false;
    }

    protected void addToBot(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    protected String localize(String key) {
        String result = friendCardStrings.get(key);
        return result != null ? result : key;
    }

    protected String localize(String key, String targetAllKey) {
        return localize(toAllEnemies ? targetAllKey : key);
    }

    protected String localize(String key, int value) {
        return localize(key).replace("{}", (shouldUpgrade && card.displayingUpgrades ? "[#7fff00]" + value + "[]" : String.valueOf(value)))
                .replace("(s)", value == 1 ? "" : "s");
    }

    protected String localize(String key, String targetAllKey, int value) {
        return localize(toAllEnemies ? targetAllKey : key, value);
    }

    protected int getValueMayUpgrade() {
        return (int) (value + (card.upgraded ? upgradeValue : 0));
    }

    protected void forEnemyOrAllEnemies(AbstractMonster target, Consumer<AbstractMonster> callback) {
        if (toAllEnemies) {
            AbstractDungeon.getMonsters().monsters.stream().filter(m -> !m.isDeadOrEscaped()).forEach(callback);
        } else {
            callback.accept(target);
        }
    }

    protected void addSelfTarget() {
        if (card.target == AbstractCard.CardTarget.NONE) {
            card.target = AbstractCard.CardTarget.SELF;
        } else if (card.target == AbstractCard.CardTarget.ALL_ENEMY) {
            card.target = AbstractCard.CardTarget.ALL;
        } else if (card.target == AbstractCard.CardTarget.ENEMY) {
            card.target = AbstractCard.CardTarget.SELF_AND_ENEMY;
        }
    }

    protected void addEnemyTarget() {
        if (card.target == AbstractCard.CardTarget.NONE) {
            card.target = toAllEnemies ? AbstractCard.CardTarget.ALL_ENEMY : AbstractCard.CardTarget.ENEMY;
        } else if (card.target == AbstractCard.CardTarget.SELF) {
            card.target = toAllEnemies ? AbstractCard.CardTarget.ALL : AbstractCard.CardTarget.SELF_AND_ENEMY;
        }
    }

    protected FriendCardProperty makeNew() {
        try {
            return this.getClass().getConstructor(FriendCard.class).newInstance(this.card);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        throw new RuntimeException(getClass() + ".makeNew() doesn't work.");
    }

    public static void addProperties(FriendCard friendCard, List<FriendCardProperty> properties, Random random) {
        CharacterAnalyzer.tryAnalyzeCurrentCharacter();

        if (random.nextInt(4) == 0) {
            properties.add(new Exhaust(friendCard));
        }
        if (random.nextInt(4) == 0) {
            if (random.nextBoolean()) {
                if (friendCard.cost > 0) {
                    properties.add(new Ethereal(friendCard));
                }
            } else {
                properties.add(new Retain(friendCard));
            }
        }

        boolean useBlockOrDamage = friendCard.type == AbstractCard.CardType.ATTACK || random.nextInt(5) < 3; // 60%
        int propertyCount = Math.max(1, random.nextInt(3) + 2 - properties.size());
        int score = costScoreMap[friendCard.cost];
        for (FriendCardProperty property : properties) {
            score = property.multiplyScore(score);
        }

        // Force adding an effect that provides score.
        if (friendCard.cost == 0 && random.nextInt(5) == 0) {
            int tryCount = 100;
            while (tryCount > 0) {
                tryCount--;
                Class<? extends FriendCardProperty> propertyClass = allCardProperties
                        .ceilingEntry(random.nextInt(allCardPropertiesPowerSum)).getValue();
                FriendCardProperty property;
                try {
                    property = propertyClass.getConstructor(FriendCard.class).newInstance(friendCard).makeAlternateProperty(random);
                } catch (Exception ex) {
                    continue;
                }

                if (!property.canUse(random)) {
                    continue;
                }

                int remainingScore = property.tryApplyScore(score, random);
                if (remainingScore == score || !property.gainScores) {
                    continue;
                }

                properties.add(property);
                if (properties.stream().anyMatch(p -> !p.canUse(null))) {
                    properties.remove(property);
                    continue;
                }

                score = remainingScore;
                break;
            }
        }

        while (score / propertyCount < 2 && propertyCount > 1) {
            propertyCount--;
        }

        int effectPropertyCount = propertyCount - (useBlockOrDamage ? 1 : 0);
        int averageScore = score / propertyCount;
        int tryCount = 1000;
        while (effectPropertyCount > 0 && score > 0 && tryCount > 0) {
            tryCount--;
            Class<? extends FriendCardProperty> propertyClass = allCardProperties
                    .ceilingEntry(random.nextInt(allCardPropertiesPowerSum)).getValue();
            if (properties.stream().anyMatch(p -> p.getClass() == propertyClass || p.alternateOf == propertyClass)) {
                continue;
            }

            FriendCardProperty property;
            try {
                property = propertyClass.getConstructor(FriendCard.class).newInstance(friendCard).makeAlternateProperty(random);
            } catch (Exception ex) {
                continue;
            }

            if (!property.canUse(random)) {
                continue;
            }

            if (propertyCount <= property.weight && property.gainScores) {
                continue;
            }

            int nextScore = propertyCount == 1 ? averageScore :
                    Math.max(1, Math.min((int) (averageScore * (1 + random.nextGaussian())), Math.min(averageScore * 2, score)));
            int remainingScore = property.tryApplyScore(nextScore, random);
            if (remainingScore == nextScore) {
                continue;
            }

            // Check again in case GainStrength become LoseStrength
            if (propertyCount <= property.weight && property.gainScores) {
                continue;
            }

            properties.add(property);
            if (properties.stream().anyMatch(p -> !p.canUse(null))) {
                properties.remove(property);
                continue;
            }

            score = score - nextScore + remainingScore;
            effectPropertyCount -= property.weight;
            propertyCount -= property.weight;
            if (propertyCount > 0) {
                averageScore = score / propertyCount;
            }
        }

        boolean hasBlockOrDamage = false;
        if (score > 0 && (useBlockOrDamage || score > 5)) {
            if (friendCard.type == AbstractCard.CardType.SKILL) {
                GainBlock block = new GainBlock(friendCard);
                block.tryApplyScore(score, random);
                properties.add(block);
                hasBlockOrDamage = true;
            } else if (friendCard.type == AbstractCard.CardType.ATTACK) {
                DealDamage damage = new DealDamage(friendCard);
                damage.tryApplyScore(score, random);
                properties.add(damage);
                hasBlockOrDamage = true;
            }
        }

        if (friendCard.type == AbstractCard.CardType.ATTACK && properties.stream().noneMatch(p -> p.isAttack)) {
            friendCard.type = AbstractCard.CardType.SKILL;
        }

        // Apply upgrade
        int upgradeScore = costScoreUpgradeMap[friendCard.cost];
        for (FriendCardProperty property : properties) {
            upgradeScore = property.multiplyScore(upgradeScore);
        }

        boolean upgradeBlockOrDamage = hasBlockOrDamage && random.nextInt(5) < 2; // 60%
        List<FriendCardProperty> pendingUpgrade = new ArrayList<>(properties);
        pendingUpgrade.removeIf(p -> !p.canUpgrade());
        int upgradeCount = pendingUpgrade.size() == 0 ? 1 : random.nextInt(Math.min(3, pendingUpgrade.size())) + 1;

        if (hasBlockOrDamage) {
            pendingUpgrade.remove(pendingUpgrade.size() - 1);
        }

        while (upgradeScore / upgradeCount < 4 && upgradeCount > 1) {
            upgradeCount--;
        }

        int effectUpgradeCount = upgradeCount - (upgradeBlockOrDamage ? 1 : 0);
        int averageUpgradeScore = upgradeScore / upgradeCount;
        tryCount = 100;
        boolean hasUpgrade = false;
        int upgradeScoreBonus = 0;
        while (effectUpgradeCount > 0 && upgradeScore > 0 && tryCount > 0 && pendingUpgrade.size() > 0) {
            tryCount--;
            if (tryCount < 10 && !hasUpgrade) {
                if (upgradeCount > 1) {
                    upgradeCount -= 1;
                } else {
                    upgradeScore += 1;
                    upgradeScoreBonus += 1;
                }
                averageUpgradeScore = upgradeScore / upgradeCount;
            }
            FriendCardProperty property = pendingUpgrade.get(random.nextInt(pendingUpgrade.size()));

            int nextScore = upgradeCount == 1 ? averageUpgradeScore :
                    Math.max(1, Math.min((int) (averageUpgradeScore * (1 + random.nextGaussian())), Math.min(averageUpgradeScore * 2, upgradeScore)));
            int remainingScore = property.tryApplyUpgradeScore(nextScore, random);
            if (remainingScore != nextScore) {
                hasUpgrade = true;
                pendingUpgrade.remove(property);
                property.shouldUpgrade = true;
                upgradeScore = upgradeScore - nextScore + remainingScore;
                effectUpgradeCount--;
                upgradeCount--;
                if (upgradeCount > 0) {
                    averageUpgradeScore = upgradeScore / upgradeCount;
                }
            }
        }

        upgradeScore -= upgradeScoreBonus;
        if (hasBlockOrDamage && upgradeScore > 0) {
            FriendCardProperty lastFriendCardProperty = properties.get(properties.size() - 1);
            lastFriendCardProperty.tryApplyUpgradeScore(upgradeScore, random);
            lastFriendCardProperty.shouldUpgrade = true;
        }

        if (!hasUpgrade && !hasBlockOrDamage) {
            if (friendCard.cost == 0) {
                DrawCard drawCard = new DrawCard(friendCard);
                drawCard.tryApplyScore(2, random);
                drawCard.upgradeOnly = true;
                drawCard.shouldUpgrade = true;
                drawCard.shouldUse = false;
                drawCard.shouldShowDescription = false;
                properties.add(drawCard);
            } else {
                friendCard.reduceCostOnUpgrade = true;
            }
        }

        if (random.nextInt(5) == 0) {
            properties.forEach(FriendCardProperty::setToAllEnemies);
        }

        if (properties.stream().anyMatch(p -> p instanceof Exhaust) &&
                properties.stream().allMatch(p -> p.canBePower) && random.nextInt(3) != 0) {
            friendCard.type = AbstractCard.CardType.POWER;
            properties.removeIf(p -> p instanceof Exhaust);
        }
    }

    public static void applyProperties(FriendCard friendCard, List<FriendCardProperty> properties) {
        properties.sort(Comparator.comparing(FriendCardProperty::applyPriority));
        properties.forEach(FriendCardProperty::modifyCard);
        applyDescriptions(friendCard, properties);
    }

    public static void upgrade(FriendCard friendCard, List<FriendCardProperty> properties) {
        properties.sort(Comparator.comparing(FriendCardProperty::applyPriority));
        properties.stream().filter(p -> p.shouldUpgrade).forEach(FriendCardProperty::upgrade);
        applyDescriptions(friendCard, properties);
    }

    public static void use(List<FriendCardProperty> properties, AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
        properties.sort(Comparator.comparing(FriendCardProperty::applyPriority));
        properties.stream().filter(p -> p.shouldUse).forEach(p -> p.use(abstractPlayer, abstractMonster));
    }

    public static void displayUpgrades(FriendCard friendCard, List<FriendCardProperty> properties) {
        if (Loader.isModLoaded("mintyspire")) {
            return;
        }

        friendCard.displayingUpgrades = true;
        applyDescriptions(friendCard, properties);
        friendCard.displayingUpgrades = false;
    }
    
    public static void registerProperty(Class<? extends FriendCardProperty> propertyClass, int power) {
        allCardProperties.put(allCardPropertiesPowerSum += power, propertyClass);
    }

    protected static float multiplyValue(float originalValue, float scale) {
        return originalValue == 0 ? 0 : Math.max(1f, originalValue * scale);
    }

    protected static String toLowerPrefix(String s) {
        if (s.length() == 0 || s.startsWith("ALL ")) {
            return s;
        }

        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private static void applyDescriptions(FriendCard friendCard, List<FriendCardProperty> properties) {
        properties.sort(Comparator.comparing(FriendCardProperty::descriptionPriority));
        friendCard.rawDescription = properties.stream().filter(p -> p.shouldShowDescription)
                .map(FriendCardProperty::getDescription).collect(Collectors.joining(" NL "));
        friendCard.initializeDescription();
    }
}
