package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import io.chaofan.sts.chaofanmod.relics.TauntMask;
import javassist.*;
import javassist.bytecode.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class TauntMaskPatches {
    private static final String AbstractMonsterClassName = AbstractMonster.class.getName();
    private static final String AbstractDungeonClassName = AbstractDungeon.class.getName();
    private static final String TauntMaskPatchesClassName = TauntMaskPatches.class.getName();

    private static final List<Boolean> canAccesses = new ArrayList<>();
    private static final List<Integer> damages = new ArrayList<>();

    @SuppressWarnings("unused")
    public static boolean initSetMove(AbstractMonster monster) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return false;
        }

        // Take effect only on turn 1
        if (GameActionManager.turn != 1 || AbstractDungeon.actionManager.turnHasEnded) {
            return false;
        }

        debug("initSetMove");
        damages.clear();
        canAccesses.clear();
        return true;
    }

    @SuppressWarnings("unused")
    public static void addSetMoveItem(boolean canAccess, AbstractMonster monster, int baseDamage, int multiplier, boolean isMultiDamage) {
        // Special case that can't be handled by code analysis.
        if (monster.getClass().getName().equals("com.megacrit.cardcrawl.monsters.city.Centurion") && damages.size() != 1) {
            canAccess = canAccess && AbstractDungeon.getMonsters().monsters.stream().filter(m -> !m.isDying && !m.isEscaping).count() <= 1;
        }

        AbstractPower power = monster.getPower(StrengthPower.POWER_ID);
        int damage = baseDamage;
        if (power != null) {
            damage += power.amount;
        }

        debug("addSetMoveItem " + baseDamage + " " + multiplier + " " + (damage * multiplier) + " " + canAccess);
        damages.add(damage * multiplier);
        canAccesses.add(canAccess);
    }

    @SuppressWarnings("unused")
    public static void addSetMoveItem(boolean canAccess, AbstractMonster monster, int baseDamage) {
        AbstractPower power = monster.getPower(StrengthPower.POWER_ID);
        int damage = baseDamage;
        if (power != null) {
            damage += power.amount;
        }

        debug("addSetMoveItem " + baseDamage + " " + damage + " " + canAccess);
        damages.add(baseDamage);
        canAccesses.add(canAccess);
    }

    @SuppressWarnings("unused")
    public static boolean modifyRandomResult(boolean originalResult, int[] trueDamages /* IFEQ next */, int[] falseDamages /* IFEQ branch */) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return originalResult;
        }

        // Take effect only on turn 1
        if (GameActionManager.turn != 1 || AbstractDungeon.actionManager.turnHasEnded) {
            return originalResult;
        }

        int maxTrueDamage = Arrays.stream(trueDamages).filter(canAccesses::get).map(damages::get).max().orElse(0);
        int maxFalseDamage = Arrays.stream(falseDamages).filter(canAccesses::get).map(damages::get).max().orElse(0);
        if (maxTrueDamage > maxFalseDamage) {
            return true;
        } else if (maxTrueDamage < maxFalseDamage) {
            return false;
        } else {
            return originalResult;
        }
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class MonsterIntentPatch {
        @SpireRawPatch
        public static void Raw(CtBehavior method) throws Exception {
            ClassPool pool = method.getDeclaringClass().getClassPool();
            List<CtClass> monsters = listAllMonsterClasses(pool);

            for (CtClass monster : monsters) {
                try {
                    patchGetMove(monster, null); // "Champ");
                } catch (Exception ex) {
                    debug("TauntMaskPatches.MonsterIntentPatch.Raw: Failed to patch monster: " + monster.getName() + ". " + ex);
                }
            }

            debug("Done");
        }
    }

    private static List<CtClass> listAllMonsterClasses(ClassPool pool) throws NotFoundException {
        CtClass abstractMonsterClass = pool.get(AbstractMonsterClassName);

        List<URI> jars = Arrays.stream(Loader.MODINFOS).map(mi -> {
            try {
                return mi.jarURL.toURI();
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        jars.add(new File(Loader.STS_JAR).getAbsoluteFile().toURI());

        List<String> classes = new ArrayList<>();
        for (URI jar : jars) {
            getClassesFromJar(jar, classes);
        }

        List<CtClass> monsters = new ArrayList<>();
        for (String clzName : classes) {
            try {
                CtClass clz = pool.get(clzName);
                if (clz.subclassOf(abstractMonsterClass) && clz != abstractMonsterClass) {
                    monsters.add(clz);
                }
            } catch (NotFoundException ignored) {
                debug("TauntMaskPatches.listAllMonsterClasses: class not found: " + clzName);
            }
        }
        return monsters;
    }

    private static void getClassesFromJar(URI jar, List<String> results) {
        File cpFile = new File(jar);

        try {
            JarFile jFile = new JarFile(cpFile);
            Enumeration<JarEntry> jEntrys = jFile.entries();

            while (jEntrys.hasMoreElements()) {
                JarEntry jClass = jEntrys.nextElement();
                String jClassPath = jClass.getName();
                if (jClassPath.endsWith(".class")) {
                    results.add(jClassPath.replace('/', '.').substring(0, jClassPath.length() - ".class".length()));
                }
            }
        } catch (IOException e) {
            debug("TauntMaskPatches.getClassesFromJar: failed to get classes from jar: " + jar);
        }
    }

    private static void patchGetMove(CtClass monster, String targetMonster) throws Exception {
        boolean showDecompile = targetMonster != null;

        if (showDecompile && !monster.getName().endsWith(targetMonster)) {
            return;
        }

        debug("TauntMaskPatches.patchGetMove: processing " + monster.getName());

        CtMethod getMove = monster.getDeclaredMethod("getMove");

        MethodInfo mi = getMove.getMethodInfo();
        ConstPool cp = mi.getConstPool();
        CodeAttribute ca = mi.getCodeAttribute();
        StackMapTable smt = (StackMapTable)ca.getAttribute(StackMapTable.tag);
        CodeIterator ci = ca.iterator();

        List<Integer> byteCodes = new ArrayList<>();
        List<Integer> byteCodeIndices = new ArrayList<>();

        int originalCodeLength = ca.length();

        getCodes(ci, byteCodes, byteCodeIndices);

        if (showDecompile) {
            printCode(ca);
        }

        List<Integer> randomConditions = findRandomConditions(byteCodes, byteCodeIndices, ca.iterator(), cp);
        if (randomConditions.isEmpty()) {
            return;
        }

        List<AttackIntentInfo> attackIntents = findAllAttackIndents(byteCodes, byteCodeIndices, ca.iterator(), cp);
        List<Integer> predictableConditions = findPredictableConditions(byteCodes, byteCodeIndices, ca.iterator(), cp);

        TreeMap<Integer, CodePiece> codeGraph = splitCodeStructure(byteCodes, byteCodeIndices, ca.iterator());
        TreeMap<CodePiece, CodePieceExtension> codeExtensions = fillPossibleSetMoves(codeGraph, attackIntents, randomConditions, predictableConditions);

        populateAttackIntentOtherConditions(attackIntents, byteCodes, byteCodeIndices, codeGraph, codeExtensions);

        List<Integer> sameFrames = new ArrayList<>();
        List<Integer> sameLocals = new ArrayList<>();
        Bytecode attackInitCode = generateAttackIntentInitialize(attackIntents, ca.iterator(), cp, sameFrames, sameLocals);

        int[] offset = { 0 };
        randomConditions.sort(Comparator.comparingInt(a -> a));
        for (int randomCondition : randomConditions) {
            if (updateConditionToBoolean(ca.iterator(), smt, randomCondition, cp, offset)) {
                insertModifyRandomResult(randomCondition, codeGraph, codeExtensions, ca.iterator(), cp, offset);
            }
        }

        insertAttackInitCode(attackInitCode, ca.iterator(), smt, sameFrames, sameLocals);
        if (ca.getMaxStack() < 6) {
            ca.setMaxStack(6);
        }

        debug("TauntMaskPatches.patchGetMove: done processing " + monster.getName() +
                ". length gain: " + originalCodeLength + " -> " + ca.length() + ".");

        if (showDecompile) {
            smt.println(System.out);
            //printCode(ca);
            System.exit(0);
        }
    }

    private static void printCode(CodeAttribute ca) throws BadBytecode {
        int lastIndex = 0;
        CodeIterator ci2 = ca.iterator();
        while (ci2.hasNext()) {
            int index = ci2.next();
            int op = ci2.byteAt(index);
            for (int i = lastIndex; i < index; i++) {
                System.out.printf(" %02X", ci2.byteAt(i));
            }
            System.out.println();
            System.out.printf("%04X: %20s", index, Mnemonic.OPCODE[op]);
            lastIndex = index + 1;
        }
        System.out.println();
    }

    private static void getCodes(CodeIterator ci, List<Integer> byteCodes, List<Integer> byteCodeIndices) throws BadBytecode {
        byteCodes.clear();
        byteCodeIndices.clear();
        while (ci.hasNext()) {
            int index = ci.next();
            int op = ci.byteAt(index);
            byteCodes.add(op);
            byteCodeIndices.add(index);
        }
    }

    private static List<Integer> findRandomConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> results = new ArrayList<>();
        results.addAll(findArgumentConditions(byteCodes, byteCodeIndices, iterator));
        results.addAll(findAiRandomConditions(byteCodes, byteCodeIndices, iterator, constPool));
        return results;
    }

    private static List<Integer> findArgumentConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator) {
        List<Integer> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            // Load first method argument
            if (byteCodes.get(i) == Opcode.ILOAD_1 ||
                    (byteCodes.get(i) == Opcode.ILOAD && iterator.byteAt(byteCodeIndices.get(i) + 1) == 1)) {
                int conditionEnd = i;
                boolean foundBiPush = false;
                if (i + 1 < size && isIconstOrBipush(byteCodes.get(i + 1))) {
                    conditionEnd = i + 1;
                    foundBiPush = true;
                }
                if (!foundBiPush && i - 1 > 0 && isIconstOrBipush(byteCodes.get(i - 1))) {
                    foundBiPush = true;
                }
                if (foundBiPush) {
                    if (conditionEnd + 1 < size) {
                        int conditionCandidateOp = byteCodes.get(conditionEnd + 1);
                        if (conditionCandidateOp == Opcode.IF_ICMPGE || conditionCandidateOp == Opcode.IF_ICMPGT ||
                                conditionCandidateOp == Opcode.IF_ICMPLE || conditionCandidateOp == Opcode.IF_ICMPLT) {
                            conditionEnd += 1;
                            result.add(byteCodeIndices.get(conditionEnd));
                            continue;
                        }
                    }
                }
                debug("TauntMaskPatches.findArgumentConditions: Found ILOAD_1 at " + Integer.toHexString(byteCodeIndices.get(i)) + " but can't parse.");
            }
        }

        debug("TauntMaskPatches.findArgumentConditions: result:" + result.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));
        return result;
    }

    private static List<Integer> findAiRandomConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> result = new ArrayList<>();
        int size = byteCodes.size();
        rootLoop: for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.GETSTATIC) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                // AbstractDungeon.aiRng
                if (constPool.getFieldrefName(argument).equals("aiRng") &&
                        constPool.getFieldrefClassName(argument).equals(AbstractDungeonClassName)) {
                    for (int j = i + 1; j < size && j <= i + 3; j++) {
                        if (byteCodes.get(j) == Opcode.INVOKEVIRTUAL) {
                            int index2 = byteCodeIndices.get(j);
                            int argument2 = iterator.s16bitAt(index2 + 1);
                            // AbstractDungeon.aiRng.random**(**)
                            if (constPool.getMethodrefName(argument2).contains("random")) {
                                for (int k = j + 1; k< size && k <= j + 3; k++) {
                                    int conditionCandidateOp = byteCodes.get(k);
                                    if (conditionCandidateOp == Opcode.INVOKEVIRTUAL) {
                                        continue rootLoop;
                                    }
                                    if (conditionCandidateOp == Opcode.IF_ICMPGE || conditionCandidateOp == Opcode.IF_ICMPGT ||
                                            conditionCandidateOp == Opcode.IF_ICMPLE || conditionCandidateOp == Opcode.IF_ICMPLT ||
                                            conditionCandidateOp == Opcode.IF_ICMPNE || conditionCandidateOp == Opcode.IF_ICMPEQ ||
                                            conditionCandidateOp == Opcode.IFEQ || conditionCandidateOp == Opcode.IFNE ||
                                            conditionCandidateOp == Opcode.IFGE || conditionCandidateOp == Opcode.IFGT ||
                                            conditionCandidateOp == Opcode.IFLE || conditionCandidateOp == Opcode.IFLT) {
                                        result.add(byteCodeIndices.get(k));
                                        continue rootLoop;
                                    }
                                }
                            }
                        }
                    }

                    debug("TauntMaskPatches.findAiRandomConditions: Found AbstractDungeon.aiRng usage at " + Integer.toHexString(byteCodeIndices.get(i)) + " but can't parse.");
                }
            }
        }

        debug("TauntMaskPatches.findAiRandomConditions: result:" + result.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));
        return result;
    }

    private static List<Integer> findPredictableConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> results = new ArrayList<>();
        results.addAll(findLastMoveConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.addAll(findSingleFieldConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.addAll(findAscensionLevelConditions(byteCodes, byteCodeIndices, iterator, constPool));
        return results;
    }

    private static List<Integer> findLastMoveConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.INVOKEVIRTUAL) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                String methodName = constPool.getMethodrefName(argument);
                if (methodName.equals("lastMove") || methodName.equals("lastMoveBefore") || methodName.equals("lastTwoMoves")) {
                    int nextOp = byteCodes.get(i + 1);
                    if (nextOp == Opcode.IFNE || nextOp == Opcode.IFEQ) {
                        result.add(byteCodeIndices.get(i + 1));
                    }
                }
            }
        }

        debug("TauntMaskPatches.findLastMoveConditions: result:" + result.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));
        return result;
    }

    private static List<Integer> findSingleFieldConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.GETFIELD) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                String fieldType = constPool.getFieldrefType(argument);
                if (fieldType.equals("Z")) {
                    int nextOp = byteCodes.get(i + 1);
                    if (nextOp == Opcode.IFNE || nextOp == Opcode.IFEQ) {
                        result.add(byteCodeIndices.get(i + 1));
                    }
                }
            }
        }

        debug("TauntMaskPatches.findSingleFieldConditions: result:" + result.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));
        return result;
    }

    private static List<Integer> findAscensionLevelConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Integer> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            // Load first method argument
            if (byteCodes.get(i) == Opcode.GETSTATIC) {
                int argument = iterator.s16bitAt(byteCodeIndices.get(i) + 1);
                if (constPool.getFieldrefName(argument).equals("ascensionLevel") && constPool.getFieldrefType(argument).equals("I")) {
                    int conditionEnd = i;
                    boolean foundBiPush = false;
                    if (i + 1 < size && isIconstOrBipush(byteCodes.get(i + 1))) {
                        conditionEnd = i + 1;
                        foundBiPush = true;
                    }
                    if (!foundBiPush && i - 1 > 0 && isIconstOrBipush(byteCodes.get(i - 1))) {
                        foundBiPush = true;
                    }
                    if (foundBiPush) {
                        if (conditionEnd + 1 < size) {
                            int conditionCandidateOp = byteCodes.get(conditionEnd + 1);
                            if (conditionCandidateOp == Opcode.IF_ICMPGE || conditionCandidateOp == Opcode.IF_ICMPGT ||
                                    conditionCandidateOp == Opcode.IF_ICMPLE || conditionCandidateOp == Opcode.IF_ICMPLT) {
                                conditionEnd += 1;
                                result.add(byteCodeIndices.get(conditionEnd));
                                continue;
                            }
                        }
                    }

                    debug("TauntMaskPatches.findAscensionLevelConditions: Found GETSTATIC at " + Integer.toHexString(byteCodeIndices.get(i)) + " but can't parse.");
                }
            }
        }

        debug("TauntMaskPatches.findAscensionLevelConditions: result:" + result.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));
        return result;
    }

    private static boolean updateConditionToBoolean(CodeIterator ci, StackMapTable smt, int condition, ConstPool constPool, int[] offset) {
        condition += offset[0];
        int op = ci.byteAt(condition);
        if (op == Opcode.IFEQ) {
            return true;
        } if (op == Opcode.IFNE ||
                op == Opcode.IFGE || op == Opcode.IFGT ||
                op == Opcode.IFLE || op == Opcode.IFLT ||
                op == Opcode.IF_ICMPGE || op == Opcode.IF_ICMPGT ||
                op == Opcode.IF_ICMPLE || op == Opcode.IF_ICMPLT ||
                op == Opcode.IF_ICMPNE || op == Opcode.IF_ICMPEQ) {
            // 0 ifne 00 07
            // 3 iconst_1
            // 4 goto 00 04
            // 7 iconst_0
            // 8 ifeq branch
            Bytecode bytecode = new Bytecode(constPool);
            bytecode.addOpcode(op);
            bytecode.add(0, 7);
            bytecode.addOpcode(Opcode.ICONST_1);
            bytecode.addOpcode(Opcode.GOTO);
            bytecode.add(0, 4);
            bytecode.addOpcode(Opcode.ICONST_0);
            ci.writeByte(Opcode.IFEQ, condition);

            try {
                ci.insert(condition, bytecode.get());
                StackMapTableUpdater smtUpdater = new StackMapTableUpdater(smt.get());
                smtUpdater.addFrame(condition + 7, StackMapTable.Writer::sameFrame);
                smtUpdater.addFrame(condition + 8, (w, od) -> w.sameLocals(od, StackMapTable.INTEGER, 0));
                smt.set(smtUpdater.doIt());
            } catch (BadBytecode e) {
                debug("TauntMaskPatches.updateConditionToBoolean: BadBytecode: " + e.getMessage());
                return false;
            }

            offset[0] += 8;
            return true;
        }

        debug("TauntMaskPatches.updateConditionToBoolean: Unsupported byte code: " + Mnemonic.OPCODE[op]);
        return false;
    }

    private static List<AttackIntentInfo> findAllAttackIndents(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<SetMoveInfo> setMoves = findAllSetMoves(byteCodes, byteCodeIndices, iterator, constPool);
        List<AttackIntentInfo> result = new ArrayList<>();
        for (SetMoveInfo setMove : setMoves) {
            int index;
            if (setMove.type.endsWith("IIZ)V")) {
                index = byteCodeIndices.indexOf(setMove.pos);
                index = previousMatchIntConstOrField(byteCodes, index);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse isMultiDamage: " + setMove);
                    continue;
                }

                index = previousMatchIntConstOrField(byteCodes, index);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse multiplier: " + setMove);
                    continue;
                }

                index = previousMatchIntConstOrFieldOrDamageInfo(byteCodes, index);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse baseDamage: " + setMove);
                    continue;
                }

                if (index - 1 < 0 || byteCodes.get(index - 1) != Opcode.GETSTATIC) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse intent: " + setMove);
                    continue;
                }

                int argument = iterator.s16bitAt(byteCodeIndices.get(index - 1) + 1);
                if (constPool.getFieldrefName(argument).contains("ATTACK")) {
                    result.add(new AttackIntentInfo(setMove.pos, byteCodeIndices.get(index - 1), setMove.type));
                }

            } else if (setMove.type.endsWith("I)V")) {
                index = byteCodeIndices.indexOf(setMove.pos);
                index = previousMatchIntConstOrFieldOrDamageInfo(byteCodes, index);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse baseDamage: " + setMove);
                    continue;
                }

                if (index - 1 < 0 || byteCodes.get(index - 1) != Opcode.GETSTATIC) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse intent: " + setMove);
                    continue;
                }

                int argument = iterator.s16bitAt(byteCodeIndices.get(index - 1) + 1);
                if (constPool.getFieldrefName(argument).contains("ATTACK")) {
                    result.add(new AttackIntentInfo(setMove.pos, byteCodeIndices.get(index - 1), setMove.type));
                }
            }
        }

        debug("TauntMaskPatches.findAllAttackIndents: result:" + result);
        return result;
    }

    private static List<SetMoveInfo> findAllSetMoves(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<SetMoveInfo> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.INVOKEVIRTUAL) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                if (constPool.getMethodrefName(argument).equals("setMove")) {
                    String type = constPool.getMethodrefType(argument);
                    if (type.equals("(Ljava/lang/String;BLcom/megacrit/cardcrawl/monsters/AbstractMonster$Intent;IIZ)V") ||
                            type.equals("(Ljava/lang/String;BLcom/megacrit/cardcrawl/monsters/AbstractMonster$Intent;I)V") ||
                            type.equals("(BLcom/megacrit/cardcrawl/monsters/AbstractMonster$Intent;IIZ)V") ||
                            type.equals("(BLcom/megacrit/cardcrawl/monsters/AbstractMonster$Intent;I)V")) {
                        result.add(new SetMoveInfo(index, type));
                    }
                }
            }
        }

        debug("TauntMaskPatches.findAllSetMove: result:" + result);
        return result;
    }

    private static Bytecode generateAttackIntentInitialize(List<AttackIntentInfo> attackIntents, CodeIterator iterator, ConstPool constPool, List<Integer> sameFrames, List<Integer> sameLocals) {
        Bytecode bytecode = new Bytecode(constPool);
        if (attackIntents.size() == 0) {
            return bytecode;
        }

        bytecode.addAload(0);
        bytecode.addInvokestatic(TauntMaskPatchesClassName, "initSetMove", "(L" + AbstractMonsterClassName.replace('.', '/') + ";)Z");
        int afterInitSetMove = bytecode.length();
        bytecode.addOpcode(Opcode.IFEQ);
        bytecode.add(0, 0); // Update later

        for (AttackIntentInfo attackIntent : attackIntents) {
            generateAttackIntentPredictableCondition(attackIntent.predictableConditions, bytecode, iterator, sameFrames, sameLocals);
            bytecode.addAload(0);
            int startIndex = attackIntent.getIntentFieldPos + 3;
            int endIndex = attackIntent.setMovePos;
            for (int i = startIndex; i < endIndex; i++) {
                bytecode.add(iterator.byteAt(i));
            }
            int lastSemicolon = attackIntent.type.lastIndexOf(';');
            bytecode.addInvokestatic(TauntMaskPatchesClassName,
                    "addSetMoveItem",
                    "(ZL" + AbstractMonsterClassName.replace('.', '/') + attackIntent.type.substring(lastSemicolon));
        }

        int afterSetMoveItems = bytecode.length();
        bytecode.write16bit(afterInitSetMove + 1, afterSetMoveItems - afterInitSetMove);

        return bytecode;
    }

    private static void generateAttackIntentPredictableCondition(List<AttackIntentInfoConditions> predictableConditions, Bytecode bytecode, CodeIterator iterator, List<Integer> sameFrames, List<Integer> sameLocals) {
        List<Integer> successBranchLocations = new ArrayList<>();
        for (AttackIntentInfoConditions predictableCondition : predictableConditions) {
            int size = predictableCondition.starts.size();
            List<Integer> failBranchLocations = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int start = predictableCondition.starts.get(i);
                int end = predictableCondition.ends.get(i);
                boolean isBranch = predictableCondition.isBranch.get(i);
                for (int j = start; j < end; j++) {
                    bytecode.add(iterator.byteAt(j));
                }
                int branchOp = iterator.byteAt(end);
                failBranchLocations.add(bytecode.length());
                bytecode.add(isBranch ? revertBranchOp(branchOp) : branchOp);
                bytecode.add(0, 0); // Update later
            }
            successBranchLocations.add(bytecode.length());
            bytecode.addOpcode(Opcode.GOTO);
            bytecode.add(0, 0);
            int currentPos = bytecode.length();
            sameFrames.add(currentPos);
            for (int failedBranchLocation : failBranchLocations) {
                bytecode.write16bit(failedBranchLocation + 1, currentPos - failedBranchLocation);
            }
        }
        bytecode.addIconst(0);
        bytecode.addOpcode(Opcode.GOTO);
        bytecode.add(0, 4);
        int currentPos = bytecode.length();
        sameFrames.add(currentPos);
        for (int successBranchLocation : successBranchLocations) {
            bytecode.write16bit(successBranchLocation + 1, currentPos - successBranchLocation);
        }
        bytecode.addIconst(1);
        sameLocals.add(bytecode.length());
    }

    private static void insertAttackInitCode(Bytecode attackInitCode, CodeIterator iterator, StackMapTable smt, List<Integer> sameFrames, List<Integer> sameLocals) throws BadBytecode {
        iterator.insert(0, attackInitCode.get());
        StackMapTableUpdater smtUpdater = new StackMapTableUpdater(smt.get());
        smtUpdater.addFrame(attackInitCode.length(), StackMapTable.Writer::sameFrame);
        for (int sameFrame : sameFrames) {
            smtUpdater.addFrame(sameFrame, StackMapTable.Writer::sameFrame);
        }
        for (int sameLocal : sameLocals) {
            smtUpdater.addFrame(sameLocal, (w, od) -> w.sameLocals(od, StackMapTable.INTEGER, 0));
        }
        smt.set(smtUpdater.doIt());
    }

    private static TreeMap<Integer, CodePiece> splitCodeStructure(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator) {
        TreeMap<Integer, CodePiece> codes = new TreeMap<>();
        codes.put(0, new CodePiece(0, iterator.getCodeLength(), null, null));

        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            int op = byteCodes.get(i);
            if ((op >= Opcode.IFEQ && op <= Opcode.GOTO) || op == Opcode.GOTO_W) {
                int targetOffset = op == Opcode.GOTO_W ?
                        iterator.s32bitAt(byteCodeIndices.get(i) + 1) :
                        iterator.s16bitAt(byteCodeIndices.get(i) + 1);
                int target = byteCodeIndices.get(i) + targetOffset;
                int nextOpPos = byteCodeIndices.get(i) + (op == Opcode.GOTO_W ? 5 : 3);
                CodePiece currentPiece = codes.lowerEntry(nextOpPos).getValue();
                if (currentPiece.end > nextOpPos) {
                    CodePiece nextCodePiece = currentPiece.split(nextOpPos);
                    codes.put(nextOpPos, nextCodePiece);
                }

                CodePiece targetPiece = codes.floorEntry(target).getValue();
                if (targetPiece.start != target) {
                    CodePiece nextCodePiece = targetPiece.split(target);
                    codes.put(target, nextCodePiece);
                    targetPiece = nextCodePiece;
                }

                if (op == Opcode.GOTO || op == Opcode.GOTO_W) {
                    currentPiece.setNext(targetPiece);
                } else {
                    currentPiece.setBranches(Collections.singletonList(targetPiece));
                }

            } else if (op == Opcode.RETURN) {
                int nextOpPos = byteCodeIndices.get(i) + 1;
                CodePiece currentPiece = codes.lowerEntry(nextOpPos).getValue();
                if (currentPiece.end > nextOpPos) {
                    CodePiece nextCodePiece = currentPiece.split(nextOpPos);
                    codes.put(nextOpPos, nextCodePiece);
                }
                currentPiece.setNext(null);

            } else if (op == Opcode.TABLESWITCH || op == Opcode.LOOKUPSWITCH) {
                int opStart = byteCodeIndices.get(i);
                int tableStart = (opStart + 1 + 3) / 4 * 4;
                int defaultTarget = iterator.s32bitAt(tableStart) + opStart;
                int count;
                int nextOpPos;
                int loopStep;

                if (op == Opcode.TABLESWITCH) {
                    int low = iterator.s32bitAt(tableStart + 4);
                    int high = iterator.s32bitAt(tableStart + 8);
                    count = high - low + 1;
                    nextOpPos = tableStart + 12 + count * 4;
                    loopStep = 4;
                } else {
                    count = iterator.s32bitAt(tableStart + 4);
                    nextOpPos = tableStart + 8 + count * 8;
                    loopStep = 8;
                }

                CodePiece currentPiece = codes.lowerEntry(nextOpPos).getValue();
                if (currentPiece.end > nextOpPos) {
                    CodePiece nextCodePiece = currentPiece.split(nextOpPos);
                    codes.put(nextOpPos, nextCodePiece);
                }

                CodePiece defaultTargetPiece = codes.floorEntry(defaultTarget).getValue();
                if (defaultTargetPiece.start != defaultTarget) {
                    CodePiece nextCodePiece = defaultTargetPiece.split(defaultTarget);
                    codes.put(defaultTarget, nextCodePiece);
                    defaultTargetPiece = nextCodePiece;
                }

                List<CodePiece> targetPieces = new ArrayList<>();
                for (int j = 0, k = tableStart + 12; j < count; j++, k += loopStep) {
                    int target = iterator.s32bitAt(k) + opStart;

                    CodePiece targetPiece = codes.floorEntry(target).getValue();
                    if (targetPiece.start != target) {
                        CodePiece nextCodePiece = targetPiece.split(target);
                        codes.put(target, nextCodePiece);
                        targetPiece = nextCodePiece;
                    }
                    targetPieces.add(targetPiece);
                }

                currentPiece.setNext(defaultTargetPiece);
                currentPiece.setBranches(targetPieces);
            }
        }

        debug("TauntMaskPatches.splitCodeStructure: result:" +
                codes.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).map(Map.Entry::getValue).collect(Collectors.toList()));

        return codes;
    }

    private static TreeMap<CodePiece, CodePieceExtension> fillPossibleSetMoves(TreeMap<Integer, CodePiece> codes, List<AttackIntentInfo> attackIntents, List<Integer> randomConditions, List<Integer> predictableConditions) {
        TreeMap<CodePiece, CodePieceExtension> result = new TreeMap<>(Comparator.comparingInt(a -> a.start));
        for (Map.Entry<Integer, CodePiece> entry : codes.entrySet()) {
            result.put(entry.getValue(), new CodePieceExtension(entry.getKey()));
        }

        for (int randomCondition : randomConditions) {
            result.get(codes.floorEntry(randomCondition).getValue()).randomCondition = randomCondition;
        }

        for (int predictableCondition : predictableConditions) {
            result.get(codes.floorEntry(predictableCondition).getValue()).predictableCondition = predictableCondition;
        }

        for (int i = 0, attackIntentsSize = attackIntents.size(); i < attackIntentsSize; i++) {
            AttackIntentInfo attackIntent = attackIntents.get(i);
            int codePos = attackIntent.setMovePos;
            CodePiece startCode = codes.floorEntry(codePos).getValue();
            traverseToFillSetMoves(startCode, i, result);
        }

        debug("TauntMaskPatches.fillPossibleSetMoves: result:" +
                result.entrySet().stream().sorted(Comparator.comparingInt(a -> a.getKey().start)).map(Map.Entry::getValue).collect(Collectors.toList()));

        return result;
    }

    private static void traverseToFillSetMoves(CodePiece startCode, int setMoveId, TreeMap<CodePiece, CodePieceExtension> codeExtensions) {
        Stack<CodePiece> codePieceStack = new Stack<>();
        Set<CodePiece> accessedCode = new HashSet<>();
        codePieceStack.push(startCode);
        while (!codePieceStack.empty()) {
            CodePiece code = codePieceStack.pop();
            if (accessedCode.contains(code)) {
                continue;
            }
            accessedCode.add(code);
            for (CodePiece previous : code.previous) {
                CodePieceExtension previousExt = codeExtensions.get(previous);
                if (previous.next == code) {
                    previousExt.nextSetMoveIds.add(setMoveId);
                }
                if (previous.branches != null && previous.branches.contains(code)) {
                    previousExt.branchSetMoveIds.add(setMoveId);
                }
                codePieceStack.push(previous);
            }
        }
    }

    private static void insertModifyRandomResult(int conditionPos, TreeMap<Integer, CodePiece> codes, TreeMap<CodePiece, CodePieceExtension> codeExtensions, CodeIterator iterator, ConstPool constPool, int[] offset) throws BadBytecode {
        CodePiece code = codes.floorEntry(conditionPos).getValue();
        CodePieceExtension codeExt = codeExtensions.get(code);
        Bytecode bytecode = new Bytecode(constPool);
        pushArrayToByteCode(bytecode, codeExt.nextSetMoveIds);
        pushArrayToByteCode(bytecode, codeExt.branchSetMoveIds);
        bytecode.addInvokestatic(TauntMaskPatchesClassName, "modifyRandomResult", "(Z[I[I)Z");
        iterator.insert(conditionPos + offset[0], bytecode.get());
        offset[0] += bytecode.length();
    }

    private static void populateAttackIntentOtherConditions(
            List<AttackIntentInfo> attackIntents,
            List<Integer> byteCodes,
            List<Integer> byteCodeIndices,
            TreeMap<Integer, CodePiece> codes,
            TreeMap<CodePiece, CodePieceExtension> codeExtensions) {

        for (AttackIntentInfo attackIntent : attackIntents) {
            int codePos = attackIntent.setMovePos;
            CodePiece startCode = codes.floorEntry(codePos).getValue();
            traverseToFillAttackIntentOtherConditions(attackIntent, startCode, byteCodes, byteCodeIndices, codes, codeExtensions);
        }

        debug("TauntMaskPatches.populateAttackIntentOtherConditions: result:" +
                attackIntents.stream().map(i -> i.predictableConditions).collect(Collectors.toList()));
    }

    private static void traverseToFillAttackIntentOtherConditions(
            AttackIntentInfo attackIntent,
            CodePiece startCode,
            List<Integer> byteCodes,
            List<Integer> byteCodeIndices,
            TreeMap<Integer, CodePiece> codes,
            TreeMap<CodePiece, CodePieceExtension> codeExtensions) {
        Set<AttackIntentInfoConditions> results = new HashSet<>();
        Set<CodePiece> accessed = new HashSet<>();
        findAttackIntentOtherConditionsRecursively(startCode, new AttackIntentInfoConditions(), codes.get(0), byteCodes, byteCodeIndices, codeExtensions, accessed, results);
        attackIntent.predictableConditions.addAll(results);
    }

    private static void findAttackIntentOtherConditionsRecursively(
            CodePiece currentCode,
            AttackIntentInfoConditions currentConditions,
            CodePiece targetCode,
            List<Integer> byteCodes,
            List<Integer> byteCodeIndices,
            TreeMap<CodePiece, CodePieceExtension> codeExtensions,
            Set<CodePiece> accessed,
            Set<AttackIntentInfoConditions> results) {
        if (currentCode == targetCode) {
            addConditionToResults(currentConditions, results);
            return;
        }

        if (accessed.contains(currentCode) || currentCode.previous.size() == 0) {
            return;
        }

        accessed.add(currentCode);
        for (CodePiece previousCode : currentCode.previous) {
            if (previousCode.branches == null || previousCode.branches.size() == 0) {
                findAttackIntentOtherConditionsRecursively(previousCode, currentConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
                continue;
            }

            CodePieceExtension previousCodeExt = codeExtensions.get(previousCode);
            if (previousCodeExt.predictableCondition != -1) {
                int index = byteCodeIndices.indexOf(previousCodeExt.predictableCondition);
                if (byteCodes.get(index - 1) == Opcode.GETFIELD) {
                    int start = byteCodeIndices.get(index - 2);
                    AttackIntentInfoConditions previousConditions = currentConditions.clone();
                    previousConditions.starts.add(start);
                    previousConditions.ends.add(previousCodeExt.predictableCondition);
                    previousConditions.isBranch.add(previousCode.next != currentCode);
                    findAttackIntentOtherConditionsRecursively(previousCode, previousConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
                    continue;
                } else if (byteCodes.get(index - 1) == Opcode.INVOKEVIRTUAL) {
                    int index2 = previousMatchIntConstOrField(byteCodes, index - 1);
                    if (index2 - 1 >= 0 && byteCodes.get(index2 - 1) == Opcode.ALOAD_0) {
                        AttackIntentInfoConditions previousConditions = currentConditions.clone();
                        previousConditions.starts.add(byteCodeIndices.get(index2 - 1));
                        previousConditions.ends.add(previousCodeExt.predictableCondition);
                        previousConditions.isBranch.add(previousCode.next != currentCode);
                        findAttackIntentOtherConditionsRecursively(previousCode, previousConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
                        continue;
                    }
                } else if (byteCodes.get(index - 1) == Opcode.GETSTATIC ||
                        (index - 2 >= 0 && byteCodes.get(index - 2) == Opcode.GETSTATIC)) {
                    AttackIntentInfoConditions previousConditions = currentConditions.clone();
                    previousConditions.starts.add(byteCodeIndices.get(index - 2));
                    previousConditions.ends.add(previousCodeExt.predictableCondition);
                    previousConditions.isBranch.add(previousCode.next != currentCode);
                    findAttackIntentOtherConditionsRecursively(previousCode, previousConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
                    continue;
                }
            }

            if (previousCodeExt.randomCondition != -1) {
                findAttackIntentOtherConditionsRecursively(previousCode, currentConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
                continue;
            }

            AttackIntentInfoConditions previousConditions = currentConditions.clone();
            previousConditions.hasUnknown = true;
            findAttackIntentOtherConditionsRecursively(previousCode, previousConditions, targetCode, byteCodes, byteCodeIndices, codeExtensions, accessed, results);
        }

        accessed.remove(currentCode);
    }

    private static void addConditionToResults(AttackIntentInfoConditions currentConditions, Set<AttackIntentInfoConditions> results) {
        List<AttackIntentInfoConditions> additionalToBeAdded = new ArrayList<>();
        boolean addCurrent = true;
        for (Iterator<AttackIntentInfoConditions> iterator = results.iterator(); iterator.hasNext(); ) {
            AttackIntentInfoConditions existingCondition = iterator.next();
            if (existingCondition.isSubsetOf(currentConditions)) {
                iterator.remove();
            }
            if (currentConditions.isSubsetOf(existingCondition)) {
                addCurrent = false;
            }
            AttackIntentInfoConditions newCondition = existingCondition.singleDifference(currentConditions);
            if (newCondition != null) {
                iterator.remove();
                additionalToBeAdded.add(newCondition);
            }
        }
        if (addCurrent) {
            results.add(currentConditions);
        }
        for (AttackIntentInfoConditions toBeAdded : additionalToBeAdded) {
            addConditionToResults(toBeAdded, results);
        }
    }

    private static int previousMatchIntConstOrField(List<Integer> byteCodes, int pos) {
        if (pos - 1 >= 0) {
            int op = byteCodes.get(pos - 1);
            if (op == Opcode.ICONST_0 || op == Opcode.ICONST_1 || op == Opcode.ICONST_2 || op == Opcode.ICONST_3 || op == Opcode.ICONST_4 ||
                    op == Opcode.ICONST_5 || op == Opcode.ICONST_M1 || op == Opcode.BIPUSH || op == Opcode.SIPUSH || op == Opcode.LDC || op == Opcode.LDC_W) {
                return pos - 1;
            }
        }

        if (pos - 2 >= 0) {
            if (byteCodes.get(pos - 2) == Opcode.ALOAD_0 && byteCodes.get(pos - 1) == Opcode.GETFIELD) {
                return pos - 2;
            }
        }

        return -1;
    }

    private static int previousMatchIntConstOrFieldOrDamageInfo(List<Integer> byteCodes, int pos) {
        int index = previousMatchIntConstOrField(byteCodes, pos);
        if (index != -1) {
            return index;
        }

        if (pos - 6 >= 0) {
            int indexOp = byteCodes.get(pos - 4);
            if (byteCodes.get(pos - 6) == Opcode.ALOAD_0 &&
                    byteCodes.get(pos - 5) == Opcode.GETFIELD &&
                    isIconstOrBipush(indexOp) &&
                    byteCodes.get(pos - 3) == Opcode.INVOKEVIRTUAL &&
                    byteCodes.get(pos - 2) == Opcode.CHECKCAST &&
                    byteCodes.get(pos - 1) == Opcode.GETFIELD) {
                return pos - 6;
            }
        }

        return -1;
    }

    private static boolean isIconstOrBipush(int indexOp) {
        return (indexOp >= Opcode.ICONST_0 && indexOp <= Opcode.ICONST_5) || indexOp == Opcode.BIPUSH;
    }

    private static void pushArrayToByteCode(Bytecode bytecode, List<Integer> array) {
        int size = array.size();
        bytecode.addNewarray(Bytecode.T_INT, size);
        for (int i = 0; i < size; i++) {
            bytecode.addOpcode(Opcode.DUP);
            bytecode.addIconst(i);
            bytecode.addIconst(array.get(i));
            bytecode.addOpcode(Opcode.IASTORE);
        }
    }

    private static int revertBranchOp(int branchOp) {
        switch (branchOp) {
            case Opcode.IFNE:       return Opcode.IFEQ;
            case Opcode.IFEQ:       return Opcode.IFNE;
            case Opcode.IF_ICMPGE:  return Opcode.IF_ICMPLT;
            case Opcode.IF_ICMPGT:  return Opcode.IF_ICMPLE;
            case Opcode.IF_ICMPLE:  return Opcode.IF_ICMPGT;
            case Opcode.IF_ICMPLT:  return Opcode.IF_ICMPGE;
            case Opcode.IF_ICMPEQ:  return Opcode.IF_ICMPNE;
            default: return Opcode.POP;
        }
    }

    private static void debug(String messge) {
        System.out.println(messge);
    }

    static class StackMapTableUpdater extends StackMapTable.Walker {
        static class NewOffset {
            int offset;
            BiConsumer<StackMapTable.Writer, Integer> operation;
            boolean isReplace;
        }

        private final StackMapTable.Writer writer;
        private final List<NewOffset> newOffsets = new ArrayList<>();
        private int newOffsetIndex;
        private int offset;

        public StackMapTableUpdater(byte[] data) {
            super(data);
            this.writer = new StackMapTable.Writer(data.length);
        }

        public void addFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
            NewOffset newOffset = new NewOffset();
            newOffset.offset = offset;
            newOffset.operation = operation;
            newOffset.isReplace = false;
            newOffsets.add(newOffset);
        }

        public void replaceFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
            NewOffset newOffset = new NewOffset();
            newOffset.offset = offset;
            newOffset.operation = operation;
            newOffset.isReplace = true;
            newOffsets.add(newOffset);
        }

        public byte[] doIt() throws BadBytecode {
            this.newOffsets.sort(Comparator.comparingInt(a -> a.offset));
            this.newOffsetIndex = 0;
            this.offset = -1;
            this.parse();
            return this.writer.toByteArray();
        }

        @Override
        public void sameFrame(int pos, int offsetDelta) {
            offsetDelta = tryInsert(offsetDelta);
            if (offsetDelta >= 0) {
                this.writer.sameFrame(offsetDelta);
            }
        }

        @Override
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            offsetDelta = tryInsert(offsetDelta);
            if (offsetDelta >= 0) {
                this.writer.sameLocals(offsetDelta, stackTag, stackData);
            }
        }

        @Override
        public void chopFrame(int pos, int offsetDelta, int k) {
            offsetDelta = tryInsert(offsetDelta);
            if (offsetDelta >= 0) {
                this.writer.chopFrame(offsetDelta, k);
            }
        }

        @Override
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            offsetDelta = tryInsert(offsetDelta);
            if (offsetDelta >= 0) {
                this.writer.appendFrame(offsetDelta, tags, data);
            }
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData) {
            offsetDelta = tryInsert(offsetDelta);
            if (offsetDelta >= 0) {
                this.writer.fullFrame(offsetDelta, localTags, localData, stackTags, stackData);
            }
        }

        private int tryInsert(int offsetDelta) {
            boolean skipNextFrame = false;
            while (newOffsetIndex < this.newOffsets.size()) {
                NewOffset newOffset = this.newOffsets.get(newOffsetIndex);
                int nextSameFrameOffset = newOffset.offset;
                if (offset + offsetDelta + 1 > nextSameFrameOffset) {
                    int newOffsetDelta = nextSameFrameOffset - offset;

                    if (newOffset.isReplace) {
                        skipNextFrame = true;
                        newOffset.operation.accept(this.writer, offsetDelta);
                    } else {
                        newOffset.operation.accept(this.writer, newOffsetDelta - 1);
                    }

                    offset += newOffsetDelta;
                    offsetDelta -= newOffsetDelta;
                    newOffsetIndex++;
                    if (skipNextFrame) {
                        return -1;
                    } else {
                        continue;
                    }
                }

                break;
            }

            offset += offsetDelta + 1;
            return offsetDelta;
        }
    }

    static class SetMoveInfo {
        public final String type;
        public final int pos;
        public SetMoveInfo(int pos, String type) {
            this.pos = pos;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("SetMoveInfo(%X, %s)", pos, type);
        }
    }

    static class AttackIntentInfo {
        public final String type;
        public final int setMovePos;
        public final int getIntentFieldPos;
        public final List<AttackIntentInfoConditions> predictableConditions = new ArrayList<>();
        public AttackIntentInfo(int setMovePos, int getIntentFieldPos, String type) {
            this.setMovePos = setMovePos;
            this.getIntentFieldPos = getIntentFieldPos;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("AttackIntentInfo(%X, %X, %s)", setMovePos, getIntentFieldPos, type);
        }
    }

    static class AttackIntentInfoConditions {
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<Boolean> isBranch = new ArrayList<>();
        boolean hasUnknown;

        @Override
        public String toString() {
            return "AttackIntentInfoConditions{" +
                    "starts=" + starts.stream().map(Integer::toHexString).collect(Collectors.toList()) +
                    ", ends=" + ends.stream().map(Integer::toHexString).collect(Collectors.toList()) +
                    ", isBranch=" + isBranch +
                    ", hasUnknown=" + hasUnknown +
                    '}';
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public AttackIntentInfoConditions clone() {
            AttackIntentInfoConditions result = new AttackIntentInfoConditions();
            result.starts = new ArrayList<>(starts);
            result.ends = new ArrayList<>(ends);
            result.isBranch = new ArrayList<>(isBranch);
            result.hasUnknown = hasUnknown;
            return result;
        }

        public boolean isSubsetOf(AttackIntentInfoConditions other) {
            int thisSize = this.starts.size();
            int otherSize = other.starts.size();
            if (otherSize >= thisSize) {
                return false;
            }
            for (int i = 0, j = 0; i < thisSize && j < otherSize; i++, j++) {
                int thisStart = this.starts.get(i);
                int otherStart = other.starts.get(j);
                if (otherStart < thisStart) {
                    j--;
                    continue;
                }
                if (otherStart > thisStart) {
                    return false;
                }
                if (this.isBranch.get(i) != other.isBranch.get(j)) {
                    return false;
                }
            }
            return true;
        }

        public AttackIntentInfoConditions singleDifference(AttackIntentInfoConditions other) {
            int thisSize = this.starts.size();
            int otherSize = other.starts.size();
            if (otherSize != thisSize) {
                return null;
            }

            int differenceCount = 0;
            int differenceIndex = 0;
            for (int i = 0; i < thisSize; i++) {
                int thisStart = this.starts.get(i);
                int otherStart = other.starts.get(i);
                if (otherStart != thisStart) {
                    return null;
                }
                if (this.isBranch.get(i) != other.isBranch.get(i)) {
                    differenceCount += 1;
                    differenceIndex = i;
                }
                if (differenceCount > 1) {
                    return null;
                }
            }

            AttackIntentInfoConditions result = this.clone();
            result.starts.remove(differenceIndex);
            result.ends.remove(differenceIndex);
            result.isBranch.remove(differenceIndex);
            result.hasUnknown = this.hasUnknown || other.hasUnknown;
            return result;
        }
    }

    static class CodePiece {
        final int start;
        int end;
        List<CodePiece> branches;
        CodePiece next;
        TreeSet<CodePiece> previous = new TreeSet<>(Comparator.comparingInt(a -> a.start));

        public CodePiece(int start, int end, List<CodePiece> branches, CodePiece next) {
            this.start = start;
            this.end = end;
            this.setBranches(branches);
            this.setNext(next);
        }

        CodePiece split(int pos) {
            CodePiece nextCodePiece = new CodePiece(pos, this.end, this.branches, this.next);
            this.end = pos;
            this.setBranches(null);
            this.setNext(nextCodePiece);
            return nextCodePiece;
        }

        public void setBranches(List<CodePiece> branches) {
            if (this.branches != null) {
                for (CodePiece branch : this.branches) {
                    branch.previous.remove(this);
                }
            }
            this.branches = branches;
            if (this.branches != null) {
                for (CodePiece branch : this.branches) {
                    branch.previous.add(this);
                }
            }
        }

        public void setNext(CodePiece next) {
            if (this.next != null) {
                this.next.previous.remove(this);
            }
            this.next = next;
            if (this.next != null) {
                this.next.previous.add(this);
            }
        }

        @Override
        public String toString() {
            return String.format("CodePiece(%X, %X, %S, %S, %S)",
                    start,
                    end,
                    branches != null ? branches.stream().map(b -> Integer.toHexString(b.start)).collect(Collectors.toList()) : "null",
                    next != null ? Integer.toHexString(next.start) : "null",
                    previous.stream().map(b -> Integer.toHexString(b.start)).collect(Collectors.toList()));
        }
    }

    static class CodePieceExtension {
        private final int start;
        public List<Integer> nextSetMoveIds = new ArrayList<>();
        public List<Integer> branchSetMoveIds = new ArrayList<>();
        public int randomCondition = -1;
        public int predictableCondition = -1;
        public CodePieceExtension(int start) {
            this.start = start;
        }

        @Override
        public String toString() {
            return String.format("CodePieceExtension(%X, %S, %S, %X, %X)",
                    start,
                    branchSetMoveIds.stream().map(Integer::toHexString).collect(Collectors.toList()),
                    nextSetMoveIds.stream().map(Integer::toHexString).collect(Collectors.toList()),
                    randomCondition,
                    predictableCondition);
        }
    }
}
