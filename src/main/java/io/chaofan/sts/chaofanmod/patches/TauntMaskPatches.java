package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.google.gson.Gson;
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
import java.util.function.BiPredicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class TauntMaskPatches {
    private static final String AbstractMonsterClassName = AbstractMonster.class.getName();
    private static final String AbstractDungeonClassName = AbstractDungeon.class.getName();
    private static final String TauntMaskPatchesClassName = TauntMaskPatches.class.getName();

    private static final Gson gson = new Gson();
    private static CodeNodeBundle bundle;
    private static final List<Boolean> predictableConditionValues = new ArrayList<>();
    private static final List<Integer> damages = new ArrayList<>();

    private static final boolean enableDebug = false;

    @SuppressWarnings("unused")
    public static boolean initSetMove(AbstractMonster monster, String bundleString) {
        damages.clear();
        predictableConditionValues.clear();
        bundle = null;

        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return false;
        }

        // Take effect only on turn 1
        if (GameActionManager.turn != 1 || AbstractDungeon.actionManager.turnHasEnded) {
            return false;
        }

        debug("initSetMove " + monster.getClass().getName());

        bundle = gson.fromJson(bundleString, CodeNodeBundle.class);
        damages.clear();
        predictableConditionValues.clear();
        return true;
    }

    @SuppressWarnings("unused")
    public static void addSetMoveItem(AbstractMonster monster, int baseDamage, int multiplier, boolean isMultiDamage) {
        // Special case that can't be handled by code analysis.
        if (monster.getClass().getName().equals("com.megacrit.cardcrawl.monsters.city.Centurion") && multiplier > 1 &&
                AbstractDungeon.getMonsters().monsters.stream().filter(m -> !m.isDying && !m.isEscaping).count() > 1) {
            baseDamage = 0;
        }

        AbstractPower power = monster.getPower(StrengthPower.POWER_ID);
        int damage = baseDamage;
        if (power != null) {
            damage += power.amount;
        }

        debug("addSetMoveItem " + baseDamage + " " + multiplier + " " + (damage * multiplier));
        damages.add(damage * multiplier);
    }

    @SuppressWarnings("unused")
    public static void addSetMoveItem(AbstractMonster monster, int baseDamage) {
        AbstractPower power = monster.getPower(StrengthPower.POWER_ID);
        int damage = baseDamage;
        if (power != null) {
            damage += power.amount;
        }

        debug("addSetMoveItem " + baseDamage + " " + damage);
        damages.add(baseDamage);
    }

    @SuppressWarnings("unused")
    public static void addPredictableConditionValue(boolean value, AbstractMonster monster) {
        debug("addPredictableConditionValue " + value);
        predictableConditionValues.add(value);
    }

    @SuppressWarnings("unused")
    public static boolean /* true == next, false == branch */ modifyRandomResult(boolean originalResult, int randomConditionId) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return originalResult;
        }

        // Take effect only on turn 1
        if (GameActionManager.turn != 1 || AbstractDungeon.actionManager.turnHasEnded) {
            return originalResult;
        }

        if (bundle == null) {
            return originalResult;
        }

        int randomConditionPos = bundle.r.get(randomConditionId);
        CodeNode node = bundle.n.floorEntry(randomConditionPos).getValue();
        Map<CodeNode, Integer> maxDamages = new HashMap<>();

        int maxTrueDamage = findMaxDamage(bundle.n, node.n, maxDamages);
        int maxFalseDamage = node.b != null && node.b.size() > 0 ? findMaxDamage(bundle.n, node.b.get(0), maxDamages) : 0;
        if (maxTrueDamage > maxFalseDamage) {
            return true;
        } else if (maxTrueDamage < maxFalseDamage) {
            return false;
        } else {
            return originalResult;
        }
    }

    private static int findMaxDamage(TreeMap<Integer, CodeNode> codes, int nodePos, Map<CodeNode, Integer> maxDamages) {
        if (nodePos == -1) {
            return -1;
        }

        Map.Entry<Integer, CodeNode> nodeEntry = codes.floorEntry(nodePos);
        if (nodeEntry == null) {
            return -1;
        }

        CodeNode node = nodeEntry.getValue();
        Integer savedDamage = maxDamages.get(node);
        if (savedDamage != null) {
            return savedDamage;
        }

        // Init value to avoid infinite loop
        maxDamages.put(node, -1);

        List<Integer> possibleDamages = new ArrayList<>();
        if (!node.r && node.c != -1) {
            possibleDamages.add(predictableConditionValues.get(node.c) ?
                    findMaxDamage(codes, node.n, maxDamages) :
                    (node.b != null && node.b.size() > 0 ? findMaxDamage(codes, node.b.get(0), maxDamages) : 0));
        } else {
            possibleDamages.add(findMaxDamage(codes, node.n, maxDamages));
            if (node.b != null) {
                for (int branch : node.b) {
                    possibleDamages.add(findMaxDamage(codes, branch, maxDamages));
                }
            }
        }

        if (node.sm != -1 && possibleDamages.stream().allMatch(d -> d == -1)) {
            possibleDamages.add(damages.get(node.sm));
        }

        int maxDamage = possibleDamages.stream().max(Comparator.comparingInt(d -> d)).orElse(-1);
        maxDamages.put(node, maxDamage);
        return maxDamage;
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class MonsterIntentPatch {
        @SpireRawPatch
        public static void Raw(CtBehavior method) throws Exception {
            ClassPool pool = method.getDeclaringClass().getClassPool();
            List<CtClass> monsters = listAllMonsterClasses(pool);

            System.out.println();
            for (CtClass monster : monsters) {
                try {
                    patchGetMove(monster, null);
                } catch (Exception ex) {
                    if (!enableDebug) {
                        System.out.println(" [Fail]");
                    }
                    System.out.println("TauntMaskPatches.MonsterIntentPatch.Raw: Failed to patch monster: " + monster.getName() + ". " + ex);
                }
            }

            debug("Done");
        }
    }

    private static List<CtClass> listAllMonsterClasses(ClassPool pool) throws NotFoundException {
        CtClass abstractMonsterClass = pool.get(AbstractMonsterClassName);

        List<URI> jars = Arrays.stream(Loader.MODINFOS)
                .map(TauntMaskPatches::modInfoToUri)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    private static URI modInfoToUri(ModInfo modInfo) {
        try {
            return modInfo.jarURL.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
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

        System.out.print("TauntMaskPatches.patchGetMove: processing " + monster.getName());
        if (enableDebug) {
            System.out.println();
        }

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

        List<Range> randomConditions = findRandomConditions(byteCodes, byteCodeIndices, ca.iterator(), cp);
        if (randomConditions.isEmpty()) {
            if (!enableDebug) {
                System.out.println(" [Skip]");
            }
            return;
        }

        randomConditions.sort(Comparator.comparingInt(a -> a.start));

        List<AttackIntentInfo> attackIntents = findAllAttackIndents(byteCodes, byteCodeIndices, ca.iterator(), cp);
        List<Range> predictableConditions = findPredictableConditions(byteCodes, byteCodeIndices, ca.iterator(), cp);

        TreeMap<Integer, CodePiece> codeGraph = splitCodeStructure(byteCodes, byteCodeIndices, ca.iterator());
        TreeMap<CodePiece, CodePieceExtension> codeExtensions = fillCodePieceExtension(codeGraph, attackIntents, randomConditions, predictableConditions);

        CodeNodeBundle codeNodeBundle = makeCodeNodes(randomConditions, codeGraph, codeExtensions);

        List<Integer> sameFrames = new ArrayList<>();
        List<Integer> sameLocals = new ArrayList<>();

        int[] offset = { 0 };
        Bytecode attackInitCode = generateAttackIntentInitialize(attackIntents, ca.iterator(), cp, offset);
        Bytecode predictableConditionInitCode = generatePredictableConditionInitialize(predictableConditions, ca.iterator(), cp, sameFrames, sameLocals, offset);

        offset[0] = 0;
        for (Range randomCondition : randomConditions) {
            if (updateConditionToBoolean(ca.iterator(), smt, randomCondition.end, cp, offset)) {
                insertModifyRandomResult(randomCondition.end, codeGraph, codeExtensions, ca.iterator(), cp, offset);
            }
        }

        insertInitCodes(codeNodeBundle, attackInitCode, predictableConditionInitCode, ca.iterator(), smt, sameFrames, sameLocals);
        if (ca.getMaxStack() < 6) {
            ca.setMaxStack(6);
        }

        if (!enableDebug) {
            System.out.println(" [Done]");
        }

        debug("TauntMaskPatches.patchGetMove: done processing " + monster.getName() +
                ". length gain: " + originalCodeLength + " -> " + ca.length() + ".");

        if (showDecompile) {
            smt.println(System.out);
            printCode(ca);
        }
    }

    private static void printCode(CodeAttribute ca) throws BadBytecode {
        int lastIndex = 0;
        int lastOp = -1;
        CodeIterator ci2 = ca.iterator();
        ConstPool cp = ca.getConstPool();
        while (ci2.hasNext()) {
            int index = ci2.next();
            int op = ci2.byteAt(index);
            if (lastOp == Opcode.INVOKESTATIC || lastOp == Opcode.INVOKEVIRTUAL || lastOp == Opcode.INVOKESPECIAL || lastOp == Opcode.INVOKEINTERFACE) {
                int argument = ci2.u16bitAt(lastIndex);
                System.out.printf(" %s.%s %s", cp.getMethodrefClassName(argument), cp.getMethodrefName(argument), cp.getMethodrefType(argument));
            } else if (lastOp == Opcode.GETSTATIC || lastOp == Opcode.GETFIELD) {
                int argument = ci2.u16bitAt(lastIndex);
                System.out.printf(" %s.%s", cp.getFieldrefClassName(argument), cp.getFieldrefName(argument));
            } else {
                for (int i = lastIndex; i < index; i++) {
                    System.out.printf(" %02X", ci2.byteAt(i));
                }
            }
            System.out.println();
            System.out.printf("%04X: %20s", index, Mnemonic.OPCODE[op]);
            lastIndex = index + 1;
            lastOp = op;
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

    private static List<Range> findRandomConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> results = new ArrayList<>();
        results.addAll(findArgumentConditions(byteCodes, byteCodeIndices, iterator));
        results.addAll(findAiRandomConditions(byteCodes, byteCodeIndices, iterator, constPool));
        return results;
    }

    private static List<Range> findArgumentConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator) {
        List<Range> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            // Load first method argument
            if (byteCodes.get(i) == Opcode.ILOAD_1 ||
                    (byteCodes.get(i) == Opcode.ILOAD && iterator.byteAt(byteCodeIndices.get(i) + 1) == 1)) {
                int conditionStart = i;
                int conditionEnd = i;
                boolean foundBiPush = false;
                if (i + 1 < size && isIconstOrBipush(byteCodes.get(i + 1))) {
                    conditionEnd = i + 1;
                    foundBiPush = true;
                }
                if (!foundBiPush && i - 1 > 0 && isIconstOrBipush(byteCodes.get(i - 1))) {
                    conditionStart = i - 1;
                    foundBiPush = true;
                }
                if (foundBiPush) {
                    if (conditionEnd + 1 < size) {
                        int conditionCandidateOp = byteCodes.get(conditionEnd + 1);
                        if (conditionCandidateOp == Opcode.IF_ICMPGE || conditionCandidateOp == Opcode.IF_ICMPGT ||
                                conditionCandidateOp == Opcode.IF_ICMPLE || conditionCandidateOp == Opcode.IF_ICMPLT) {
                            conditionEnd += 1;
                            result.add(new Range(byteCodeIndices.get(conditionStart), byteCodeIndices.get(conditionEnd)));
                            continue;
                        }
                    }
                }
                debug("TauntMaskPatches.findArgumentConditions: Found ILOAD_1 at " + Integer.toHexString(byteCodeIndices.get(i)) + " but can't parse.");
            }
        }

        debug("TauntMaskPatches.findArgumentConditions: result:" + result);
        return result;
    }

    private static List<Range> findAiRandomConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> result = new ArrayList<>();
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
                                        result.add(new Range(byteCodeIndices.get(i), byteCodeIndices.get(k)));
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

        debug("TauntMaskPatches.findAiRandomConditions: result:" + result);
        return result;
    }

    private static List<Range> findPredictableConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> results = new ArrayList<>();
        results.addAll(findLastMoveConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.addAll(findSingleFieldConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.addAll(findAscensionLevelConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.addAll(findHasPowerConditions(byteCodes, byteCodeIndices, iterator, constPool));
        results.sort(Comparator.comparingInt(a -> a.start));
        return results;
    }

    private static List<Range> findLastMoveConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.INVOKEVIRTUAL) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                String methodName = constPool.getMethodrefName(argument);
                if (methodName.equals("lastMove") || methodName.equals("lastMoveBefore") || methodName.equals("lastTwoMoves")) {
                    int pos2 = previousMatchIntConstOrField(byteCodes, i);
                    if (pos2 > 0 && byteCodes.get(pos2 - 1) == Opcode.ALOAD_0) {
                        int nextOp = byteCodes.get(i + 1);
                        if (nextOp == Opcode.IFNE || nextOp == Opcode.IFEQ) {
                            result.add(new Range(byteCodeIndices.get(pos2 - 1), byteCodeIndices.get(i + 1)));
                        }
                    }
                }
            }
        }

        debug("TauntMaskPatches.findLastMoveConditions: result:" + result);
        return result;
    }

    private static List<Range> findSingleFieldConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            if (byteCodes.get(i) == Opcode.GETFIELD && i > 0 && byteCodes.get(i - 1) == Opcode.ALOAD_0) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                String fieldType = constPool.getFieldrefType(argument);
                if (fieldType.equals("Z")) {
                    int nextOp = byteCodes.get(i + 1);
                    if (nextOp == Opcode.IFNE || nextOp == Opcode.IFEQ) {
                        result.add(new Range(byteCodeIndices.get(i - 1), byteCodeIndices.get(i + 1)));
                    }
                }
            }
        }

        debug("TauntMaskPatches.findSingleFieldConditions: result:" + result);
        return result;
    }

    private static List<Range> findAscensionLevelConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            // Load first method argument
            if (byteCodes.get(i) == Opcode.GETSTATIC) {
                int argument = iterator.s16bitAt(byteCodeIndices.get(i) + 1);
                if (constPool.getFieldrefName(argument).equals("ascensionLevel") && constPool.getFieldrefType(argument).equals("I")) {
                    int conditionStart = i;
                    int conditionEnd = i;
                    boolean foundBiPush = false;
                    if (i + 1 < size && isIconstOrBipush(byteCodes.get(i + 1))) {
                        conditionEnd = i + 1;
                        foundBiPush = true;
                    }
                    if (!foundBiPush && i - 1 >= 0 && isIconstOrBipush(byteCodes.get(i - 1))) {
                        conditionStart = i - 1;
                        foundBiPush = true;
                    }
                    if (foundBiPush) {
                        if (conditionEnd + 1 < size) {
                            int conditionCandidateOp = byteCodes.get(conditionEnd + 1);
                            if (conditionCandidateOp == Opcode.IF_ICMPGE || conditionCandidateOp == Opcode.IF_ICMPGT ||
                                    conditionCandidateOp == Opcode.IF_ICMPLE || conditionCandidateOp == Opcode.IF_ICMPLT) {
                                conditionEnd += 1;
                                result.add(new Range(byteCodeIndices.get(conditionStart), byteCodeIndices.get(conditionEnd)));
                                continue;
                            }
                        }
                    }

                    debug("TauntMaskPatches.findAscensionLevelConditions: Found GETSTATIC at " + Integer.toHexString(byteCodeIndices.get(i)) + " but can't parse.");
                }
            }
        }

        debug("TauntMaskPatches.findAscensionLevelConditions: result:" + result);
        return result;
    }

    private static List<Range> findHasPowerConditions(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<Range> result = new ArrayList<>();
        int size = byteCodes.size();
        for (int i = 0; i < size; i++) {
            // Load first method argument
            if (byteCodes.get(i) == Opcode.INVOKEVIRTUAL) {
                int index = byteCodeIndices.get(i);
                int argument = iterator.s16bitAt(index + 1);
                String methodName = constPool.getMethodrefName(argument);
                if (methodName.equals("hasPower") && i >= 2 &&
                    byteCodes.get(i - 1) == Opcode.LDC &&
                    byteCodes.get(i - 2) == Opcode.GETSTATIC) {
                    int argument2 = iterator.s16bitAt(byteCodeIndices.get(i - 2) + 1);
                    if (constPool.getFieldrefName(argument2).equals("player") && constPool.getFieldrefClassName(argument2).equals(AbstractDungeonClassName)) {
                        int nextOp = byteCodes.get(i + 1);
                        if (nextOp == Opcode.IFNE || nextOp == Opcode.IFEQ) {
                            result.add(new Range(byteCodeIndices.get(i - 2), byteCodeIndices.get(i + 1)));
                        }
                    }
                }
            }
        }

        debug("TauntMaskPatches.findHasPowerConditions: result:" + result);
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

    private static Bytecode generateAttackIntentInitialize(List<AttackIntentInfo> attackIntents, CodeIterator iterator, ConstPool constPool, int[] offset) {
        Bytecode bytecode = new Bytecode(constPool);
        for (AttackIntentInfo attackIntent : attackIntents) {
            bytecode.addAload(0);
            int startIndex = attackIntent.getIntentFieldPos + 3;
            int endIndex = attackIntent.setMovePos;
            for (int i = startIndex; i < endIndex; i++) {
                bytecode.add(iterator.byteAt(i));
            }
            int lastSemicolon = attackIntent.type.lastIndexOf(';');
            bytecode.addInvokestatic(TauntMaskPatchesClassName,
                    "addSetMoveItem",
                    "(L" + AbstractMonsterClassName.replace('.', '/') + attackIntent.type.substring(lastSemicolon));
        }

        offset[0] += bytecode.length();
        return bytecode;
    }

    private static Bytecode generatePredictableConditionInitialize(List<Range> predictableConditions, CodeIterator iterator, ConstPool constPool, List<Integer> sameFrames, List<Integer> sameLocals, int[] offset) {
        Bytecode bytecode = new Bytecode(constPool);
        for (Range predictableCondition : predictableConditions) {
            for (int i = predictableCondition.start; i <= predictableCondition.end; i++) {
                bytecode.add(iterator.byteAt(i));
            }
            bytecode.add(0, 7);
            bytecode.addOpcode(Opcode.ICONST_1);
            bytecode.addOpcode(Opcode.GOTO);
            bytecode.add(0, 4);
            sameFrames.add(offset[0] + bytecode.length());
            bytecode.addOpcode(Opcode.ICONST_0);
            sameLocals.add(offset[0] + bytecode.length());
            bytecode.addAload(0);
            bytecode.addInvokestatic(TauntMaskPatchesClassName,
                    "addPredictableConditionValue",
                    "(ZL" + AbstractMonsterClassName.replace('.', '/') + ";)V");
        }

        offset[0] += bytecode.length();
        return bytecode;
    }

    private static void insertInitCodes(CodeNodeBundle codeNodeBundle, Bytecode attackInitCode, Bytecode predictableConditionInitCode, CodeIterator iterator, StackMapTable smt, List<Integer> sameFrames, List<Integer> sameLocals) throws BadBytecode {
        Bytecode initSetMove = new Bytecode(iterator.get().getConstPool());

        initSetMove.addAload(0);
        String bundleString = gson.toJson(codeNodeBundle);
        debug("insertInitCodes: bundleString.length = " + bundleString.length());
        initSetMove.addLdc(bundleString);
        initSetMove.addInvokestatic(TauntMaskPatchesClassName,
                "initSetMove",
                "(L" + AbstractMonsterClassName.replace('.', '/') + ";Ljava/lang/String;)Z");
        initSetMove.addOpcode(Opcode.IFEQ);
        int firstBranch = initSetMove.length();
        initSetMove.add(0, 0); // Update later
        iterator.insert(0, initSetMove.get());

        int offset = initSetMove.length();
        int length = offset;
        iterator.insert(length, attackInitCode.get());
        length += attackInitCode.length();
        iterator.insert(length, predictableConditionInitCode.get());
        length += predictableConditionInitCode.length();

        iterator.write16bit(length - offset + 3, firstBranch); // Update first branch

        StackMapTableUpdater smtUpdater = new StackMapTableUpdater(smt.get());
        smtUpdater.addFrame(length, StackMapTable.Writer::sameFrame);
        for (int sameFrame : sameFrames) {
            smtUpdater.addFrame(offset + sameFrame, StackMapTable.Writer::sameFrame);
        }
        for (int sameLocal : sameLocals) {
            smtUpdater.addFrame(offset + sameLocal, (w, od) -> w.sameLocals(od, StackMapTable.INTEGER, 0));
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

    private static TreeMap<CodePiece, CodePieceExtension> fillCodePieceExtension(TreeMap<Integer, CodePiece> codes, List<AttackIntentInfo> attackIntents, List<Range> randomConditions, List<Range> predictableConditions) {
        TreeMap<CodePiece, CodePieceExtension> result = new TreeMap<>(Comparator.comparingInt(a -> a.start));
        for (Map.Entry<Integer, CodePiece> entry : codes.entrySet()) {
            result.put(entry.getValue(), new CodePieceExtension(entry.getKey()));
        }

        for (int i = 0, randomConditionsSize = randomConditions.size(); i < randomConditionsSize; i++) {
            int randomCondition = randomConditions.get(i).end;
            result.get(codes.floorEntry(randomCondition).getValue()).randomCondition = randomCondition;
            result.get(codes.floorEntry(randomCondition).getValue()).randomConditionId = i;
        }

        for (int i = 0, predictableConditionsSize = predictableConditions.size(); i < predictableConditionsSize; i++) {
            int predictableCondition = predictableConditions.get(i).end;
            result.get(codes.floorEntry(predictableCondition).getValue()).predictableCondition = predictableCondition;
            result.get(codes.floorEntry(predictableCondition).getValue()).predictableConditionId = i;
        }

        for (int i = 0, attackIntentsSize = attackIntents.size(); i < attackIntentsSize; i++) {
            AttackIntentInfo attackIntentInfo = attackIntents.get(i);
            result.get(codes.floorEntry(attackIntentInfo.setMovePos).getValue()).setMoveId = i;
        }

        debug("TauntMaskPatches.fillPossibleSetMoves: result:" +
                result.entrySet().stream().sorted(Comparator.comparingInt(a -> a.getKey().start)).map(Map.Entry::getValue).collect(Collectors.toList()));

        return result;
    }

    private static CodeNodeBundle makeCodeNodes(List<Range> randomConditions, TreeMap<Integer, CodePiece> codes, TreeMap<CodePiece, CodePieceExtension> codeExtensions) {
        TreeMap<Integer, CodeNode> codeNodes = new TreeMap<>();
        for (Map.Entry<Integer, CodePiece> entry : codes.entrySet()) {
            toNode(entry.getValue(), codeExtensions, codeNodes);
        }

        CodeNodeBundle bundle = new CodeNodeBundle();
        bundle.n = codeNodes;
        bundle.r = randomConditions.stream().map(rc -> rc.end).collect(Collectors.toList());

        debug("TauntMaskPatches.makeCodeNodes: result:" + bundle);

        return bundle;
    }

    private static void toNode(CodePiece code, TreeMap<CodePiece, CodePieceExtension> codeExtensions, TreeMap<Integer, CodeNode> codeNodes) {
        CodePieceExtension codeExtension = codeExtensions.get(code);
        CodeNode node = new CodeNode();
        codeNodes.put(code.start, node);

        node.s = code.start;
        node.r = codeExtension.randomConditionId != -1;
        node.c = node.r ? codeExtension.randomConditionId : codeExtension.predictableConditionId;
        node.sm = codeExtension.setMoveId;
        node.n = code.next != null ? code.next.start : -1;
        node.b = code.branches != null ? code.branches.stream().map(n -> n.start).collect(Collectors.toList()) : null;
    }

    private static void insertModifyRandomResult(int conditionPos, TreeMap<Integer, CodePiece> codes, TreeMap<CodePiece, CodePieceExtension> codeExtensions, CodeIterator iterator, ConstPool constPool, int[] offset) throws BadBytecode {
        CodePiece code = codes.floorEntry(conditionPos).getValue();
        CodePieceExtension codeExt = codeExtensions.get(code);
        Bytecode bytecode = new Bytecode(constPool);
        bytecode.addIconst(codeExt.randomConditionId);
        bytecode.addInvokestatic(TauntMaskPatchesClassName, "modifyRandomResult", "(ZI)Z");
        iterator.insert(conditionPos + offset[0], bytecode.get());
        offset[0] += bytecode.length();
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

    private static void debug(String message) {
        if (enableDebug) {
            System.out.println(message);
        }
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
        public int randomCondition = -1;
        public int predictableCondition = -1;
        public int randomConditionId = -1;
        public int predictableConditionId = -1;
        public int setMoveId = -1;

        public CodePieceExtension(int start) {
            this.start = start;
        }

        @Override
        public String toString() {
            return String.format("CodePieceExtension(%X, %X, %X, %d, %d, %d)",
                    start,
                    randomCondition,
                    predictableCondition,
                    randomConditionId,
                    predictableConditionId,
                    setMoveId);
        }
    }

    static class CodeNode {
        int s; // start
        List<Integer> b; // branches
        int n; // next
        boolean r; // isRandom
        int c; // conditionId
        int sm; // setMoveId

        @Override
        public String toString() {
            return String.format("CodeNode(%X, %S, %S, %s, %d, %d)",
                    s,
                    b != null ? b.stream().map(Integer::toHexString).collect(Collectors.toList()) : "null",
                    n != -1 ? Integer.toHexString(n) : "null",
                    r,
                    c,
                    sm);
        }
    }

    static class CodeNodeBundle {
        TreeMap<Integer, CodeNode> n;
        List<Integer> r;

        @Override
        public String toString() {
            return "CodeNodeBundle{" +
                    "nodes=" + n +
                    ", randomConditions=" + r +
                    '}';
        }
    }

    static class Range {
        private final int start;
        private final int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return String.format("Range(%X, %X)", start, end);
        }
    }

    static class MatchCode {
        private final int op;
        private final BiPredicate<CodeIterator, Integer> condition;
        private final MatchCode[] matches;
        private final MatchType matchType;

        public MatchCode(int op) {
            this(op, null);
        }

        public MatchCode(int op, BiPredicate<CodeIterator, Integer> condition) {
            this.op = op;
            this.condition = condition;
            this.matches = null;
            this.matchType = null;
        }

        public MatchCode(MatchCode[] matches, MatchType matchType) {
            this.matchType = matchType;
            this.op = -1;
            this.condition = null;
            this.matches = matches;
        }

        public int test(CodeIterator ci, int location) {
            if (matches != null) {
                if (matchType == MatchType.ANY) {
                    return Arrays.stream(matches).map(m -> m.test(ci, location)).filter(r -> r >= 0).findAny().orElse(-1);
                } else if (matchType == MatchType.SEQUENCE) {
                    int localLocation = location;
                    for (MatchCode match : matches) {
                        localLocation = match.test(ci, localLocation);
                        if (localLocation == -1) {
                            return -1;
                        }
                    }
                    return localLocation;
                } else {
                    return -1;
                }
            }

            int ciOldPos = ci.lookAhead();
            ci.move(location);
            if (!ci.hasNext()) {
                ci.move(ciOldPos);
                return -1;
            }

            if (ci.byteAt(location) != op) {
                ci.move(ciOldPos);
                return -1;
            }

            if (condition != null && !condition.test(ci, location)) {
                ci.move(ciOldPos);
                return -1;
            }

            try {
                ci.next();
            } catch (BadBytecode e) {
                ci.move(ciOldPos);
                return -1;
            }

            int result = ci.lookAhead();
            ci.move(ciOldPos);
            return result;
        }

        enum MatchType {
            SEQUENCE,
            ANY;
        }
    }
}
