package io.chaofan.sts.chaofanmod.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.relics.MsWrithing;
import io.chaofan.sts.chaofanmod.utils.ClassUtils;
import io.chaofan.sts.chaofanmod.utils.CodePattern;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.chaofan.sts.chaofanmod.relics.MsWrithing.DISABLE_RELIC_DURATION;

public class MsWrithingPatches {
    private static boolean doDisabledRelicCheck = true;

    @SpirePatch(clz = AbstractRelic.class, method = SpirePatch.CLASS)
    public static class Fields {
        public static SpireField<Boolean> disabled = new SpireField<>(() -> false);
        public static SpireField<Float> disabledProgress = new SpireField<>(() -> 0f);
        public static SpireField<PowerTip> disabledTooltip = new SpireField<>(() -> null);
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class DisableLifecyclePatchV2 {
        @SpireRawPatch
        public static void Raw(CtBehavior method) throws NotFoundException {
            SpireConfig config = ChaofanMod.tryCreateConfig();
            if (config != null && config.has(ChaofanMod.DISABLE_MS_WRITHING) && config.getBool(ChaofanMod.DISABLE_MS_WRITHING)) {
                return;
            }

            // conflict mod
            if (Loader.isModLoadedOrSideloaded("testmod")) {
                return;
            }

            ClassPool pool = method.getDeclaringClass().getClassPool();
            CtClass abstractRelicClass = pool.get(AbstractRelic.class.getName());

            List<URI> jars = Arrays.stream(Loader.MODINFOS)
                    .map(ClassUtils::modInfoToUri)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            jars.add(new File(Loader.STS_JAR).getAbsoluteFile().toURI());

            List<String> classes = new ArrayList<>();
            for (URI jar : jars) {
                ClassUtils.getClassesFromJar(jar, classes);
            }

            for (String clzName : classes) {
                if (clzName.startsWith("io.chaofan.sts.chaofanmod.patches.MsWrithingPatches")) {
                    continue;
                }
                try {
                    CtClass clz = pool.get(clzName);
                    patchClass(clz);

                    if (clz.subclassOf(abstractRelicClass) && clz != abstractRelicClass) {
                        addRelicInfo(clz);
                    }
                } catch (NotFoundException ignored) {
                }
            }
        }

        private static void patchClass(CtClass clz) {
            try {
                for (CtMethod method : clz.getDeclaredMethods()) {
                    try {
                        patchMethod(method);
                    } catch (CannotCompileException ignored) {
                    }
                }
                for (CtConstructor method : clz.getConstructors()) {
                    try {
                        patchMethod(method);
                    } catch (CannotCompileException ignored) {
                    }
                }
            } catch (Throwable ignored) {}
        }

        private static void patchMethod(CtBehavior method) throws CannotCompileException {
            if (method.getName().equals("renderRelics") && method.getDeclaringClass().getName().equals(AbstractPlayer.class.getName())) {
                return;
            }
            if (method.getName().equals("update") && method.getDeclaringClass().getName().equals(OverlayMenu.class.getName())) {
                return;
            }
            boolean[] shouldPatch = new boolean[] { false };
            method.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess f) {
                    if (f.getFieldName().equals("relics") && f.getClassName().equals(AbstractPlayer.class.getName())) {
                        shouldPatch[0] = true;
                    }
                }
            });
            if (shouldPatch[0]) {
                method.instrument(new ExprEditor() {
                    @Override
                    public void edit(MethodCall m) throws CannotCompileException {
                        String methodName = m.getMethodName();
                        String className = m.getClassName();
                        if (methodName.equals("iterator") && className.equals(ArrayList.class.getName())) {
                            m.replace(String.format("{ $_ = %s.replaceIterator($0, $proceed($$)); }", MsWrithingPatches.class.getName()));
                        }
                        if (methodName.equals("stream") && className.equals(ArrayList.class.getName())) {
                            m.replace(String.format("{ $_ = %s.replaceStream($0, $proceed($$)); }", MsWrithingPatches.class.getName()));
                        }
                    }
                });

                System.out.println("MrWrithingPatches.patchMethod: " + method.getDeclaringClass().getName() + "." + method.getName() + method.getSignature() + " patched successfully.");
            }
        }

        public static void addRelicInfo(CtClass clz) {
            try {
                if (Arrays.stream(clz.getInterfaces()).anyMatch(i -> i.getName().equals(DisableRelic.class.getName()))) {
                    return;
                }

                CtMethod method = clz.getDeclaredMethod("onUnequip");
                MethodInfo mi = method.getMethodInfo();
                ConstPool constPool = mi.getConstPool();
                CodeAttribute ca = mi.getCodeAttribute();
                List<CodePattern.Range> modifyHandSize = CodePattern.find(ca.iterator(),
                        CodePattern.sequence(
                                new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l))),
                                new CodePattern(Opcode.ISUB),
                                new CodePattern(Opcode.PUTFIELD, (i, l) -> {
                                    int argument = i.s16bitAt(l + 1);
                                    return constPool.getFieldrefName(argument).equals("masterHandSize") &&
                                            constPool.getFieldrefClassName(argument).equals(AbstractPlayer.class.getName());
                                })
                        ));

                StringBuilder code = new StringBuilder();
                for (CodePattern.Range range : modifyHandSize) {
                    CodeIterator ci = ca.iterator();
                    int opcode = ci.byteAt(range.start);
                    int number = opcode == Opcode.BIPUSH ? ci.byteAt(range.start + 1) : (opcode - Opcode.ICONST_0);
                    code.append(AbstractDungeon.class.getName())
                            .append(".player.gameHandSize -= ")
                            .append(number)
                            .append(";");
                }

                List<CodePattern.Range> modifyEnergy = CodePattern.find(ca.iterator(),
                        CodePattern.sequence(
                                new CodePattern((i, l) -> isIconstOrBipush(i.byteAt(l))),
                                new CodePattern(Opcode.ISUB),
                                new CodePattern(Opcode.PUTFIELD, (i, l) -> {
                                    int argument = i.s16bitAt(l + 1);
                                    return constPool.getFieldrefName(argument).equals("energyMaster") &&
                                            constPool.getFieldrefClassName(argument).equals(EnergyManager.class.getName());
                                })
                        ));

                for (CodePattern.Range range : modifyEnergy) {
                    CodeIterator ci = ca.iterator();
                    int opcode = ci.byteAt(range.start);
                    int number = opcode == Opcode.BIPUSH ? ci.byteAt(range.start + 1) : (opcode - Opcode.ICONST_0);
                    code.append(AbstractDungeon.class.getName())
                            .append(".player.energy.energy -= ")
                            .append(number)
                            .append(";");
                }

                if (code.length() > 0) {
                    CtMethod disableMethod = CtNewMethod.make("public void disableByMsWrithing() { " + code + " }", clz);
                    CtMethod[] methods = clz.getMethods();
                    if (Arrays.stream(methods).noneMatch(m -> m.getName().equals("disableByMsWrithing") && m.getSignature().equals("()V"))) {
                        clz.addMethod(disableMethod);
                    }
                    CtMethod enableMethod = CtNewMethod.make("public void enableByMsWrithing() {}", clz);
                    if (Arrays.stream(methods).noneMatch(m -> m.getName().equals("enableByMsWrithing") && m.getSignature().equals("()V"))) {
                        clz.addMethod(enableMethod);
                    }
                    clz.addInterface(clz.getClassPool().get(DisableRelic.class.getName()));
                }

                System.out.println("MrWrithingPatches.addRelicInfo: " + clz.getName() + " patched successfully.");

            } catch (NotFoundException | BadBytecode | CannotCompileException ignored) { }
        }

        private static boolean isIconstOrBipush(int indexOp) {
            return (indexOp >= Opcode.ICONST_0 && indexOp <= Opcode.ICONST_5) || indexOp == Opcode.BIPUSH;
        }
    }

    public interface DisableRelic {
        void disableByMsWrithing();
        void enableByMsWrithing();
    }

    public static Iterator<?> replaceIterator(ArrayList<?> list, Iterator<?> iterator) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null && list == player.relics && doDisabledRelicCheck) {
            return new RemoveDisabledIterator((Iterator<AbstractRelic>) iterator);
        }
        return iterator;
    }

    public static Stream<?> replaceStream(ArrayList<?> list, Stream<?> stream) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null && list == player.relics && doDisabledRelicCheck) {
            return stream.filter(relic -> !Fields.disabled.get(relic));
        }
        return stream;
    }

    private static class RemoveDisabledIterator implements Iterator<AbstractRelic> {
        private final Iterator<AbstractRelic> iterator;
        private AbstractRelic next;

        public RemoveDisabledIterator(Iterator<AbstractRelic> iterator) {
            this.iterator = iterator;
            this.next = null;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }

            while (iterator.hasNext()) {
                AbstractRelic next = iterator.next();
                if (Fields.disabled.get(next)) {
                    continue;
                }

                this.next = next;
                return true;
            }

            return false;
        }

        @Override
        public AbstractRelic next() {
            if (!hasNext()) {
                throw new IllegalStateException("No next element");
            }

            AbstractRelic next = this.next;
            this.next = null;
            return next;
        }

        @Override
        public void remove() {
            if (this.next == null) {
                iterator.remove();
            } else {
                throw new IllegalStateException("Missed chance to remove");
            }
        }
    }

    @SpirePatch(clz = AbstractRelic.class, method = "renderTip")
    public static class FixTipPositionPatch {
        @SpirePrefixPatch
        public static void Prefix() {
            doDisabledRelicCheck = false;
        }

        @SpirePostfixPatch
        public static void Postfix() {
            doDisabledRelicCheck = true;
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "onVictory")
    public static class ResetRelicDisablePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer instance) {
            for (AbstractRelic relic : instance.relics) {
                if (!Fields.disabled.get(relic)) {
                    continue;
                }

                Fields.disabled.set(relic, false);
                Fields.disabledProgress.set(relic, DISABLE_RELIC_DURATION);
                PowerTip powerTip = Fields.disabledTooltip.get(relic);
                if (powerTip != null) {
                    relic.tips.remove(powerTip);
                }
                if (relic instanceof MsWrithingPatches.DisableRelic) {
                    ((MsWrithingPatches.DisableRelic) relic).enableByMsWrithing();
                }
            }
        }
    }

    @SpirePatch(clz = AbstractRelic.class, method = "update")
    public static class UpdateDisablePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractRelic instance) {
            float progress = Fields.disabledProgress.get(instance);
            progress -= Gdx.graphics.getDeltaTime();
            if (progress < 0) {
                progress = 0;
            }
            Fields.disabledProgress.set(instance, progress);
        }
    }

    @SpirePatch(clz = AbstractRelic.class, method = "renderInTopPanel")
    public static class RenderDisablePatch {
        private static final Color color = Color.WHITE.cpy();

        @SpireInsertPatch(locator = Locator.class)
        public static void Insert(AbstractRelic instance, SpriteBatch sb) {
            float progress = Fields.disabledProgress.get(instance) / DISABLE_RELIC_DURATION;
            float scale = Settings.scale;
            boolean shouldRender = false;
            if (Fields.disabled.get(instance)) {
                color.a = 1 - progress;
                scale *= (1 + 0.5f * progress);
                shouldRender = true;
            } else if (progress > 0) {
                color.a = progress;
                shouldRender = true;
            }
            if (shouldRender) {
                sb.setColor(color);
                sb.draw(
                        MsWrithing.DISABLED_RELIC,
                        instance.currentX - 64.0F,
                        instance.currentY - 64.0F,
                        64.0F,
                        64.0F,
                        128.0F,
                        128.0F,
                        instance.scale * scale,
                        instance.scale * scale,
                        0,
                        0,
                        0,
                        128,
                        128,
                        false,
                        false);
            }
        }

        public static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(Hitbox.class, "render");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }
    }
}
