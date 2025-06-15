package io.chaofan.sts.chaofanmod.commands;

import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import io.chaofan.sts.chaofanmod.patches.ThirdPerspectiveViewPatches;

public class ThirdPersonCommand extends ConsoleCommand {
    private static boolean enabled = false;

    public ThirdPersonCommand() {
        minExtraTokens = 0;
        maxExtraTokens = 0;
        simpleCheck = true;
    }

    @Override
    protected void execute(String[] strings, int i) {
        enabled = !enabled;
        ThirdPerspectiveViewPatches.setEnable(enabled, "Command");
        DevConsole.log("Third-person view has been " + (enabled ? "enabled" : "disabled") + ".");
    }
}
