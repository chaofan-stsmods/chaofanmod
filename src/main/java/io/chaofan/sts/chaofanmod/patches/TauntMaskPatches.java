package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.relics.TauntMask;
import io.chaofan.sts.chaofanmod.utils.CodePattern;
import io.chaofan.sts.chaofanmod.utils.CodeSplitter;
import io.chaofan.sts.chaofanmod.utils.StackMapTableUpdater;
import javassist.*;
import javassist.bytecode.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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
            SpireConfig config = ChaofanMod.tryCreateConfig();
            if (config != null && config.has(ChaofanMod.DISABLE_TAUNT_MASK) && config.getBool(ChaofanMod.DISABLE_TAUNT_MASK)) {
                return;
            }

            ClassPool pool = method.getDeclaringClass().getClassPool();
            List<CtClass> monsters = listAllMonsterClasses(pool);

            System.out.println();
            for (CtClass monster : monsters) {
                try {
                    patchGetMove(monster, "SpiritFireMonster");
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

        CodeSplitter.getCodes(ci, byteCodes, byteCodeIndices);

        if (showDecompile) {
            smt.println(System.out);
            printCode(ca);
        }

        List<CodePattern.Range> randomConditions = findRandomConditions(ca.iterator(), cp);
        if (randomConditions.isEmpty()) {
            if (!enableDebug) {
                System.out.println(" [Skip]");
            }
            return;
        }

        List<AttackIntentInfo> attackIntents = findAllAttackIndents(byteCodes, byteCodeIndices, ca.iterator(), cp);
        List<CodePattern.Range> predictableConditions = findPredictableConditions(ca.iterator(), cp);

        TreeMap<Integer, CodeSplitter.CodePiece> codeGraph = CodeSplitter.split(byteCodes, byteCodeIndices, ca.iterator());

        TauntMaskPatches.debug("TauntMaskPatches.splitCodeStructure: result:" +
                codeGraph.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).map(Map.Entry::getValue).collect(Collectors.toList()));

        TreeMap<CodeSplitter.CodePiece, CodePieceExtension> codeExtensions = fillCodePieceExtension(codeGraph, attackIntents, randomConditions, predictableConditions);

        CodeNodeBundle codeNodeBundle = makeCodeNodes(randomConditions, codeGraph, codeExtensions);

        List<Integer> sameFrames = new ArrayList<>();
        List<Integer> sameLocals = new ArrayList<>();

        int[] offset = { 0 };
        Bytecode attackInitCode = generateAttackIntentInitialize(attackIntents, ca.iterator(), cp, offset);
        Bytecode predictableConditionInitCode = generatePredictableConditionInitialize(predictableConditions, ca.iterator(), cp, sameFrames, sameLocals, offset);

        offset[0] = 0;
        for (CodePattern.Range randomCondition : randomConditions) {
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

        /*StackMapTableUpdater smtUpdater = new StackMapTableUpdater(smt.get());
        smtUpdater.replaceFrame(265, (w,i) -> w.appendFrame(i, new int[]{StackMapTable.INTEGER}, new int[]{0}));
        smtUpdater.replaceFrame(273, StackMapTable.Writer::sameFrame);
        smt.set(smtUpdater.doIt());
*/

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

    private static List<CodePattern.Range> findRandomConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> results = new ArrayList<>();
        results.addAll(findArgumentConditions(iterator));
        results.addAll(findAiRandomConditions(iterator, constPool));
        results.sort(Comparator.comparingInt(a -> a.start));
        return results;
    }

    private static List<CodePattern.Range> findArgumentConditions(CodeIterator iterator) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
                CodePattern.sequence(
                        CodePattern.unorderedPair(
                                new CodePattern(Opcode.ILOAD_1),
                                new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l)))
                        ),
                        new CodePattern(new int[]{ Opcode.IF_ICMPGE, Opcode.IF_ICMPGT, Opcode.IF_ICMPLE, Opcode.IF_ICMPLT })
                ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
        debug("TauntMaskPatches.findArgumentConditions: result:" + result);
        return result;
    }

    private static List<CodePattern.Range> findAiRandomConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
            CodePattern.sequence(
                    new CodePattern(Opcode.GETSTATIC, (i, l) -> {
                        int argument = i.s16bitAt(l + 1);
                        return constPool.getFieldrefName(argument).equals("aiRng") &&
                                constPool.getFieldrefClassName(argument).equals(AbstractDungeonClassName);
                    }),
                    CodePattern.prefixAnyCodes(new CodePattern(Opcode.INVOKEVIRTUAL, (i, l) -> constPool.getMethodrefName(i.s16bitAt(l + 1)).contains("random")), 2),
                    CodePattern.anyOf(
                            CodePattern.sequence(
                                    new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l))),
                                    new CodePattern(new int[]{ Opcode.IF_ICMPGE, Opcode.IF_ICMPGT, Opcode.IF_ICMPLE, Opcode.IF_ICMPLT, Opcode.IF_ICMPEQ, Opcode.IF_ICMPNE })
                            ),
                            new CodePattern(new int[] { Opcode.IFNE, Opcode.IFEQ })
                    )
            ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
        debug("TauntMaskPatches.findAiRandomConditions: result:" + result);
        return result;
    }

    private static List<CodePattern.Range> findPredictableConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> results = new ArrayList<>();
        results.addAll(findLastMoveConditions(iterator, constPool));
        results.addAll(findSingleFieldConditions(iterator, constPool));
        results.addAll(findAscensionLevelConditions(iterator, constPool));
        results.addAll(findHasPowerConditions(iterator, constPool));
        results.sort(Comparator.comparingInt(a -> a.start));
        return results;
    }

    private static List<CodePattern.Range> findLastMoveConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
                CodePattern.sequence(
                        new CodePattern(Opcode.ALOAD_0),
                        CodePattern.anyOf(
                                CodePattern.sequence(Opcode.ALOAD_0, Opcode.GETFIELD),
                                new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l)))
                        ),
                        new CodePattern(Opcode.INVOKEVIRTUAL, (i, l) -> {
                            String methodName = constPool.getMethodrefName(i.s16bitAt(l + 1));
                            return methodName.equals("lastMove") || methodName.equals("lastMoveBefore") || methodName.equals("lastTwoMoves");
                        }),
                        new CodePattern(new int[] { Opcode.IFNE, Opcode.IFEQ })
                ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
        debug("TauntMaskPatches.findLastMoveConditions: result:" + result);
        return result;
    }

    private static List<CodePattern.Range> findSingleFieldConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
                CodePattern.sequence(
                        new CodePattern(Opcode.ALOAD_0),
                        new CodePattern(Opcode.GETFIELD, (i, l) -> constPool.getFieldrefType(i.s16bitAt(l + 1)).equals("Z")),
                        new CodePattern(new int[] { Opcode.IFNE, Opcode.IFEQ })
                ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
        debug("TauntMaskPatches.findSingleFieldConditions: result:" + result);
        return result;
    }

    private static List<CodePattern.Range> findAscensionLevelConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
                CodePattern.sequence(
                        CodePattern.unorderedPair(
                            new CodePattern(Opcode.GETSTATIC, (i, l) -> {
                                int argument = i.s16bitAt(l + 1);
                                return constPool.getFieldrefName(argument).equals("ascensionLevel") && constPool.getFieldrefType(argument).equals("I");
                            }),
                            new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l)))
                        ),
                        new CodePattern(new int[]{ Opcode.IF_ICMPGE, Opcode.IF_ICMPGT, Opcode.IF_ICMPLE, Opcode.IF_ICMPLT })
                ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
        debug("TauntMaskPatches.findAscensionLevelConditions: result:" + result);
        return result;
    }

    private static List<CodePattern.Range> findHasPowerConditions(CodeIterator iterator, ConstPool constPool) throws BadBytecode {
        List<CodePattern.Range> result = CodePattern.find(iterator,
                CodePattern.sequence(
                        new CodePattern(Opcode.GETSTATIC, (i, l) -> {
                            int argument = i.s16bitAt(l + 1);
                            return constPool.getFieldrefName(argument).equals("player") && constPool.getFieldrefClassName(argument).equals(AbstractDungeonClassName);
                        }),
                        new CodePattern(Opcode.LDC),
                        new CodePattern(Opcode.INVOKEVIRTUAL, (i, l) -> {
                            String methodName = constPool.getMethodrefName(i.s16bitAt(l + 1));
                            return methodName.equals("hasPower");
                        }),
                        new CodePattern(new int[]{ Opcode.IFNE, Opcode.IFEQ })
                ));

        result.replaceAll(r -> new CodePattern.Range(r.start, r.end - 3));
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
            int index = byteCodeIndices.indexOf(setMove.pos);
            if (setMove.type.endsWith("IIZ)V")) {
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

    private static Bytecode generatePredictableConditionInitialize(List<CodePattern.Range> predictableConditions, CodeIterator iterator, ConstPool constPool, List<Integer> sameFrames, List<Integer> sameLocals, int[] offset) {
        Bytecode bytecode = new Bytecode(constPool);
        for (CodePattern.Range predictableCondition : predictableConditions) {
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

    private static TreeMap<CodeSplitter.CodePiece, CodePieceExtension> fillCodePieceExtension(TreeMap<Integer, CodeSplitter.CodePiece> codes, List<AttackIntentInfo> attackIntents, List<CodePattern.Range> randomConditions, List<CodePattern.Range> predictableConditions) {
        TreeMap<CodeSplitter.CodePiece, CodePieceExtension> result = new TreeMap<>(Comparator.comparingInt(a -> a.start));
        for (Map.Entry<Integer, CodeSplitter.CodePiece> entry : codes.entrySet()) {
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

    private static CodeNodeBundle makeCodeNodes(List<CodePattern.Range> randomConditions, TreeMap<Integer, CodeSplitter.CodePiece> codes, TreeMap<CodeSplitter.CodePiece, CodePieceExtension> codeExtensions) {
        TreeMap<Integer, CodeNode> codeNodes = new TreeMap<>();
        for (Map.Entry<Integer, CodeSplitter.CodePiece> entry : codes.entrySet()) {
            toNode(entry.getValue(), codeExtensions, codeNodes);
        }

        CodeNodeBundle bundle = new CodeNodeBundle();
        bundle.n = codeNodes;
        bundle.r = randomConditions.stream().map(rc -> rc.end).collect(Collectors.toList());

        debug("TauntMaskPatches.makeCodeNodes: result:" + bundle);

        return bundle;
    }

    private static void toNode(CodeSplitter.CodePiece code, TreeMap<CodeSplitter.CodePiece, CodePieceExtension> codeExtensions, TreeMap<Integer, CodeNode> codeNodes) {
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

    private static void insertModifyRandomResult(int conditionPos, TreeMap<Integer, CodeSplitter.CodePiece> codes, TreeMap<CodeSplitter.CodePiece, CodePieceExtension> codeExtensions, CodeIterator iterator, ConstPool constPool, int[] offset) throws BadBytecode {
        CodeSplitter.CodePiece code = codes.floorEntry(conditionPos).getValue();
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
}
