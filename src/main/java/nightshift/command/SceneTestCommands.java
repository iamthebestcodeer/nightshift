package nightshift.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import nightshift.encounter.EnvironmentalScenes;
import nightshift.encounter.SceneRules;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

final class SceneTestCommands {
    private SceneTestCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> construction(String name, CommandBuildContext registry) {
        return Commands.literal(name).then(Commands.argument("from", BlockPosArgument.blockPos())
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .then(Commands.argument("block", BlockStateArgument.block(registry)).executes(context -> build(context, name)))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> tunnel() {
        return Commands.literal("tunnel").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            try {
                EnvironmentalScenes.tunnel(player);
                return success(context.getSource(), "Quiet encounter behind you. Turn around; it leaves within 2 blocks or after 30 seconds.");
            } catch (IllegalArgumentException exception) { return failure(context.getSource(), exception); }
        });
    }

    static LiteralArgumentBuilder<CommandSourceStack> replica() {
        return Commands.literal("replica").then(Commands.argument("from", BlockPosArgument.blockPos())
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .then(Commands.argument("destination", BlockPosArgument.blockPos()).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            try {
                                EnvironmentalScenes.replica(player, BlockPosArgument.getLoadedBlockPos(context, "from"),
                                        BlockPosArgument.getLoadedBlockPos(context, "to"), BlockPosArgument.getLoadedBlockPos(context, "destination"));
                                return success(context.getSource(), "Temporary apparition created. No source or destination blocks changed.");
                            } catch (IllegalArgumentException exception) { return failure(context.getSource(), exception); }
                        }))));
    }

    private static int build(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        try {
            var from = EnvironmentalScenes.cell(BlockPosArgument.getLoadedBlockPos(context, "from"));
            var to = EnvironmentalScenes.cell(BlockPosArgument.getLoadedBlockPos(context, "to"));
            var cells = name.equals("bridge") ? SceneRules.bridge(from, to) : SceneRules.wall(from, to);
            EnvironmentalScenes.construct(player, cells, BlockStateArgument.getBlock(context, "block").getState());
            return success(context.getSource(), "Passive " + name + " scene started. It fills empty cells only; placed blocks remain.");
        } catch (IllegalArgumentException exception) { return failure(context.getSource(), exception); }
    }

    private static int success(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int failure(CommandSourceStack source, IllegalArgumentException exception) {
        source.sendFailure(Component.literal(exception.getMessage()));
        return 0;
    }
}
