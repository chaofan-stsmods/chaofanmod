package io.chaofan.sts.chaofanmod.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapGenerator;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.megacrit.cardcrawl.vfx.FlameAnimationEffect;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.mods.SummarizedMap;
import io.chaofan.sts.chaofanmod.relics.ZMap;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.lwjgl.util.vector.Vector2f;

import java.lang.reflect.Field;
import java.util.*;

public class ZMapPatches {
    private static final Color SUMMARY_COLOR = new Color(0.26F, 0.26F, 0.26F, 1.0F);
    private static final Color OUTLINE_COLOR = Color.valueOf("8c8c80ff");
    private static final Map<String, Texture> iconMap = new HashMap<>();

    private static final float JITTER_X = Settings.isMobile ? (13.0F * Settings.xScale) : (14.0F * Settings.xScale);
    private static final float JITTER_Y = Settings.isMobile ? (18.0F * Settings.xScale) : (12.0F * Settings.xScale);
    private static boolean isBeforeFirstRoom = false;

    @SpirePatch(clz = MapRoomNode.class, method = SpirePatch.CLASS)
    public static class MapRoomNodeFields {
        public static SpireField<Map<String, Integer>> maxCountByRoomType = new SpireField<>(() -> null);
        public static SpireField<Map<String, Integer>> minCountByRoomType = new SpireField<>(() -> null);
        public static SpireField<MapEdge> summaryEdge = new SpireField<>(() -> null);
        public static SpireField<Vector2f> summaryOffset = new SpireField<>(() -> null);
        public static SpireField<Boolean> childrenHaveEmeraldKey = new SpireField<>(() -> false);
        public static SpireField<ArrayList<FlameAnimationEffect>> effects = new SpireField<>(() -> null);
        public static SpireField<Float> flameVfxTimer = new SpireField<>(() -> 0f);
        public static SpireField<Hitbox> eliteIconHitbox = new SpireField<>(() -> null);
        public static SpireField<Boolean> lastEffectFlipped = new SpireField<>(() -> false);
    }

    @SpirePatch(clz = MapRoomNode.class, method = "update")
    public static class MapRoomNodeUpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(MapRoomNode node) {
            if (!hasZMapRelic()) {
                return;
            }

            MapRoomNode currentMapNode = AbstractDungeon.getCurrMapNode();
            if ((currentMapNode == null || highestVisibleRoomY(currentMapNode) != node.y) &&
                    (!isSelectingFirstRoom() || node.y != 0)) {
                return;
            }

            if (currentMapNode != null && node != currentMapNode && (node.y != 0 || currentMapNode.y == node.y) && !currentMapNode.isConnectedTo(node)) {
                return;
            }

