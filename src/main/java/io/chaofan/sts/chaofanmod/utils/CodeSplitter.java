package io.chaofan.sts.chaofanmod.utils;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;

import java.util.*;
import java.util.stream.Collectors;

public class CodeSplitter {
    public static TreeMap<Integer, CodePiece> split(List<Integer> byteCodes, List<Integer> byteCodeIndices, CodeIterator iterator) {
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

        return codes;
    }

    public static class CodePiece {
        public final int start;
        public int end;
        public List<CodePiece> branches;
        public CodePiece next;
        public TreeSet<CodePiece> previous = new TreeSet<>(Comparator.comparingInt(a -> a.start));

        public CodePiece(int start, int end, List<CodePiece> branches, CodePiece next) {
            this.start = start;
            this.end = end;
            this.setBranches(branches);
            this.setNext(next);
        }

        public CodePiece split(int pos) {
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
}
