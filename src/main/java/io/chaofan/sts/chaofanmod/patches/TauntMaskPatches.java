package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
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

    private static final List<Integer> damages = new ArrayList<>();

    @SuppressWarnings("unused")
    public static boolean initSetMove(AbstractMonster monster) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return false;
        }

        debug("initSetMove");
        damages.clear();
        return true;
    }

    @SuppressWarnings("unused")
    public static void addSetMoveItem(AbstractMonster monster, int baseDamage, int multiplier, boolean isMultiDamage) {
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
    public static boolean modifyRandomResult(boolean originalResult, int[] trueDamages /* IFEQ next */, int[] falseDamages /* IFEQ branch */) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TauntMask.ID)) {
            return originalResult;
        }

        int maxTrueDamage = Arrays.stream(trueDamages).map(damages::get).max().orElse(0);
        int maxFalseDamage = Arrays.stream(falseDamages).map(damages::get).max().orElse(0);
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
                    patchGetMove(monster);
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

    private static void patchGetMove(CtClass monster) throws Exception {
        String targetMonster = null; //"JawWorm";
        boolean showDecompile = targetMonster != null;

        if (showDecompile && !monster.getName().endsWith(targetMonster)) {
            return;
        }

        if ((monster.getModifiers() & Modifier.ABSTRACT) > 0) {
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

        getCodes(ci, byteCodes, byteCodeIndices);

        List<Integer> conditions = findRandomConditions(byteCodes, byteCodeIndices, ca.iterator(), cp);
        int[] offset = new int[] { 0 };
        List<Integer> updatedConditions = conditions.stream()
                .sorted()
                .map(c -> updateConditionToBoolean(ci, smt, c + offset[0], cp, offset))
                .filter(c -> c > 0)
                .collect(Collectors.toList());
        debug("TauntMaskPatches.patchGetMove: updatedConditions:" + updatedConditions.stream().map(i -> String.format("%X", i)).collect(Collectors.toList()));

        if (updatedConditions.isEmpty()) {
            return;
        }

        getCodes(ca.iterator(), byteCodes, byteCodeIndices);
        List<AttackIntentInfo> attackIntents = findAllAttackIndents(byteCodes, byteCodeIndices, ca.iterator(), cp);
        Bytecode attackInitCode = generateAttackIntentInitialize(attackIntents, byteCodes, byteCodeIndices, ca.iterator(), cp);

        TreeMap<Integer, CodePiece> codeGraph = splitCodeStructure(byteCodes, byteCodeIndices, ca.iterator());
        TreeMap<CodePiece, CodePieceExtension> codeExtensions = fillPossibleSetMoves(codeGraph, byteCodeIndices, attackIntents);

        offset[0] = 0;
        for (int updatedCondition : updatedConditions) {
            insertModifyRandomResult(updatedCondition, codeGraph, codeExtensions, ca.iterator(), cp, offset);
        }

        insertAttackInitCode(attackInitCode, ca.iterator(), smt);
        if (ca.getMaxStack() < 6) {
            ca.setMaxStack(6);
        }

        if (showDecompile) {
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
                if (i + 1 < size && byteCodes.get(i + 1) == Opcode.BIPUSH) {
                    conditionEnd = i + 1;
                    foundBiPush = true;
                }
                if (!foundBiPush && i - 1 > 0 && byteCodes.get(i - 1) == Opcode.BIPUSH) {
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

    private static int updateConditionToBoolean(CodeIterator ci, StackMapTable smt, int condition, ConstPool constPool, int[] offset) {
        int op = ci.byteAt(condition);
        if (op == Opcode.IFEQ) {
            return condition;
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
                smt.set(smtUpdater.doit());
            } catch (BadBytecode e) {
                debug("TauntMaskPatches.updateConditionToBoolean: BadBytecode: " + e.getMessage());
                return -1;
            }

            offset[0] += 8;
            return condition + 8;
        }

        debug("TauntMaskPatches.updateConditionToBoolean: Unsupported byte code: " + Mnemonic.OPCODE[op]);
        return -1;
    }

    private static List<AttackIntentInfo> findAllAttackIndents(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
        List<SetMoveInfo> setMoves = findAllSetMoves(byteCodes, byteCodeIndices, iterator, constPool);
        List<AttackIntentInfo> result = new ArrayList<>();
        for (SetMoveInfo setMove : setMoves) {
            int index;
            if (setMove.type.endsWith("IIZ)V")) {
                index = previousMatchIntConstOrField(byteCodes, setMove.pos, iterator, constPool);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse isMultiDamage: " + setMove);
                    continue;
                }

                index = previousMatchIntConstOrField(byteCodes, index, iterator, constPool);
                if (index == -1) {
                    debug("TauntMaskPatches.findAllAttackIndents: Cannot parse multiplier: " + setMove);
                    continue;
                }

                index = previousMatchIntConstOrFieldOrDamageInfo(byteCodes, index, iterator, constPool);
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
                    result.add(new AttackIntentInfo(setMove.pos, index - 1, setMove.type));
                }

            } else if (setMove.type.endsWith("I)V")) {
                index = previousMatchIntConstOrFieldOrDamageInfo(byteCodes, setMove.pos, iterator, constPool);
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
                    result.add(new AttackIntentInfo(setMove.pos, index - 1, setMove.type));
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
                        result.add(new SetMoveInfo(i, type));
                    }
                }
            }
        }

        debug("TauntMaskPatches.findAllSetMove: result:" + result);
        return result;
    }

    private static Bytecode generateAttackIntentInitialize(List<AttackIntentInfo> attackIntents, List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator, ConstPool constPool) {
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
            bytecode.addAload(0);
            int startIndex = byteCodeIndices.get(attackIntent.getIntentFieldPos) + 3;
            int endIndex = byteCodeIndices.get(attackIntent.setMovePos);
            for (int i = startIndex; i < endIndex; i++) {
                bytecode.add(iterator.byteAt(i));
            }
            int lastSemicolon = attackIntent.type.lastIndexOf(';');
            bytecode.addInvokestatic(TauntMaskPatchesClassName,
                    "addSetMoveItem",
                    "(L" + AbstractMonsterClassName.replace('.', '/') + attackIntent.type.substring(lastSemicolon));
        }

        int afterSetMoveItems = bytecode.length();
        bytecode.write16bit(afterInitSetMove + 1, afterSetMoveItems - afterInitSetMove);

        return bytecode;
    }

    private static void insertAttackInitCode(Bytecode attackInitCode, CodeIterator iterator, StackMapTable smt) throws BadBytecode {
        iterator.insert(0, attackInitCode.get());
        StackMapTableUpdater smtUpdater = new StackMapTableUpdater(smt.get());
        smtUpdater.addFrame(attackInitCode.length(), StackMapTable.Writer::sameFrame);
        smt.set(smtUpdater.doit());
    }

    private static TreeMap<Integer, CodePiece> splitCodeStructure(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator) throws BadBytecode {
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

    private static TreeMap<CodePiece, CodePieceExtension> fillPossibleSetMoves(TreeMap<Integer, CodePiece> codes, List<Integer> byteCodeIndices, List<AttackIntentInfo> attackIntents) {
        TreeMap<CodePiece, CodePieceExtension> result = new TreeMap<>(Comparator.comparingInt(a -> a.start));
        for (Map.Entry<Integer, CodePiece> entry : codes.entrySet()) {
            result.put(entry.getValue(), new CodePieceExtension(entry.getKey()));
        }

        for (int i = 0, attackIntentsSize = attackIntents.size(); i < attackIntentsSize; i++) {
            AttackIntentInfo attackIntent = attackIntents.get(i);
            int codePos = byteCodeIndices.get(attackIntent.setMovePos);
            CodePiece startCode = codes.floorEntry(codePos).getValue();
            traverseToFillSetMoves(startCode, i, result);
        }

        debug("TauntMaskPatches.fillPossibleSetMoves: result:" +
                result.entrySet().stream().sorted(Comparator.comparingInt(a -> a.getKey().start)).map(Map.Entry::getValue).collect(Collectors.toList()));

        return result;
    }

    private static void traverseToFillSetMoves(CodePiece startCode, int setMoveId, TreeMap<CodePiece, CodePieceExtension> codes) {
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
                CodePieceExtension previousExt = codes.get(previous);
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

    private static int previousMatchIntConstOrField(List<Integer> byteCodes, int pos, CodeIterator iterator, ConstPool constPool) {
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

    private static int previousMatchIntConstOrFieldOrDamageInfo(List<Integer> byteCodes, int pos, CodeIterator iterator, ConstPool constPool) {
        int index = previousMatchIntConstOrField(byteCodes, pos, iterator, constPool);
        if (index != -1) {
            return index;
        }

        if (pos - 6 >= 0) {
            int indexOp = byteCodes.get(pos - 4);
            if (byteCodes.get(pos - 6) == Opcode.ALOAD_0 &&
                    byteCodes.get(pos - 5) == Opcode.GETFIELD &&
                    ((indexOp >= Opcode.ICONST_0 && indexOp <= Opcode.ICONST_5) || indexOp == Opcode.BIPUSH) &&
                    byteCodes.get(pos - 3) == Opcode.INVOKEVIRTUAL &&
                    byteCodes.get(pos - 2) == Opcode.CHECKCAST &&
                    byteCodes.get(pos - 1) == Opcode.GETFIELD) {
                return pos - 6;
            }
        }

        return -1;
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

    private static void debug(String messge) {
        System.out.println(messge);
    }

    static class StackMapTableUpdater extends StackMapTable.Walker {
        private final StackMapTable.Writer writer;
        private final List<Integer> newOffsets = new ArrayList<>();
        private final List<BiConsumer<StackMapTable.Writer, Integer>> newOffsetOperations = new ArrayList<>();
        private final List<Boolean> newOffsetIsReplace = new ArrayList<>();
        private int newOffsetIndex;
        private int offset;

        public StackMapTableUpdater(byte[] data) {
            super(data);
            this.writer = new StackMapTable.Writer(data.length);
        }

        public void addFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
            newOffsets.add(offset);
            newOffsetOperations.add(operation);
            newOffsetIsReplace.add(false);
        }

        public void replaceFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
            newOffsets.add(offset);
            newOffsetOperations.add(operation);
            newOffsetIsReplace.add(true);
        }

        public byte[] doit() throws BadBytecode {
            this.newOffsets.sort(Comparator.comparingInt(a -> a));
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
                int nextSameFrameOffset = this.newOffsets.get(newOffsetIndex);
                if (offset + offsetDelta + 1 > nextSameFrameOffset) {
                    int newOffsetDelta = nextSameFrameOffset - offset;

                    if (newOffsetIsReplace.get(newOffsetIndex)) {
                        skipNextFrame = true;
                        newOffsetOperations.get(newOffsetIndex).accept(this.writer, offsetDelta);
                    } else {
                        newOffsetOperations.get(newOffsetIndex).accept(this.writer, newOffsetDelta - 1);
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
        public List<Integer> nextSetMoveIds = new ArrayList<>();
        public List<Integer> branchSetMoveIds = new ArrayList<>();
        public CodePieceExtension(int start) {
            this.start = start;
        }

        @Override
        public String toString() {
            return String.format("CodePieceExtension(%X, %S, %S)",
                    start,
                    branchSetMoveIds.stream().map(Integer::toHexString).collect(Collectors.toList()),
                    nextSetMoveIds.stream().map(Integer::toHexString).collect(Collectors.toList()));
        }
    }
}