            boolean showFire = MapRoomNodeFields.childrenHaveEmeraldKey.get(node);
            if (showFire) {
                ArrayList<FlameAnimationEffect> effects = MapRoomNodeFields.effects.get(node);
                if (effects == null) {
                    effects = new ArrayList<>();
                    MapRoomNodeFields.effects.set(node, effects);
                }

                for (Iterator<FlameAnimationEffect> iterator = effects.iterator(); iterator.hasNext(); ) {
                    FlameAnimationEffect effect = iterator.next();
                    if (effect.isDone) {
                        effect.dispose();
                        iterator.remove();
                    }
                }

                for (FlameAnimationEffect effect : effects) {
                    effect.update();
                }
            }
        }
    }

    @SpirePatch(clz = MapRoomNode.class, method = "render")
    public static class MapRoomNodeRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(MapRoomNode node, SpriteBatch sb) {
            MapRoomNode currentMapNode = AbstractDungeon.getCurrMapNode();
            if (hasZMapRelic()) {
                if (currentMapNode != null && highestVisibleRoomY(currentMapNode) < node.y) {
                    return SpireReturn.Return();
                }
                if (isSelectingFirstRoom() && node.y > 0) {
                    return SpireReturn.Return();
                }
            } else {
                return SpireReturn.Continue();
            }

            if ((currentMapNode == null || highestVisibleRoomY(currentMapNode) != node.y) &&
                    (!isSelectingFirstRoom() || node.y != 0)) {
                return SpireReturn.Continue();
            }

            if (currentMapNode != null && node != currentMapNode && (node.y != 0 || currentMapNode.y == node.y) && !currentMapNode.isConnectedTo(node)) {
                return SpireReturn.Continue();
            }

            Map<String, Integer> minCount = MapRoomNodeFields.minCountByRoomType.get(node);
            Map<String, Integer> maxCount = MapRoomNodeFields.maxCountByRoomType.get(node);
            if (minCount == null || maxCount == null) {
                return SpireReturn.Continue();
            }

            MapEdge edge = MapRoomNodeFields.summaryEdge.get(node);
            if (edge != null) {
                edge.render(sb);
            }

            return SpireReturn.Continue();
        }

        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(MapEdge.class.getName()) && m.getMethodName().equals("render")) {
                        m.replace(String.format("if (%s.shouldShowEdge(this)) { $_ = $proceed($$); }", ZMapPatches.class.getName()));
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void Postfix(MapRoomNode node, SpriteBatch sb) {
            if (!hasZMapRelic()) {
                return;
            }

            MapRoomNode currentMapNode = AbstractDungeon.getCurrMapNode();
            if ((currentMapNode == null || highestVisibleRoomY(currentMapNode) != node.y) &&
                    (!isSelectingFirstRoom() || node.y != 0)) {
                return;
            }

            if (currentMapNode != null && node != currentMapNode && (node.y != 0 || currentMapNode.y == node.y) && !currentMapNode.isConnectedTo(node)) {
                return;
            }

            Map<String, Integer> minCount = MapRoomNodeFields.minCountByRoomType.get(node);
            Map<String, Integer> maxCount = MapRoomNodeFields.maxCountByRoomType.get(node);
            if (minCount == null || maxCount == null) {
                return;
            }

            MapEdge edge = MapRoomNodeFields.summaryEdge.get(node);
            if (edge == null) {
                return;
            }

            float IMG_WIDTH = 64 * Settings.scale;
            float SPACING_X = Settings.isMobile ? (IMG_WIDTH * 2.2F) : (IMG_WIDTH * 2.0F);
            float OFFSET_Y = 150.0F * Settings.scale;

            int itemCount = 0;
            int eliteIndex = -1;
            for (Map.Entry<String, Integer> entry : minCount.entrySet()) {
                String roomType = entry.getKey();
                int min = entry.getValue();
                int max = maxCount.get(roomType);

                if (node.room.getMapSymbol().equals(roomType) && min == 1 && max == 1) {
                    continue;
                }

                if (entry.getKey().equals("E")) {
                    eliteIndex = itemCount;
                }
                itemCount++;
            }

            if (itemCount == 0) {
                return;
            }

            Vector2f summaryOffset = MapRoomNodeFields.summaryOffset.get(node);
            float x = edge.dstX * SPACING_X + MapRoomNode.OFFSET_X + summaryOffset.x - 40 * Settings.scale;
            float y = edge.dstY * Settings.MAP_DST_Y + OFFSET_Y + DungeonMapScreen.offsetY + summaryOffset.y + itemCount * 32f * Settings.scale;

            if (eliteIndex >= 0) {
                boolean showFire = MapRoomNodeFields.childrenHaveEmeraldKey.get(node);
                if (showFire) {
                    Hitbox eliteIconHitbox = MapRoomNodeFields.eliteIconHitbox.get(node);
                    if (eliteIconHitbox == null) {
                        float hitboxSize = Settings.isMobile ? (114.0F * Settings.scale) : (64.0F * Settings.scale);
                        eliteIconHitbox = new Hitbox(hitboxSize, hitboxSize);
                        MapRoomNodeFields.eliteIconHitbox.set(node, eliteIconHitbox);
                    }
                    eliteIconHitbox.move(x, y - Settings.scale * 32f * eliteIndex);

                    float flameVfxTimer = MapRoomNodeFields.flameVfxTimer.get(node);
                    flameVfxTimer -= Gdx.graphics.getDeltaTime();
                    ArrayList<FlameAnimationEffect> effects = MapRoomNodeFields.effects.get(node);
                    if (effects == null) {
                        effects = new ArrayList<>();
                        MapRoomNodeFields.effects.set(node, effects);
                    }

                    for (FlameAnimationEffect effect : effects) {
                        effect.render(sb, 0.5f);
                    }

                    if (flameVfxTimer < 0f) {
                        flameVfxTimer = MathUtils.random(0.2F, 0.4F);
                        FlameAnimationEffect newEffect = new FlameAnimationEffect(eliteIconHitbox);
                        try {
                            Field flipped = FlameAnimationEffect.class.getDeclaredField("flipped");
                            flipped.setAccessible(true);
                            boolean newFlipped = !MapRoomNodeFields.lastEffectFlipped.get(node);
                            flipped.set(newEffect, newFlipped);
                            MapRoomNodeFields.lastEffectFlipped.set(node, newFlipped);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {
                            ChaofanMod.logger.warn(ignored);
                        }
                        effects.add(newEffect);
                    }
                    MapRoomNodeFields.flameVfxTimer.set(node, flameVfxTimer);
                }

                sb.setColor(OUTLINE_COLOR);
                sb.draw(ImageMaster.MAP_NODE_ELITE_OUTLINE,
                        x - 64.0F,
                        y - 64.0F - Settings.scale * 32f * eliteIndex,
                        64.0F,
                        64.0F,
                        128.0F,
                        128.0F,
                        Settings.scale * 0.5f,
                        Settings.scale * 0.5f,
                        0.0F,
                        0,
                        0,
                        128,
                        128,
                        false,
                        false);
            }

            for (Map.Entry<String, Integer> entry : minCount.entrySet()) {
                String roomType = entry.getKey();
                int min = entry.getValue();
                int max = maxCount.get(roomType);

                if (node.room.getMapSymbol().equals(roomType)) {
                    min--;
                    max--;
                    if (min == 0 && max == 0) {
                        continue;
                    }
                }

                Texture texture = iconMap.computeIfAbsent(roomType, k -> ImageMaster.MAP_NODE_EVENT);

                sb.setColor(SUMMARY_COLOR);
                sb.draw(texture,
                        x - 64.0F,
                        y - 64.0F,
                        64.0F,
                        64.0F,
                        128.0F,
                        128.0F,
                        Settings.scale * 0.5f,
                        Settings.scale * 0.5f,
                        0.0F,
                        0,
                        0,
                        128,
                        128,
                        false,
                        false);

                String text = min == max ? String.valueOf(min) : String.format("%d~%d", min, max);
                FontHelper.renderFontLeftTopAligned(
                        sb,
                        FontHelper.panelNameFont,
                        text,
                        x + 28 * Settings.scale,
                        y + 14 * Settings.scale,
                        SUMMARY_COLOR);

                y = y - Settings.scale * 32f;
            }
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "updateImage")
    public static class DungeonMapScreenUpdateImagePatch {
        @SpirePrefixPatch
        public static void Prefix(DungeonMapScreen instance) {
            if (!hasZMapRelic()) {
                return;
            }

            ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
            for (int mapSize = map.size(), i = mapSize - 1; i >= 0; i--) {
                ArrayList<MapRoomNode> row = map.get(i);
                for (MapRoomNode node : row) {
                    if (node.hasEdges()) {
                        calculateForRoom(node);
                    }
                }
            }
        }

        private static void calculateForRoom(MapRoomNode node) {
            Map<String, Integer> minCount = MapRoomNodeFields.minCountByRoomType.get(node);
            Map<String, Integer> maxCount = MapRoomNodeFields.maxCountByRoomType.get(node);
            if (minCount == null) {
                minCount = new HashMap<>();
                MapRoomNodeFields.minCountByRoomType.set(node, minCount);
            }
            if (maxCount == null) {
                maxCount = new HashMap<>();
                MapRoomNodeFields.maxCountByRoomType.set(node, maxCount);
            }

            minCount.clear();
            maxCount.clear();

            String nodeRoomType = node.room.getMapSymbol();
            iconMap.put(nodeRoomType, node.room.getMapImg());

            Set<String> roomTypes = new HashSet<>();

            for (MapEdge edge : node.getEdges()) {
                if (edge.dstY >= AbstractDungeon.map.size()) {
                    continue;
                }

                MapRoomNode dstNode = AbstractDungeon.map.get(edge.dstY).get(edge.dstX);
                Map<String, Integer> dstMinCount = MapRoomNodeFields.minCountByRoomType.get(dstNode);
                if (dstMinCount == null) {
                    continue;
                }
                for (Map.Entry<String, Integer> entry : dstMinCount.entrySet()) {
                    roomTypes.add(entry.getKey());
                }
            }

            for (MapEdge edge : node.getEdges()) {
                if (edge.dstY >= AbstractDungeon.map.size()) {
                    continue;
                }

                MapRoomNode dstNode = AbstractDungeon.map.get(edge.dstY).get(edge.dstX);
                Map<String, Integer> dstMinCount = MapRoomNodeFields.minCountByRoomType.get(dstNode);
                Map<String, Integer> dstMaxCount = MapRoomNodeFields.maxCountByRoomType.get(dstNode);
                if (dstMinCount == null) {
                    continue;
                }
                for (String roomType : roomTypes) {
                    minCount.put(roomType, Math.min(minCount.getOrDefault(roomType, Integer.MAX_VALUE), dstMinCount.getOrDefault(roomType, 0)));
                    maxCount.put(roomType, Math.max(maxCount.getOrDefault(roomType, Integer.MIN_VALUE), dstMaxCount.getOrDefault(roomType, 0)));
                }

                MapRoomNodeFields.childrenHaveEmeraldKey.set(node,
                                MapRoomNodeFields.childrenHaveEmeraldKey.get(node) ||
                                MapRoomNodeFields.childrenHaveEmeraldKey.get(dstNode) ||
                                (Settings.isFinalActAvailable && dstNode.hasEmeraldKey));
            }

            minCount.put(nodeRoomType, minCount.getOrDefault(nodeRoomType, 0) + 1);
            maxCount.put(nodeRoomType, maxCount.getOrDefault(nodeRoomType, 0) + 1);

            if (node.y == AbstractDungeon.map.size() - 1) {
                return;
            }

            if (MapRoomNodeFields.summaryEdge.get(node) == null) {
                Vector2f offset = new Vector2f(MathUtils.random(-JITTER_X, JITTER_X), MathUtils.random(-JITTER_Y, JITTER_Y) - 32f * Settings.scale);
                MapRoomNodeFields.summaryOffset.set(node, offset);
                MapRoomNodeFields.summaryEdge.set(node, new MapEdge(
                        node.x, node.y,
                        node.offsetX, node.offsetY,
                        node.x, node.y + 1,
                        offset.x, offset.y,
                        false));
            }
        }
    }

    @SpirePatch(clz = MapGenerator.class, method = "toString", paramtypez = { ArrayList.class, Boolean.class })
    public static class MapGeneratorToStringPatch {
        @SpirePrefixPatch
        public static SpireReturn<String> Prefix() {
            if (!hasZMapRelic()) {
                return SpireReturn.Continue();
            }

            return SpireReturn.Return("[Redacted because of Z Map]");
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "setCurrMapNode")
    public static class AbstractDungeonSetCurrMapNodePatch {
        @SpirePostfixPatch
        public static void Postfix() {
            isBeforeFirstRoom = false;
        }
    }

    public static boolean shouldShowEdge(MapRoomNode node) {
        if (!hasZMapRelic()) {
            return true;
        }

        MapRoomNode currentMapNode = AbstractDungeon.getCurrMapNode();
        return (currentMapNode == null || highestVisibleRoomY(currentMapNode) > node.y) &&
                !isSelectingFirstRoom();
    }

    private static int highestVisibleRoomY(MapRoomNode currentMapNode) {
        AbstractRoom currentRoom = currentMapNode.room;
        return Math.max(0, currentMapNode.y + (currentRoom != null && currentRoom.phase == AbstractRoom.RoomPhase.COMPLETE ? 1 : 0));
    }

    private static boolean hasZMapRelic() {
        if (ModHelper.isModEnabled(SummarizedMap.ID)) {
            return true;
        }

        AbstractPlayer player = AbstractDungeon.player;
        return player != null && player.hasRelic(ZMap.ID);
    }

    private static boolean isSelectingFirstRoom() {
        if (!AbstractDungeon.firstRoomChosen) {
            isBeforeFirstRoom = true;
        }
        return isBeforeFirstRoom;
    }
}
