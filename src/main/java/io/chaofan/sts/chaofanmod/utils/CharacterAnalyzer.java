package io.chaofan.sts.chaofanmod.utils;

import basemod.BaseMod;
import basemod.abstracts.DynamicVariable;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.*;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.stances.*;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.chaofan.sts.CommonModUtils.getLocalizationFilePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.MOD_ID;

public class CharacterAnalyzer {
    public static final int[] costScoreMap = new int[] { 4, 9, 16, 30, 45, 60 };
    public static final int[] costScoreUpgradeMap = new int[] { 3, 4, 7, 10, 13, 16 };

    public static AbstractCard.CardColor playerColor;
    public static List<StanceInfo> useStances = new ArrayList<>();
    public static List<OrbInfo> useOrbs = new ArrayList<>();
    public static boolean affectedByFocus = false;
    public static Map<AbstractCard, CardInfo> cardInfoMap;
    public static List<PowerInfo> applyPowers = new ArrayList<>();
    public static List<PowerInfo> gainPowers = new ArrayList<>();

    private static final Map<String, CardInfo> referencedMethods = new HashMap<>();

    private static final Map<Class<? extends AbstractStance>, Integer> predefinedStanceScores = new HashMap<>();
    private static final Map<Class<? extends AbstractOrb>, Integer> predefinedOrbScores = new HashMap<>();
    private static final Map<String, String> analyzerStrings;

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
        calculatePowerScores();

        ChaofanMod.logger.info("CharacterAnalyzer:");
        ChaofanMod.logger.info("Stances: " + useStances.stream().map(s -> s.stance.name).collect(Collectors.toList()));
        ChaofanMod.logger.info("Orbs: " + useOrbs.stream().map(o -> o.orb.name).collect(Collectors.toList()));
        ChaofanMod.logger.info("ApplyPowers: " + applyPowers.stream().map(p -> p.name).collect(Collectors.toList()));
        ChaofanMod.logger.info("GainPowers: " + gainPowers.stream().map(p -> p.name).collect(Collectors.toList()));

        referencedMethods.clear();
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
                try { findNewActions(classPool, useMethod, cardInfo); } catch (Exception e) { e.printStackTrace(); }
                try { findBranches(useMethod, cardInfo); } catch (Exception e) { e.printStackTrace(); }
                try { findStanceFromCard(classPool, useMethod, cardInfo); } catch (Exception e) { e.printStackTrace(); }
                try { findOrbsFromCard(classPool, useMethod, cardInfo); } catch (Exception e) { e.printStackTrace(); }
                try { findApplyPowerFromCard(classPool, useMethod, cardInfo, 1); } catch (Exception e) { e.printStackTrace(); }
                try { mergeReferencedMethods(classPool, useMethod, cardInfo, 1); } catch (Exception e) { e.printStackTrace(); }
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

