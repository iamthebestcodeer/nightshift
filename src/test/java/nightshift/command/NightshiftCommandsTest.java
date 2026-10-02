package nightshift.command;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NightshiftCommandsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void parsesDebugSyntaxAndRejectsInvalidValues() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        CommandSourceStack operator = Commands.createCompilationContext(LevelBasedPermissionSet.GAMEMASTER);
        for (String command : new String[]{"nightshift anger", "nightshift anger get",
                "nightshift anger get Alex", "nightshift anger set 0", "nightshift anger set 31 Alex",
                "nightshift phase", "nightshift phase set siege_nights", "nightshift spawn",
                "nightshift spawn ~ ~ ~", "nightshift spawn 0 -60 4"}) {
            var parsed = dispatcher.parse(command, operator);
            assertFalse(parsed.getReader().canRead(), command);
            assertTrue(parsed.getExceptions().isEmpty(), command);
            assertNotNull(parsed.getContext().getCommand(), command);
        }
        for (String command : new String[]{"nightshift anger set -1", "nightshift anger set 2147483648",
                "nightshift phase set unknown"}) {
            assertTrue(dispatcher.parse(command, operator).getReader().canRead(), command);
        }
    }

    @Test
    void onlyGameMastersAndAboveCanUseDebugCommands() {
        var root = dispatcher().getRoot().getChild("nightshift");
        assertFalse(root.canUse(Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS)));
        assertFalse(root.canUse(Commands.createCompilationContext(LevelBasedPermissionSet.MODERATOR)));
        assertTrue(root.canUse(Commands.createCompilationContext(LevelBasedPermissionSet.GAMEMASTER)));
        assertTrue(root.canUse(Commands.createCompilationContext(LevelBasedPermissionSet.OWNER)));
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        NightshiftCommands.register(dispatcher);
        return dispatcher;
    }
}
