package io.chaofan.sts.chaofanmod.commands;

import basemod.devcommands.ConsoleCommand;

public class ChaofanModCommand extends ConsoleCommand {
    public ChaofanModCommand() {
        this.followup.put("friendcard", FriendCardCommand.class);
        this.followup.put("shader", ChaofanModEffectCommand.class);
        this.followup.put("thirdpersonview", ThirdPersonCommand.class);
        this.requiresPlayer = false;
        this.simpleCheck = true;
    }

    @Override
    protected void execute(String[] strings, int i) {
    }
}
