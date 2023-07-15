package io.chaofan.sts.chaofanmod.utils;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

public class CodePattern {
    private final int[] ops;
    private final BiPredicate<CodeIterator, Integer> condition;
    private final CodePattern[] matches;
    private final MatchType matchType;

    public CodePattern(int op) {
        this(new int[]{op}, null);
    }

    public CodePattern(int[] ops) {
        this(ops, null);
    }

    public CodePattern(BiPredicate<CodeIterator, Integer> condition) {
        this(null, condition);
    }

    public CodePattern(int op, BiPredicate<CodeIterator, Integer> condition) {
        this(new int[]{op}, condition);
    }

    public CodePattern(int[] ops, BiPredicate<CodeIterator, Integer> condition) {
        this.ops = ops;
        this.condition = condition;
        this.matches = null;
        this.matchType = null;
    }

    public CodePattern(CodePattern[] matches, MatchType matchType) {
        this.matchType = matchType;
        this.ops = null;
        this.condition = null;
        this.matches = matches;
    }

    public int test(CodeIterator ci, int location) {
        if (matches != null) {
            if (matchType == MatchType.ANY) {
                return Arrays.stream(matches).map(m -> m.test(ci, location)).filter(r -> r >= 0).findAny().orElse(-1);
            } else if (matchType == MatchType.SEQUENCE) {
                int localLocation = location;
                for (CodePattern match : matches) {
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

        if (ops != null && Arrays.stream(ops).allMatch(op -> ci.byteAt(location) != op)) {
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

    public static CodePattern anyOf(int... ops) {
        return new CodePattern(Arrays.stream(ops).mapToObj(CodePattern::new).toArray(CodePattern[]::new), MatchType.ANY);
    }

    public static CodePattern anyOf(CodePattern... codePatterns) {
        return new CodePattern(codePatterns, MatchType.ANY);
    }

    public static CodePattern sequence(CodePattern... codePatterns) {
        return new CodePattern(codePatterns, MatchType.SEQUENCE);
    }

    public static CodePattern sequence(int... ops) {
        return new CodePattern(Arrays.stream(ops).mapToObj(CodePattern::new).toArray(CodePattern[]::new), MatchType.SEQUENCE);
    }

    public static CodePattern unorderedPair(CodePattern a, CodePattern b) {
        return CodePattern.anyOf(
                CodePattern.sequence(a, b),
                CodePattern.sequence(b, a)
        );
    }

    public static CodePattern prefixAnyCodes(CodePattern codePattern, int maxNumber) {
        CodePattern any = any();
        List<CodePattern> sequences = new ArrayList<>();
        List<CodePattern> sequencesWithAny = new ArrayList<>();
        sequencesWithAny.add(codePattern);
        for (int i = 0; i < maxNumber; i++) {
            sequences.add(sequence(sequencesWithAny.toArray(new CodePattern[0])));
            sequencesWithAny.add(sequencesWithAny.size() - 1, any);
        }
        sequences.add(sequence(sequencesWithAny.toArray(new CodePattern[0])));
        return anyOf(sequences.toArray(new CodePattern[0]));
    }

    public static CodePattern any() {
        return new CodePattern((int[]) null);
    }

    public static List<Range> find(CodeIterator ci, CodePattern codePattern) throws BadBytecode {
        List<Range> result = new ArrayList<>();
        ci.move(0);
        while (ci.hasNext()) {
            int start = ci.next();
            int end = codePattern.test(ci, start);
            if (end != -1) {
                result.add(new Range(start, end));
            }
        }
        return result;
    }

    enum MatchType {
        SEQUENCE,
        ANY,
    }

    public static class Range {
        public final int start;
        public final int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return String.format("Range(%X, %X)", start, end);
        }
    }
}