    private static void findApplyPowerFromCard(ClassPool classPool, CtMethod useMethod, CardInfo cardInfo, int playerParameterIndex) throws BadBytecode, ClassNotFoundException {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        ConstPool cp = ca.getConstPool();

        List<CodePattern.Range> applyPowerActions = getApplyPowerActions(ci, cp);
        List<CodePattern.Range> powers = getNewAbstractPowers(classPool, ci, cp);

        for (CodePattern.Range power : powers) {
            CodePattern.Range applyPowerAction = applyPowerActions.stream().filter(a -> a.contains(power)).findFirst().orElse(null);
            if (applyPowerAction == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends AbstractPower> powerClass = (Class<? extends AbstractPower>) Class.forName(cp.getClassInfo(ci.s16bitAt(power.start + 1)));
            String powerId = getPowerId(powerClass);
            if (powerId == null || powerId.equals(WeakPower.POWER_ID) || powerId.equals(VulnerablePower.POWER_ID) ||
                    powerId.equals(ArtifactPower.POWER_ID) || powerId.equals(IntangiblePlayerPower.POWER_ID) ||
                    powerId.equals(StrengthPower.POWER_ID) || powerId.equals(DexterityPower.POWER_ID) ||
                    powerId.equals(FocusPower.POWER_ID)) {
                continue;
            }

            PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(powerId);
            String powerName = powerStrings != null ? powerStrings.NAME : powerId;
            String keyword = getKeyword(powerName);

            boolean hasGainPower;
            if (cardInfo.card != null) {
                Pattern gainPowerPattern = Pattern.compile(String.format(analyzerStrings.get("GainPower"), keyword), Pattern.CASE_INSENSITIVE);
                hasGainPower = gainPowerPattern.matcher(cardInfo.card.rawDescription).find();
                Pattern applyPowerPattern = Pattern.compile(String.format(analyzerStrings.get("ApplyPower"), keyword), Pattern.CASE_INSENSITIVE);
                boolean hasApplyPower = applyPowerPattern.matcher(cardInfo.card.rawDescription).find();
                if (!hasGainPower && !hasApplyPower) {
                    continue;
                }
            } else {
                hasGainPower = false;
            }

            boolean isToPlayer = isAloadN(ci, applyPowerAction.start + 4, playerParameterIndex) ||
                    isAbstractDungeonPlayer(ci, applyPowerAction.start + 4);
            boolean isGain = isToPlayer || hasGainPower;

            if (isGain && !cardInfo.gainPowers.containsKey(powerClass)) {
                cardInfo.gainPowers.put(powerClass, makeGainPowerConstructor(powerClass, power, ci, cp));
            } else if (!cardInfo.applyPowers.containsKey(powerClass)) {
                cardInfo.applyPowers.put(powerClass, makeApplyPowerConstructor(powerClass, power, ci, cp, playerParameterIndex));
            }
        }
    }

    private static String getPowerId(Class<? extends AbstractPower> powerClass) {
        String powerId = BaseMod.getPowerKeys().stream().filter(k -> BaseMod.getPowerClass(k) == powerClass).findFirst().orElse(null);
        if (powerId == null) {
            try {
                Field f = powerClass.getField("POWER_ID");
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                    return (String) f.get(null);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) { }

            try {
                Field f = powerClass.getField("ID");
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                    return (String) f.get(null);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) { }
        }

        return powerId;
    }

    private static List<CodePattern.Range> getApplyPowerActions(CodeIterator ci, ConstPool cp) throws BadBytecode {
        List<CodePattern.Range> newApplyPowerActions = CodePattern.find(ci, CodePattern.sequence(
                new CodePattern(Opcode.NEW, (i, l) -> {
                    int argument = i.s16bitAt(l + 1);
                    return cp.getClassInfo(argument).equals(ApplyPowerAction.class.getName());
                }),
                new CodePattern(Opcode.DUP)
        ));

        List<CodePattern.Range> constructApplyPowerActions = CodePattern.find(ci, new CodePattern(Opcode.INVOKESPECIAL, (i, l) -> {
            int argument = i.s16bitAt(l + 1);
            return cp.getMethodrefClassName(argument).equals(ApplyPowerAction.class.getName());
        }));

        return matchStartEnd(newApplyPowerActions, constructApplyPowerActions);
    }

    private static List<CodePattern.Range> getNewAbstractPowers(ClassPool classPool, CodeIterator ci, ConstPool cp) throws BadBytecode {
        List<CodePattern.Range> newPowers = CodePattern.find(ci, CodePattern.sequence(
                new CodePattern(Opcode.NEW, (i, l) -> {
                    int argument = i.s16bitAt(l + 1);
                    try {
                        CtClass candidate = classPool.get(cp.getClassInfo(argument));
                        return candidate.subclassOf(classPool.get(AbstractPower.class.getName()));
                    } catch (NotFoundException e) {
                        return false;
                    }
                }),
                new CodePattern(Opcode.DUP)
        ));

        List<CodePattern.Range> constructPowers = CodePattern.find(ci, new CodePattern(Opcode.INVOKESPECIAL, (i, l) -> {
            int argument = i.s16bitAt(l + 1);
            try {
                CtClass candidate = classPool.get(cp.getMethodrefClassName(argument));
                return candidate.subclassOf(classPool.get(AbstractPower.class.getName()));
            } catch (NotFoundException e) {
                return false;
            }
        }));

        return matchStartEnd(newPowers, constructPowers);
    }

    private static List<CodePattern.Range> matchStartEnd(List<CodePattern.Range> startRanges, List<CodePattern.Range> endRanges) {
        List<CodePattern.Range> results = new ArrayList<>();
        for (CodePattern.Range startRange : startRanges) {
            int start = startRange.start;
            endRanges.stream()
                    .filter(construct -> construct.start > start &&
                            startRanges.stream().allMatch(np -> np.start <= start || np.start > construct.end))
                    .findFirst()
                    .ifPresent(constructPower -> results.add(new CodePattern.Range(start, constructPower.end)));
        }
        return results;
    }

    private static PowerFactory makeGainPowerConstructor(Class<? extends AbstractPower> powerClass, CodePattern.Range power, CodeIterator ci, ConstPool cp) throws ClassNotFoundException {
        String signature = cp.getMethodrefType(ci.s16bitAt(power.end - 2));
        List<Class<?>> parameterList = getParameterList(signature);
        if ((parameterList.size() == 2 || parameterList.size() == 3) && parameterList.stream().filter(c -> c == int.class).count() == 1
                && parameterList.stream().filter(c -> c.isAssignableFrom(AbstractPlayer.class)).count() == parameterList.size() - 1) {
            return new GainPowerFactory(powerClass, parameterList);
        }
        return null;
    }

    private static PowerFactory makeApplyPowerConstructor(Class<? extends AbstractPower> powerClass, CodePattern.Range power, CodeIterator ci, ConstPool cp, int playerParameterIndex) throws ClassNotFoundException, BadBytecode {
        String signature = cp.getMethodrefType(ci.s16bitAt(power.end - 2));
        List<Class<?>> parameterList = getParameterList(signature);
        if (parameterList.size() == 2 && parameterList.stream().filter(c -> c == int.class).count() == 1
                && parameterList.stream().filter(c -> c.isAssignableFrom(AbstractMonster.class)).count() == parameterList.size() - 1) {
            return new ApplyPowerFactory(powerClass, parameterList);
        }
        if (parameterList.size() == 3 && parameterList.stream().filter(c -> c == int.class).count() == 1
                && parameterList.stream().filter(c -> c.isAssignableFrom(AbstractMonster.class)).count() > 1
                && parameterList.stream().filter(c -> c.isAssignableFrom(AbstractPlayer.class)).count() > 1
                && parameterList.stream().noneMatch(c -> c != int.class && !c.isAssignableFrom(AbstractMonster.class) && !c.isAssignableFrom(AbstractPlayer.class))) {

            int[] stack = StackAnalyzer.getStacks(ci, power.start, power.end - 3);
            if (stack != null) {
                int offset = stack.length - parameterList.size();
                for (int i = 0; i < stack.length; i++) {
                    int pos = stack[i];
                    if ((isAloadN(ci, pos, playerParameterIndex) || isAbstractDungeonPlayer(ci, pos)) &&
                            parameterList.get(i - offset).isAssignableFrom(AbstractPlayer.class)) {
                        return new ApplyPowerFactory(powerClass, parameterList, i - offset);
                    }
                }
            }
        }
        return null;
    }

    private static List<Class<?>> getParameterList(String descriptor) throws ClassNotFoundException {
        List<Class<?>> result = new ArrayList<>();
        String paramList = descriptor.substring(1, descriptor.indexOf(')'));
        int pointer = 0;
        while (pointer < paramList.length()) {
            int start = pointer;
            checkLoop: while (true) {
                switch (paramList.charAt(pointer)) {
                    case 'B':
                        result.add(byte.class);
                        pointer++;
                        break checkLoop;
                    case 'C':
                        result.add(char.class);
                        pointer++;
                        break checkLoop;
                    case 'D':
                        result.add(double.class);
                        pointer++;
                        break checkLoop;
                    case 'F':
                        result.add(float.class);
                        pointer++;
                        break checkLoop;
                    case 'I':
                        result.add(int.class);
                        pointer++;
                        break checkLoop;
                    case 'J':
                        result.add(long.class);
                        pointer++;
                        break checkLoop;
                    case 'S':
                        result.add(short.class);
                        pointer++;
                        break checkLoop;
                    case 'Z':
                        result.add(boolean.class);
                        pointer++;
                        break checkLoop;
                    case 'L':
                        pointer = paramList.indexOf(';', pointer) + 1;
                        result.add(Class.forName(paramList.substring(start + 1, pointer - 1).replace('/', '.')));
                        break checkLoop;
                    case '[':
                        pointer++;
                        break;
                    default:
                        throw new RuntimeException("Invalid type");
                }
            }
            if (paramList.charAt(start) == '[') {
                result.add(Class.forName(paramList.substring(start, pointer)));
            }
        }
        return result;
    }

    private static boolean isAloadN(CodeIterator ci, int index, int n) {
        if (n < 0) {
            return false;
        }

        int op = ci.byteAt(index);
        if (op == Opcode.ALOAD) {
            return ci.byteAt(index + 1) == n;
        } else if (op >= Opcode.ALOAD_0 && op <= Opcode.ALOAD_3) {
            return (op - Opcode.ALOAD_0) == n;
        }
        return false;
    }

    private static boolean isAbstractDungeonPlayer(CodeIterator ci, int index) {
        int op = ci.byteAt(index);
        if (op == Opcode.GETSTATIC) {
            int argument = ci.s16bitAt(index + 1);
            ConstPool constPool = ci.get().getConstPool();
            return constPool.getFieldrefClassName(argument).equals(AbstractDungeon.class.getName()) &&
                    constPool.getFieldrefName(argument).equals("player");
        }

        return false;
    }

    private static void mergeReferencedMethods(ClassPool classPool, CtMethod useMethod, CardInfo cardInfo, int playerParameterIndex) throws BadBytecode, ClassNotFoundException, NotFoundException {
        MethodInfo methodInfo = useMethod.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        ConstPool cp = ca.getConstPool();
        List<CodePattern.Range> methodCalls = CodePattern.find(ci, CodePattern.anyOf(
                Opcode.INVOKESTATIC, Opcode.INVOKEVIRTUAL
        ));

        for (CodePattern.Range methodCall : methodCalls) {
            int argument = ci.u16bitAt(methodCall.start + 1);
            String desc = cp.getMethodrefType(argument);
            List<Class<?>> parameterList = getParameterList(desc);
            int[] stack = StackAnalyzer.getStacks(ci, 0, methodCall.start);
            int childPlayerParameterIndex = -1;
            if (stack != null) {
                int offset = stack.length - parameterList.size();
                for (int i = offset; i < stack.length; i++) {
                    int pos = stack[i];
                    if ((isAloadN(ci, pos, playerParameterIndex) || isAbstractDungeonPlayer(ci, pos)) &&
                            parameterList.get(i - offset).isAssignableFrom(AbstractPlayer.class)) {
                        childPlayerParameterIndex = i - offset;
                        break;
                    }
                }
            }

            String className = cp.getMethodrefClassName(argument);
            CtClass targetClass = classPool.get(className);
            CtMethod targetMethod = targetClass.getMethod(cp.getMethodrefName(argument), desc);
            if (Modifier.isAbstract(targetMethod.getModifiers())) {
                continue;
            }

            CardInfo referencedMethod = getReferencedMethodInfo(classPool, targetMethod, childPlayerParameterIndex);

            cardInfo.actionCount += referencedMethod.actionCount;
            referencedMethod.applyPowers.forEach((k, v) -> cardInfo.applyPowers.putIfAbsent(k, v));
            referencedMethod.gainPowers.forEach((k, v) -> cardInfo.gainPowers.putIfAbsent(k, v));
            referencedMethod.channelOrbs.forEach(o -> {
                if (cardInfo.channelOrbs.stream().noneMatch(o1 -> o1.getClass() == o.getClass())) {
                    cardInfo.channelOrbs.add(o);
                }
            });
            referencedMethod.changeToStances.forEach(s -> {
                if (cardInfo.changeToStances.stream().noneMatch(s1 -> s1.getClass() == s.getClass())) {
                    cardInfo.changeToStances.add(s);
                }
            });
        }
    }

    private static CardInfo getReferencedMethodInfo(ClassPool classPool, CtMethod method, int playerParameterIndex) {
        String key = method.getDeclaringClass().getName() + "." + method.getName() + method.getSignature() + "_" + playerParameterIndex;
        CardInfo existingCardInfo = referencedMethods.get(key);
        if (existingCardInfo != null) {
            return existingCardInfo;
        }

        CardInfo cardInfo = new CardInfo();
        try { findNewActions(classPool, method, cardInfo); } catch (Exception e) { e.printStackTrace(); }
        try { findBranches(method, cardInfo); } catch (Exception e) { e.printStackTrace(); }
        try { findStanceFromCard(classPool, method, cardInfo); } catch (Exception e) { e.printStackTrace(); }
        try { findOrbsFromCard(classPool, method, cardInfo); } catch (Exception e) { e.printStackTrace(); }
        try { findApplyPowerFromCard(classPool, method, cardInfo, playerParameterIndex); } catch (Exception e) { e.printStackTrace(); }
        referencedMethods.put(key, cardInfo);
        return cardInfo;
    }

    private static void calculateStancesScore() {
        useStances.clear();

        for (Map.Entry<AbstractCard, CardInfo> entry : cardInfoMap.entrySet()) {
            for (AbstractStance stance : entry.getValue().changeToStances) {
                if (useStances.stream().anyMatch(s -> s.stance.getClass() == stance.getClass())) {
                    continue;
                }

                StanceInfo stanceInfo = new StanceInfo();
                stanceInfo.stance = stance;

                useStances.add(stanceInfo);
                Class<? extends AbstractStance> stanceClass = stance.getClass();
                if (predefinedStanceScores.containsKey(stanceClass)) {
                    stanceInfo.score = predefinedStanceScores.get(stanceClass);
                } else {
                    stanceInfo.score = calculateStanceScore(stanceClass);
                }
            }
        }

        useStances.sort(Comparator.comparing(o -> o.stance.ID));
    }

    private static void calculateOrbsScore() {
        useOrbs.clear();

        for (Map.Entry<AbstractCard, CardInfo> entry : cardInfoMap.entrySet()) {
            for (AbstractOrb orb : entry.getValue().channelOrbs) {
                if (useOrbs.stream().anyMatch(s -> s.orb.getClass() == orb.getClass())) {
                    continue;
                }

                OrbInfo orbInfo = new OrbInfo();
                orbInfo.orb = orb;
                useOrbs.add(orbInfo);
                Class<? extends AbstractOrb> orbClass = orb.getClass();
                if (predefinedOrbScores.containsKey(orbClass)) {
                    orbInfo.score = predefinedOrbScores.get(orbClass);
                } else {
                    orbInfo.score = calculateOrbScore(orb);
                }
            }
        }

        useOrbs.sort(Comparator.comparing(o -> o.orb.ID));
    }

    private static int calculateStanceScore(Class<? extends AbstractStance> stanceClass) {
        return calculateScore(
                cardInfoMap.values().stream()
                    .filter(c -> c.changeToStances.stream().anyMatch(s -> s.getClass() == stanceClass))
                    .collect(Collectors.toList()),
                (c, ci) -> 1);
    }

    private static int calculateOrbScore(AbstractOrb orb) {
        return calculateScore(
                cardInfoMap.values().stream()
                    .filter(c -> c.channelOrbs.stream().anyMatch(s -> s.getClass() == orb.getClass()))
                    .collect(Collectors.toList()),
                (c, ci) -> findOrbCount(c, orb));
    }

    private static int findOrbCount(AbstractCard card, AbstractOrb orb) {
        String orbName = orb.name;
        String orbKeyword = getKeyword(orbName);

        try {
            Pattern channelOrbs = Pattern.compile(String.format(analyzerStrings.get("ChannelOrbs"), orbKeyword), Pattern.CASE_INSENSITIVE);
            Matcher m = channelOrbs.matcher(card.rawDescription);
            if (!m.find()) {
                return 1;
            }

            return getNumberOrMagicNumber(card, m.group(1));
        } catch (Exception ex) {
            return 1;
        }
    }

    public static String getKeyword(String orbName) {
        return GameDictionary.parentWord.containsKey(orbName) ? orbName :
                GameDictionary.parentWord.keySet().stream().filter(k -> k.endsWith(":" + orbName)).findFirst().orElse(orbName);
    }

    private static int calculateScore(List<CardInfo> relatedCards, BiFunction<AbstractCard, CardInfo, Integer> getTargetActionCount) {
        int count = 0;
        float sum = 0;
        for (CardInfo cardInfo : relatedCards) {
            AbstractCard card = cardInfo.card;
            int targetActionCount = getTargetActionCount.apply(card, cardInfo);
            float score = calculateScore(cardInfo, card, targetActionCount);
            sum += score;
            count++;
            AbstractCard cardCopy = card.makeCopy();
            cardCopy.upgrade();
            int upgradeTargetActionCount = getTargetActionCount.apply(cardCopy, cardInfo);
            float upgradeScore = calculateScore(cardInfo, cardCopy, upgradeTargetActionCount);
            sum += upgradeScore;
            count++;
        }

        return count == 0 ? 6 : Math.max(1, Math.round(sum / count));
    }

    private static float calculateScore(CardInfo cardInfo, AbstractCard card, int targetActionCount) {
        int actionCount = cardInfo.actionCount;
        int cost = card.cost;
        if (cost == -1 || cost == -2) {
            cost = 1;
        }
        float score = cost < costScoreMap.length ? costScoreMap[cost] : 60;
        if (card.upgraded) {
            score += cost < costScoreUpgradeMap.length ? costScoreUpgradeMap[cost] : 16;
        }
        if (card.rarity == AbstractCard.CardRarity.BASIC) {
            score *= 0.7;
        }
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
            actionCount = 1;
        }
        score /= actionCount + (targetActionCount - 1);
        return score;
    }

