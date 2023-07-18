package io.chaofan.sts.chaofanmod.utils;

import javassist.bytecode.*;

import java.util.*;

public class StackAnalyzer {
    public static int[] getStacks(CodeIterator ci, int startIndex, int targetIndex) throws BadBytecode {
        List<Integer> byteCodes = new ArrayList<>();
        List<Integer> byteCodeIndices = new ArrayList<>();
        CodeSplitter.getCodes(ci, byteCodes, byteCodeIndices);
        TreeMap<Integer, CodeSplitter.CodePiece> codes = CodeSplitter.split(byteCodes, byteCodeIndices, ci);
        CodeSplitter.CodePiece startCode = codes.floorEntry(startIndex).getValue();
        CodeSplitter.CodePiece targetCode = codes.floorEntry(targetIndex).getValue();
        Map<CodeSplitter.CodePiece, CodeSplitter.CodePiece> route = getRoute(startCode, targetCode);
        if (route == null) {
            return null;
        }

        Stack<Integer> stack = new Stack<>();
        int exhaustedStack = 0;

        ci.move(startIndex);
        CodeSplitter.CodePiece currentCode = startCode;
        int pos = ci.next();
        while (pos != targetIndex) {
            int shrink = getStackShrink(ci, pos);
            for (int i = 0; i < shrink; i++) {
                if (stack.empty()) {
                    exhaustedStack++;
                } else {
                    stack.pop();
                }
            }

            int grow = getStackGrow(ci, pos);
            for (int i = 0; i < grow; i++) {
                if (exhaustedStack > 0) {
                    exhaustedStack--;
                } else {
                    stack.push(pos);
                }
            }

            pos = ci.next();
            if (pos >= currentCode.end) {
                currentCode = route.get(currentCode);
                ci.move(currentCode.start);
                pos = ci.next();
            }
        }

        return stack.stream().mapToInt(i -> i).toArray();
    }

    private static Map<CodeSplitter.CodePiece, CodeSplitter.CodePiece> getRoute(CodeSplitter.CodePiece startCode, CodeSplitter.CodePiece targetCode) {
        Stack<CodeSplitter.CodePiece> stack = new Stack<>();
        stack.push(startCode);
        Map<CodeSplitter.CodePiece, CodeSplitter.CodePiece> previous = new HashMap<>();
        boolean found = false;

        while (!stack.empty()) {
            CodeSplitter.CodePiece currentCode = stack.pop();
            if (currentCode == targetCode) {
                found = true;
                break;
            }

            if (currentCode.branches != null) {
                for (CodeSplitter.CodePiece branch : currentCode.branches) {
                    if (!previous.containsKey(branch)) {
                        stack.push(branch);
                        previous.put(branch, currentCode);
                    }
                }
            }

            if (currentCode.next != null) {
                CodeSplitter.CodePiece next = currentCode.next;
                if (!previous.containsKey(next)) {
                    stack.push(next);
                    previous.put(next, currentCode);
                }
            }
        }

        if (!found) {
            return null;
        }

        Map<CodeSplitter.CodePiece, CodeSplitter.CodePiece> route = new HashMap<>();
        CodeSplitter.CodePiece currentCode = targetCode;
        while (currentCode != startCode) {
            CodeSplitter.CodePiece previousCode = previous.get(currentCode);
            route.put(previousCode, currentCode);
            currentCode = previousCode;
        }

        return route;
    }

