package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.codedisaster.steamworks.SteamID;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import java.util.Comparator;
import java.util.List;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class BlessOfNetwork extends CustomRelic {
    public static final String ID = makeId("relic.BlessOfNetwork");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/bless_of_network.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/bless_of_network.png"));

    public BlessOfNetwork() {
        super(ID, IMG, OUTLINE, RelicTier.RARE, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new MakeTempCardInHandAction(makeFriendCard()));
    }

    private FriendCard makeFriendCard() {
        List<SteamID> friends = ChaofanMod.steamworksHelper.getFriends();
        if (friends.isEmpty()) {
            AbstractDungeon.effectList.add(new ThoughtBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, 3.0F, DESCRIPTIONS[1], true));
            return new FriendCard(true);
        }

        friends.sort(Comparator.comparing(SteamID::getNativeHandle));
        SteamID friend = friends.get(AbstractDungeon.cardRandomRng.random(friends.size() - 1));
        return new FriendCard(friend);
    }
}
