package io.chaofan.sts.chaofanmod.utils;

import com.evacipated.cardcrawl.modthespire.ModInfo;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.patches.TauntMaskPatches;
import javassist.bytecode.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtils {
    public static URI modInfoToUri(ModInfo modInfo) {
        try {
            return modInfo.jarURL.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void getClassesFromJar(URI jar, List<String> results) {
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
            ChaofanMod.logger.debug("ClassUtils.getClassesFromJar: failed to get classes from jar: " + jar);
        }
    }

    public static void printCode(CodeAttribute ca) throws BadBytecode {
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
            } else if (lastOp == Opcode.GETSTATIC || lastOp == Opcode.GETFIELD || lastOp == Opcode.PUTSTATIC || lastOp == Opcode.PUTFIELD) {
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
}
