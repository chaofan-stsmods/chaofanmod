package io.chaofan.sts.chaofanmod.commands;

import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import com.codedisaster.steamworks.SteamID;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.actions.common.SelectFromGridAction;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FriendCardCommand extends ConsoleCommand {
    public FriendCardCommand() {
        maxExtraTokens = 1;
        minExtraTokens = 0;
        requiresPlayer = true;
        simpleCheck = true;
    }

    @Override
    protected ArrayList<String> extraOptions(String[] tokens, int depth) {
        ArrayList<String> result = new ArrayList<>();
        result.add("random");
        return result;
    }

    @Override
    protected void execute(String[] args, int depth) {
        if (args.length > depth && args[depth].equals("random")) {
            AbstractDungeon.actionManager.addToBottom(new SelectFromGridAction(
                    IntStream.range(0, 100).mapToObj(f -> {
                        FriendCard friendCard = new FriendCard(true);
                        FriendCard friendCardUpgrade = (FriendCard) friendCard.makeCopy();
                        friendCardUpgrade.upgrade();
                        friendCardUpgrade.displayUpgrades();
                        return Stream.of(friendCard, friendCardUpgrade);
                    }).flatMap(f -> f).sorted(Comparator.comparingInt(c -> c.cost)).collect(Collectors.toList()),
                    (s, c) -> AbstractDungeon.actionManager.addToBottom(new MakeTempCardInHandAction(c[0])),
                    "Select a card",
                    AbstractGameAction.ActionType.DRAW,
                    1,
                    false));
            return;
        }

        List<SteamID> friends = ChaofanMod.steamworksHelper.getFriends();
        if (friends.isEmpty()) {
            DevConsole.log("You don't have a friend.");
            return;
        }

        AbstractDungeon.actionManager.addToBottom(new SelectFromGridAction(
                friends.stream().flatMap(f -> {
                    FriendCard friendCard = new FriendCard(f);
                    friendCard.upgrade();
                    friendCard.displayUpgrades();
                    return Stream.of(new FriendCard(f), friendCard);
                }).sorted(Comparator.comparingInt(c -> c.cost)).collect(Collectors.toList()),
                (s, c) -> AbstractDungeon.actionManager.addToBottom(new MakeTempCardInHandAction(c[0])),
                "Select a card",
                AbstractGameAction.ActionType.DRAW,
                1,
                false));
    }
}