    private static void setAffectedByFocus() {
        affectedByFocus = !useOrbs.isEmpty();
    }

    private static void calculatePowerScores() {
        applyPowers.clear();
        gainPowers.clear();

        calculatePowerScores(applyPowers, c -> c.applyPowers, "ApplyPower");
        calculatePowerScores(gainPowers, c -> c.gainPowers, "GainPower");

        applyPowers.sort(Comparator.comparing(p -> p.powerId));
        gainPowers.sort(Comparator.comparing(p -> p.powerId));
    }

    private static void calculatePowerScores(List<PowerInfo> powerInfoList, Function<CardInfo, Map<Class<? extends AbstractPower>, PowerFactory>> getPowerMap, String patternKey) {
        for (CardInfo cardInfo : cardInfoMap.values()) {
            for (Map.Entry<Class<? extends AbstractPower>, PowerFactory> entry : getPowerMap.apply(cardInfo).entrySet()) {
                Class<? extends AbstractPower> powerClass = entry.getKey();
                PowerInfo existingPowerInfo = powerInfoList.stream().filter(p -> p.powerClass == powerClass).findFirst().orElse(null);
                PowerFactory powerFactory = entry.getValue();
                if (existingPowerInfo != null) {
                    if (existingPowerInfo.powerFactory == null) {
                        existingPowerInfo.powerFactory = powerFactory;
                    }
                    continue;
                }

                PowerInfo powerInfo = new PowerInfo();
                powerInfo.powerClass = powerClass;
                powerInfo.powerFactory = powerFactory;
                powerInfo.powerId = getPowerId(powerClass);
                PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(powerInfo.powerId);
                powerInfo.name = powerStrings == null ? powerInfo.powerId : powerStrings.NAME;
                powerInfo.scores = calculatePowerScore(powerInfo, getPowerMap, patternKey);
                powerInfoList.add(powerInfo);
            }
        }

        powerInfoList.removeIf(p -> p.powerFactory == null || p.scores.length == 0);
    }

