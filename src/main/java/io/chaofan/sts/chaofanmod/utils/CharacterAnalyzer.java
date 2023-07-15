package io.chaofan.sts.chaofanmod.utils;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.orbs.*;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.stances.*;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.chaofan.sts.CommonModUtils.getLocalizationFilePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.MOD_ID;

public class CharacterAnalyzer {
    public static final int[] costScoreMap = new int[] { 4, 9, 16, 30, 45, 60 };

    public static AbstractCard.CardColor playerColor;
    public static List<AbstractStance> useStances = new ArrayList<>();
    public static Map<Class<? extends AbstractStance>, Integer> stanceScores = new HashMap<>();
    public static List<AbstractOrb> useOrbs = new ArrayList<>();
    public static Map<Class<? extends AbstractOrb>, Integer> orbScores = new HashMap<>();
    public static boolean affectedByFocus = false;
    public static Map<AbstractCard, CardInfo> cardInfoMap;

    private static final Map<Class<? extends AbstractStance>, Integer> predefinedStanceScores = new HashMap<>();
    private static final Map<Class<? extends AbstractOrb>, Integer> predefinedOrbScores = new HashMap<>();
    private static Map<String, String> analyzerStrings;

    static {
        predefinedStanceScores.put(WrathStance.class, 4);
        predefinedStanceScores.put(CalmStance.class, 8);
        predefinedStanceScores.put(DivinityStance.class, 51);
        predefinedStanceScores.put(NeutralStance.class, 2);

        predefinedOrbScores.put(Lightning.class, 4);
        predefinedOrbScores.put(Frost.class, 5);
        predefinedOrbScores.put(Dark.class, 8);
        predefinedOrbScores.put(Plasma.class, 12);

        Gson gson = new Gson();
        String json = Gdx.files.internal(getLocalizationFilePath(MOD_ID, "characteranalyzer.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        Type configType = (new TypeToken<Map<String, String>>() {
        }).getType();
        analyzerStrings = gson.fromJson(json, configType);
    }

    public static void tryAnalyzeCurrentCharacter() {
        if (AbstractDungeon.player == null) {
            return;
        }

        if (AbstractDungeon.player.getCardColor() == playerColor) {
            return;
        }

        playerColor = AbstractDungeon.player.getCardColor();

        List<AbstractCard> cards = CardLibrary.getAllCards().stream().filter(c -> c.color == playerColor).collect(Collectors.toList());
        ClassPool classPool = Loader.getClassPool();

        fillCardInfoMap(cards, classPool);
        calculateStancesScore();
        calculateOrbsScore();
        setAffectedByFocus();
    }

    private static void fillCardInfoMap(List<AbstractCard> cards, ClassPool classPool) {
        cardInfoMap = new HashMap<>();

        for (AbstractCard card : cards) {
            CardInfo cardInfo = new CardInfo();
            cardInfo.card = card;
            cardInfoMap.put(card, cardInfo);
            try {
                CtClass cardClass = classPool.get(card.getClass().getName());
                CtMethod useMethod = cardClass.getDeclaredMethod("use");
                try { findNewActions(classPool, useMethod, cardInfo); } catch (Exception ignored) {}
                try { findBranches(useMethod, cardInfo); } catch (Exception ignored) {}
                try { findStanceFromCard(classPool, useMethod, cardInfo); } catch (Exception ignored) {}
                try { findOrbsFromCard(classPool, useMethod, cardInfo); } catch (Exception ignored) {}
            } catch (NotFoundException e) {
                ChaofanMod.logger.warn("fillCardInfoMap failed on card " + card);
            }
        }
    }

    private static void findNewActions(ClassPool classPool, CtMethod useMethod, CardInfo cardInfo) throws BadBytecode {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        ConstPool cp = ca.getConstPool();

        List<CodePattern.Range> results = CodePattern.find(ci, new CodePattern(Opcode.NEW, (i, l) -> {
            int argument = i.s16bitAt(l + 1);
            try {
                CtClass candidate = classPool.get(cp.getClassInfo(argument));
                return candidate.subclassOf(classPool.get(AbstractGameAction.class.getName())) &&
                        !candidate.getName().equals(VFXAction.class.getName()) &&
                        !candidate.getName().equals(SFXAction.class.getName()) &&
                        !candidate.getName().equals(WaitAction.class.getName());
            } catch (NotFoundException e) {
                return false;
            }
        }));

        cardInfo.actionCount = results.size();
    }

    private static void findBranches(CtMethod useMethod, CardInfo cardInfo) throws BadBytecode {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();

        List<CodePattern.Range> results = CodePattern.find(ci, CodePattern.anyOf(
                Opcode.IF_ICMPLE, Opcode.IF_ICMPLT, Opcode.IF_ICMPGE, Opcode.IF_ICMPGT, Opcode.IF_ICMPNE, Opcode.IF_ICMPEQ,
                Opcode.IFEQ, Opcode.IFNE, Opcode.IFNONNULL, Opcode.IFNULL, Opcode.IFLE, Opcode.IFLT, Opcode.IFGE, Opcode.IFGT,
                Opcode.IF_ACMPEQ, Opcode.IF_ACMPNE));

        for (CodePattern.Range result : results) {
            if (ci.s16bitAt(result.start + 1) > 0) {
                cardInfo.forwardBranchCount++;
            } else {
                cardInfo.backwardBranchCount++;
            }
        }
    }

