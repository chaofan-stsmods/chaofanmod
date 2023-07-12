package io.chaofan.sts.chaofanmod.utils;

import basemod.interfaces.PreRenderSubscriber;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.codedisaster.steamworks.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.integrations.steam.SteamIntegration;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SteamworksHelper implements PreRenderSubscriber {

    private final boolean enabled;
    private final SteamFriends steamFriends;
    private final SteamUtils steamUtils;

    private final HashMap<SteamID, Consumer<Texture>> avatarCallbacks = new HashMap<>();
    private final HashMap<SteamID, Texture> avatarCache = new HashMap<>();
    private final HashMap<SteamID, Integer> pendingLoadDone = new HashMap<>();

    public SteamworksHelper() {
        enabled = CardCrawlGame.publisherIntegration instanceof SteamIntegration &&
                CardCrawlGame.publisherIntegration.isInitialized();
        steamFriends = new SteamFriends(new SFCallbacks());
        steamUtils = new SteamUtils(new SUtilsCallbacks());
    }

    public List<SteamID> getFriends() {
        if (!enabled) {
            return new ArrayList<>();
        }

        List<SteamID> friends = new ArrayList<>();
        int count = steamFriends.getFriendCount(SteamFriends.FriendFlags.All);
        for (int i = 0; i < count; i++) {
            friends.add(steamFriends.getFriendByIndex(i, SteamFriends.FriendFlags.All));
        }
        return friends;
    }

    public String getFriendName(SteamID steamID) {
        if (!enabled) {
            return "";
        }

        return steamFriends.getFriendPersonaName(steamID);
    }

    public void getFriendAvatar(SteamID friend, Consumer<Texture> callback) {
        Texture texture = avatarCache.get(friend);
        if (texture != null) {
            callback.accept(texture);
            return;
        }

        avatarCallbacks.put(friend, callback);
        int avatarHandle = steamFriends.getLargeFriendAvatar(friend);
        if (avatarHandle == 0) {
            avatarCallbacks.remove(friend);
            callback.accept(null);
        } else if (avatarHandle != -1) {
            avatarCallbacks.remove(friend);
            Texture texture1 = loadAvatarImage(avatarHandle);
            callback.accept(texture1);
            avatarCache.put(friend, texture1);
        }
    }

    private Texture loadAvatarImage(int avatarHandle) {
        int[] size = new int[2];
        steamUtils.getImageSize(avatarHandle, size);
        int length = size[0] * size[1] * 4;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
        if (!SteamworksAdapter.steamUtilsGetImageRGBA(steamUtils, avatarHandle, byteBuffer, length)) {
            return null;
        }

        return new Texture(new Pixmap(new Gdx2DPixmap(byteBuffer, new long[] { 0, size[0], size[1], Gdx2DPixmap.GDX2D_FORMAT_RGBA8888 })));
    }

    @Override
    public void receiveCameraRender(OrthographicCamera orthographicCamera) {
        synchronized (pendingLoadDone) {
            for (Map.Entry<SteamID, Integer> entry : pendingLoadDone.entrySet()) {
                SteamID steamID = entry.getKey();
                int handle = entry.getValue();

                Consumer<Texture> callback = avatarCallbacks.get(steamID);
                if (callback != null) {
                    avatarCallbacks.remove(steamID);
                    Texture texture = loadAvatarImage(handle);
                    callback.accept(texture);
                    avatarCache.put(steamID, texture);
                }
            }

            pendingLoadDone.clear();
        }
    }

    class SFCallbacks implements SteamFriendsCallback {

        @Override
        public void onSetPersonaNameResponse(boolean b, boolean b1, SteamResult steamResult) {

        }

        @Override
        public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange personaChange) {

        }

        @Override
        public void onGameOverlayActivated(boolean b) {

        }

        @Override
        public void onGameLobbyJoinRequested(SteamID steamID, SteamID steamID1) {

        }

        @Override
        public void onAvatarImageLoaded(SteamID steamID, int handle, int i1, int i2) {
            synchronized (pendingLoadDone) {
                pendingLoadDone.put(steamID, handle);
            }
        }

        @Override
        public void onFriendRichPresenceUpdate(SteamID steamID, int i) {

        }

        @Override
        public void onGameRichPresenceJoinRequested(SteamID steamID, String s) {

        }

        @Override
        public void onGameServerChangeRequested(String s, String s1) {

        }
    }

    class SUtilsCallbacks implements SteamUtilsCallback {

        @Override
        public void onSteamShutdown() {

        }
    }
}