    private static int[] calculatePowerScore(PowerInfo powerInfo, Function<CardInfo, Map<Class<? extends AbstractPower>, PowerFactory>> getPowerMap, String patternKey) {
        List<PointPair> points = new ArrayList<>();
        for (CardInfo cardInfo : cardInfoMap.values()) {
            if (!getPowerMap.apply(cardInfo).containsKey(powerInfo.powerClass)) {
                continue;
            }

            AbstractCard card = cardInfo.card.makeCopy();
            float count = findPowerCount(card, powerInfo, patternKey);
            if (count > 0) {
                if (card.target == AbstractCard.CardTarget.ALL || card.target == AbstractCard.CardTarget.ALL_ENEMY) {
                    count /= 0.8;
                }
                float score = calculateScore(cardInfo, card, 1);
                points.add(new PointPair(count, score));
            }

            card.upgrade();
            count = findPowerCount(card, powerInfo, patternKey);
            if (count > 0) {
                if (patternKey.equals("ApplyPower") &&
                        (card.target == AbstractCard.CardTarget.ALL || card.target == AbstractCard.CardTarget.ALL_ENEMY)) {
                    count /= 0.8;
                }
                float score = calculateScore(cardInfo, card, 1);
                points.add(new PointPair(count, score));
            }
        }

        points.add(new PointPair(0, 0));
        int pointCount = points.size();
        if (pointCount == 1) {
            return new int[0];
        }

        int maxPower = (int) Math.ceil(points.stream().max(Comparator.comparingDouble(p -> p.x)).get().x);
        if (maxPower > 100) {
            return new int[0];
        }

        int[] result = new int[maxPower + 1];
        if (pointCount < 5) {
            float sumXSqr = 0;
            float sumXy = 0;
            for (PointPair p : points) {
                sumXSqr += p.x * p.x;
                sumXy += p.x * p.y;
            }
            float scorePerCount = sumXy / sumXSqr;
            if (scorePerCount < 0) {
                return new int[0];
            }
            for (int i = 1; i < result.length; i++) {
                result[i] = Math.max(1, Math.round(scorePerCount * i));
            }
            result[0] = 0;
            return result;
        }

        float sumXSqr = 0;
        float sumXy = 0;
        float sumX = 0;
        float sumY = 0;
        float sum1 = 0;
        for (int i = pointCount - 1; i >= 0; i--) {
            PointPair p = points.get(i);
            sumXSqr += p.x * p.x;
            sumXy += p.x * p.y;
            sumX += p.x;
            sumY += p.y;
            sum1 += 1;
        }

        float b = (sumXy / sumXSqr - sumY / sumX) / (sumX / sumXSqr - sum1 / sumX);
        float a = (sumY - b * sum1) / sumX;

        if (a < 0) {
            return new int[0];
        }

        for (int i = 0; i < result.length; i++) {
            result[i] = Math.round(a * i + b);
        }
        result[0] = 0;
        int firstPositiveIndex = 0;
        for (int i = 1; i < result.length; i++) {
            if (result[i] > 0) {
                firstPositiveIndex = i;
                break;
            }
        }
        if (firstPositiveIndex == 0) {
            return new int[0];
        }
        for (int i = 1; i < firstPositiveIndex; i++) {
            result[i] = Math.max(1, Math.round((a * firstPositiveIndex + b) / firstPositiveIndex * i));
        }

        return result;
    }