    private static void findStanceFromCard(ClassPool classPool, CtMethod useMethod, CardInfo cardInfo) throws NotFoundException, BadBytecode {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        ConstPool cp = ca.getConstPool();

        List<CodePattern.Range> results = CodePattern.find(ci, CodePattern.sequence(
                CodePattern.anyOf(
                        CodePattern.sequence(
                            new CodePattern(Opcode.NEW, (i, l) -> {
                                int argument = i.s16bitAt(l + 1);
                                try {
                                    CtClass candidate = classPool.get(cp.getClassInfo(argument));
                                    return candidate.subclassOf(classPool.get(AbstractStance.class.getName()));
                                } catch (NotFoundException e) {
                                    return false;
                                }
                            }),
                            new CodePattern(Opcode.DUP),
                            new CodePattern(Opcode.INVOKESPECIAL)
                        ),
                        new CodePattern(Opcode.LDC),
                        new CodePattern(Opcode.LDC_W)
                ),
                new CodePattern(Opcode.INVOKESPECIAL, (i, l) -> {
                    int argument = i.s16bitAt(l + 1);
                    return cp.getMethodrefClassName(argument).equals(ChangeStanceAction.class.getName());
                })
        ));

        for (CodePattern.Range result : results) {
            int opcode = ci.byteAt(result.start);
            try {
                AbstractStance stance;
                if (opcode == Opcode.LDC) {
                    String stanceName = cp.getLdcValue(ci.byteAt(result.start + 1)).toString();
                    stance = AbstractStance.getStanceFromName(stanceName);
                } else if (opcode == Opcode.LDC_W) {
                    String stanceName = cp.getLdcValue(ci.u16bitAt(result.start + 1)).toString();
                    stance = AbstractStance.getStanceFromName(stanceName);
                } else if (opcode == Opcode.NEW) {
                    CtClass stanceClass = classPool.get(cp.getClassInfo(ci.s16bitAt(result.start + 1)));
                    stance = (AbstractStance) Class.forName(stanceClass.getName()).newInstance();
                } else {
                    stance = null;
                }

                if (stance != null && cardInfo.changeToStances.stream().noneMatch(s -> s.getClass() == stance.getClass())) {
                    cardInfo.changeToStances.add(stance);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void findOrbsFromCard(ClassPool classPool, CtMethod useMethod, CardInfo cardInfo) throws BadBytecode {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        ConstPool cp = ca.getConstPool();

        List<CodePattern.Range> results = CodePattern.find(ci, CodePattern.sequence(
                new CodePattern(Opcode.NEW, (i, l) -> {
                    int argument = i.s16bitAt(l + 1);
                    try {
                        CtClass candidate = classPool.get(cp.getClassInfo(argument));
                        return candidate.subclassOf(classPool.get(AbstractOrb.class.getName()));
                    } catch (NotFoundException e) {
                        return false;
                    }
                }),
                new CodePattern(Opcode.DUP),
                new CodePattern(Opcode.INVOKESPECIAL),
                new CodePattern(Opcode.INVOKESPECIAL, (i, l) -> {
                    int argument = i.s16bitAt(l + 1);
                    return cp.getMethodrefClassName(argument).equals(ChannelAction.class.getName());
                })
        ));

        for (CodePattern.Range result : results) {
            int opcode = ci.byteAt(result.start);
            try {
                AbstractOrb orb;
                if (opcode == Opcode.NEW) {
                    CtClass orbClass = classPool.get(cp.getClassInfo(ci.s16bitAt(result.start + 1)));
                    orb = (AbstractOrb) Class.forName(orbClass.getName()).newInstance();
                } else {
                    orb = null;
                }

                if (orb != null && cardInfo.channelOrbs.stream().noneMatch(s -> s.getClass() == orb.getClass())) {
                    cardInfo.channelOrbs.add(orb);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void calculateStancesScore() {
        useStances.clear();
        stanceScores.clear();

        for (Map.Entry<AbstractCard, CardInfo> entry : cardInfoMap.entrySet()) {
            for (AbstractStance stance : entry.getValue().changeToStances) {
                if (useStances.stream().anyMatch(s -> s.getClass() == stance.getClass())) {
                    continue;
                }

                useStances.add(stance);
                Class<? extends AbstractStance> stanceClass = stance.getClass();
                if (predefinedStanceScores.containsKey(stanceClass)) {
                    stanceScores.put(stanceClass, predefinedStanceScores.get(stanceClass));
                } else {
                    stanceScores.put(stanceClass, calculateStanceScore(stanceClass));
                }
            }
        }

        useStances.sort(Comparator.comparing(o -> o.ID));
    }

    private static void calculateOrbsScore() {
        useOrbs.clear();
        orbScores.clear();

        for (Map.Entry<AbstractCard, CardInfo> entry : cardInfoMap.entrySet()) {
            for (AbstractOrb orb : entry.getValue().channelOrbs) {
                if (useOrbs.stream().anyMatch(s -> s.getClass() == orb.getClass())) {
                    continue;
                }

                useOrbs.add(orb);
                Class<? extends AbstractOrb> orbClass = orb.getClass();
                if (predefinedOrbScores.containsKey(orbClass)) {
                    orbScores.put(orbClass, predefinedOrbScores.get(orbClass));
                } else {
                    orbScores.put(orbClass, calculateOrbScore(orb));
                }
            }
        }

        useOrbs.sort(Comparator.comparing(o -> o.ID));
    }

    private static int calculateStanceScore(Class<? extends AbstractStance> stanceClass) {
        return calculateScore(cardInfoMap.values().stream()
                .filter(c -> c.changeToStances.stream().anyMatch(s -> s.getClass() == stanceClass))
                .collect(Collectors.toMap(v -> v, v -> 1)));
    }

    private static int calculateOrbScore(AbstractOrb orb) {
        return calculateScore(cardInfoMap.values().stream()
                .filter(c -> c.channelOrbs.stream().anyMatch(s -> s.getClass() == orb.getClass()))
                .collect(Collectors.toMap(v -> v, v -> findOrbCount(v, orb))));
    }

    private static int findOrbCount(CardInfo cardInfo, AbstractOrb orb) {
        String orbName = orb.name;
        String orbKeyword = getKeyword(orbName);

        try {
            Pattern channelOrbs = Pattern.compile(String.format(analyzerStrings.get("ChannelOrbs"), orbKeyword), Pattern.CASE_INSENSITIVE);
            Matcher m = channelOrbs.matcher(cardInfo.card.rawDescription);
            if (!m.find()) {
                return 1;
            }

            if (m.group(1).equals("!M!")) {
                return cardInfo.card.magicNumber;
            } else {
                return Integer.parseInt(m.group(1), 10);
            }
        } catch (Exception ex) {
            return 1;
        }
    }

    public static String getKeyword(String orbName) {
        return GameDictionary.parentWord.containsKey(orbName) ? orbName :
                GameDictionary.parentWord.keySet().stream().filter(k -> k.endsWith(":" + orbName)).findFirst().orElse(orbName);
    }

    private static int calculateScore(Map<CardInfo, Integer> relatedCards) {
        int count = 0;
        float sum = 0;
        for (Map.Entry<CardInfo, Integer> entry : relatedCards.entrySet()) {
            CardInfo cardInfo = entry.getKey();
            AbstractCard card = cardInfo.card;
            int targetActionCount = entry.getValue();
            float score = calculateScore(cardInfo, card, targetActionCount);
            sum += score;
            count++;
            AbstractCard cardCopy = card.makeCopy();
            cardCopy.upgrade();
            float upgradeScore = calculateScore(cardInfo, cardCopy, targetActionCount);
            sum += upgradeScore;
            count++;
        }

        return count == 0 ? 6 : Math.round(sum / count);
    }

    private static float calculateScore(CardInfo cardInfo, AbstractCard card, int targetActionCount) {
        int cost = card.cost;
        int actionCount = cardInfo.actionCount;
        if (cost > 0 && card.rarity == AbstractCard.CardRarity.BASIC) {
            cost--;
        }
        if (cost == -1 || cost == -2) {
            cost = 1;
        }
        float score = cost < costScoreMap.length ? costScoreMap[cost] : 60;
        if (card.exhaust || card.type == AbstractCard.CardType.POWER) {
            score *= 1.7;
        }
        if (card.retain) {
            score *= 0.9;
        }
        if (card.isEthereal) {
            score *= 1.15;
        }
        if (card.baseDamage > 0 && card.type == AbstractCard.CardType.ATTACK) {
            score -= (card.baseDamage < 4 ? card.baseDamage / 2f : card.baseDamage - 2);
            actionCount -= 1;
        }
        if (card.baseBlock > 0) {
            score -= card.baseBlock;
            actionCount -= 1;
        }
        if (actionCount <= 0) {
            actionCount = 0;
        }
        score /= actionCount + (targetActionCount - 1);
        return score;
    }

    private static void setAffectedByFocus() {
        affectedByFocus = !useOrbs.isEmpty();
    }

    public static class CardInfo {
        AbstractCard card;
        int actionCount;
        int forwardBranchCount;
        int backwardBranchCount;
        List<AbstractStance> changeToStances = new ArrayList<>();
        List<AbstractOrb> channelOrbs = new ArrayList<>();
        Map<Class<? extends AbstractPower>, Integer> gainPowers = new HashMap<>();
        Map<Class<? extends AbstractPower>, Integer> applyPowers = new HashMap<>();
    }
}