    private static int getStackShrink(CodeIterator ci, int pos) {
        String desc;
        int op = ci.byteAt(pos);
        switch (op) {
            case Opcode.PUTSTATIC:
                return getFieldSize(ci, pos);
            case Opcode.PUTFIELD:
                return getFieldSize(ci, pos) + 1;
            case Opcode.WIDE:
                return -stackShrink[ci.byteAt(pos + 1)];
            case Opcode.MULTIANEWARRAY:
                return ci.byteAt(pos + 3);
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKEVIRTUAL:
                desc = ci.get().getConstPool().getMethodrefType(ci.u16bitAt(pos + 1));
                return Descriptor.paramSize(desc) + 1;
            case Opcode.INVOKESTATIC:
                desc = ci.get().getConstPool().getMethodrefType(ci.u16bitAt(pos + 1));
                return Descriptor.paramSize(desc);
            case Opcode.INVOKEINTERFACE:
                desc = ci.get().getConstPool().getInterfaceMethodrefType(ci.u16bitAt(pos + 1));
                return Descriptor.paramSize(desc) + 1;
            case Opcode.INVOKEDYNAMIC:
                desc = ci.get().getConstPool().getInvokeDynamicType(ci.u16bitAt(pos + 1));
                return Descriptor.paramSize(desc);
            default:
                return -stackShrink[op];
        }
    }

    private static int getStackGrow(CodeIterator ci, int pos) {
        String desc;
        int op = ci.byteAt(pos);
        switch (op) {
            case Opcode.GETSTATIC:
            case Opcode.GETFIELD:
                return getFieldSize(ci, pos);
            case Opcode.WIDE:
                return stackGrow[ci.byteAt(pos + 1)];
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESTATIC:
                desc = ci.get().getConstPool().getMethodrefType(ci.u16bitAt(pos + 1));
                return getReturnSize(desc);
            case Opcode.INVOKEINTERFACE:
                desc = ci.get().getConstPool().getInterfaceMethodrefType(ci.u16bitAt(pos + 1));
                return getReturnSize(desc);
            case Opcode.INVOKEDYNAMIC:
                desc = ci.get().getConstPool().getInvokeDynamicType(ci.u16bitAt(pos + 1));
                return getReturnSize(desc);
            default:
                return stackGrow[op];
        }
    }

    private static int getReturnSize(String desc) {
        char ret = desc.charAt(desc.lastIndexOf(')') + 1);
        return ret == 'V' ? 0 : (ret == 'J' || ret == 'D' ? 2 : 1);
    }

    private static int getFieldSize(CodeIterator ci, int index) {
        ConstPool cp = ci.get().getConstPool();
        String desc = cp.getFieldrefType(ci.u16bitAt(index + 1));
        return Descriptor.dataSize(desc);
    }

    private static final int D = 999;
    private static final int[] stackShrink = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 1
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-2,-2, // 2
           -2,-2,-2,-2,-2,-2,-1,-2,-1,-2,-1,-1,-1,-1,-1,-2, // 3
           -2,-2,-2,-1,-1,-1,-1,-2,-2,-2,-2,-1,-1,-1,-1,-3, // 4
           -4,-3,-4,-3,-3,-3,-3,-1,-2, 0,-2,-3, 0,-3,-4,-2, // 5
           -2,-4,-2,-4,-2,-4,-2,-4,-2,-4,-2,-4,-2,-4,-2,-4, // 6
           -2,-4,-2,-4,-1,-2,-1,-2,-2,-3,-2,-3,-2,-3,-2,-4, // 7
           -2,-4,-2,-4, 0,-1,-1,-1,-2,-2,-2,-1,-1,-1,-2,-2, // 8
           -2,-1,-1,-1,-4,-2,-2,-4,-4,-1,-1,-1,-1,-1,-1,-2, // 9
           -2,-2,-2,-2,-2,-2,-2, 0, 0, 0,-1,-1,-1,-2,-1,-2, // a
           -1, 0, 0, D,-1, D, D, D, D, D, D, 0,-1,-1,-1,-1, // b
            0,-1,-1,-1, D, D,-1,-1, 0, 0,                   // c
    };
    private static final int[] stackGrow = {
            0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 2, 2, // 0
            1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 2, // 1
            2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 2, // 2
            1, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 3
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 4
            0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 4, 2, 5, 6, 2, // 5
            1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, // 6
            1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, // 7
            1, 2, 1, 2, 0, 2, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, // 8
            1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, // 9
            0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, // a
            0, 0, D, 0, D, 0, D, D, D, D, D, 1, 1, 1, 1, 0, // b
            0, 1, 0, 0, D, 1, 0, 0, 0, 1,                   // c
    };
}