    private static int findPowerCount(AbstractCard card, PowerInfo powerInfo, String patternKey) {
        PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(powerInfo.powerId);
        String powerName = powerStrings != null ? powerStrings.NAME : powerInfo.powerId;
        String powerKeyword = getKeyword(powerName);

        try {
            Pattern powerPattern = Pattern.compile(String.format(analyzerStrings.get(patternKey), powerKeyword), Pattern.CASE_INSENSITIVE);
            Matcher m = powerPattern.matcher(card.rawDescription);
            if (!m.find()) {
                return 0;
            }

            int baseCount;
            int times;
            String baseGroup = m.group(1);
            baseCount = getNumberOrMagicNumber(card, baseGroup);
            String timeGroup = m.group(2);
            if (timeGroup != null && timeGroup.length() > 0) {
                times = getNumberOrMagicNumber(card, timeGroup);
            } else {
                times = 1;
            }
            return baseCount * times;
        } catch (Exception ex) {
            return 0;
        }
    }

    private static int getNumberOrMagicNumber(AbstractCard card, String value) {
        String trimmedValue = value.trim();
        if (trimmedValue.equals("!M!")) {
            return card.magicNumber;
        } else if (trimmedValue.startsWith("!")) {
            DynamicVariable dynamicVariable = BaseMod.cardDynamicVariableMap.get(trimmedValue.substring(1, trimmedValue.length() - 1));
            if (dynamicVariable == null) {
                return 0;
            }
            return dynamicVariable.baseValue(card);
        } else {
            return Integer.parseInt(value, 10);
        }
    }

    public static class CardInfo {
        AbstractCard card;
        int actionCount;
        int forwardBranchCount;
        int backwardBranchCount;
        List<AbstractStance> changeToStances = new ArrayList<>();
        List<AbstractOrb> channelOrbs = new ArrayList<>();
        Map<Class<? extends AbstractPower>, PowerFactory> gainPowers = new HashMap<>();
        Map<Class<? extends AbstractPower>, PowerFactory> applyPowers = new HashMap<>();
    }

    public static class StanceInfo {
        public AbstractStance stance;
        public int score;
        public AbstractStance newInstance() {
            try {
                return stance.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                return stance;
            }
        }
    }

    public static class OrbInfo {
        public AbstractOrb orb;
        public int score;
        public AbstractOrb newInstance() {
            return orb.makeCopy();
        }
    }

    public static class PowerInfo {
        public Class<? extends AbstractPower> powerClass;
        public String powerId;
        public int[] scores;
        public PowerFactory powerFactory;
        public String name;

        public AbstractPower newInstance(AbstractPlayer player, AbstractMonster monster, int amount) {
            return powerFactory.createPower(player, monster, amount);
        }
    }

    static class PointPair {
        public final float x;
        public final float y;

        public PointPair(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface PowerFactory {
        AbstractPower createPower(AbstractPlayer player, AbstractMonster monster, int amount);
    }

    private static class GainPowerFactory implements PowerFactory {
        private final Class<? extends AbstractPower> powerClass;
        private final Class<?>[] parameterList;

        public GainPowerFactory(Class<? extends AbstractPower> powerClass, List<Class<?>> parameterList) {
            this.powerClass = powerClass;
            this.parameterList = parameterList.toArray(new Class<?>[0]);
        }

        @Override
        public AbstractPower createPower(AbstractPlayer player, AbstractMonster monster, int amount) {
            Object[] arguments = new Object[parameterList.length];
            for (int i = 0, parameterListLength = parameterList.length; i < parameterListLength; i++) {
                Class<?> aClass = parameterList[i];
                if (aClass == int.class) {
                    arguments[i] = amount;
                } else {
                    arguments[i] = player;
                }
            }
            try {
                return powerClass.getConstructor(parameterList).newInstance(arguments);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class ApplyPowerFactory implements PowerFactory {
        private final Class<? extends AbstractPower> powerClass;
        private final Class<?>[] parameterList;
        private final int playerParameterIndex;

        public ApplyPowerFactory(Class<? extends AbstractPower> powerClass, List<Class<?>> parameterList) {
            this(powerClass, parameterList, -1);
        }

        public ApplyPowerFactory(Class<? extends AbstractPower> powerClass, List<Class<?>> parameterList, int playerParameterIndex) {
            this.powerClass = powerClass;
            this.parameterList = parameterList.toArray(new Class<?>[0]);
            this.playerParameterIndex = playerParameterIndex;
        }

        @Override
        public AbstractPower createPower(AbstractPlayer player, AbstractMonster monster, int amount) {
            Object[] arguments = new Object[parameterList.length];
            for (int i = 0, parameterListLength = parameterList.length; i < parameterListLength; i++) {
                Class<?> aClass = parameterList[i];
                if (aClass == int.class) {
                    arguments[i] = amount;
                } else if (playerParameterIndex == i) {
                    arguments[i] = player;
                } else {
                    arguments[i] = monster;
                }
            }
            try {
                return powerClass.getConstructor(parameterList).newInstance(arguments);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
