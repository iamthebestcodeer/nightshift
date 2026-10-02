package nightshift.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nightshift.encounter.WorldPhase;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import nightshift.world.NightshiftSavedData;

/** Operator-only tools; hidden state is never shown in the gameplay HUD. */
public final class NightshiftCommands {
    private NightshiftCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nightshift")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(angerCommand())
                .then(phaseCommand())
                .then(Commands.literal("spawn")
                        .executes(context -> spawn(context.getSource(), context.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> spawn(context.getSource(), Vec3Argument.getVec3(context, "pos"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> angerCommand() {
        return Commands.literal("anger")
                .executes(context -> inspectAnger(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("get")
                        .executes(context -> inspectAnger(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspectAnger(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> setAnger(context.getSource(), context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "value")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setAnger(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "value"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> phaseCommand() {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        for (WorldPhase phase : WorldPhase.values()) {
            set.then(Commands.literal(phase.id()).executes(context -> {
                NightshiftSavedData.get(context.getSource().getServer()).setPhase(phase);
                context.getSource().sendSuccess(() -> Component.literal("World phase: " + phase.id()), false);
                return 1;
            }));
        }
        return Commands.literal("phase").executes(context -> {
            WorldPhase phase = NightshiftSavedData.get(context.getSource().getServer()).phase();
            context.getSource().sendSuccess(() -> Component.literal("World phase: " + phase.id()), false);
            return 1;
        }).then(set);
    }

    private static int inspectAnger(CommandSourceStack source, ServerPlayer player) {
        int anger = NightshiftSavedData.get(source.getServer()).playerState(player.getUUID()).anger();
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " anger: " + anger), false);
        // Queries return the value for execute store result; success is reported separately.
        return anger;
    }

    private static int setAnger(CommandSourceStack source, ServerPlayer player, int value) {
        NightshiftSavedData.get(source.getServer()).setAnger(player.getUUID(), value);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " anger: " + value), false);
        return 1;
    }

    private static int spawn(CommandSourceStack source, Vec3 position) {
        if (!Level.isInSpawnableBounds(BlockPos.containing(position))) {
            source.sendFailure(Component.translatable("commands.summon.invalidPosition"));
            return 0;
        }
        Understudy entity = NightshiftEntities.UNDERSTUDY.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (entity == null) {
            source.sendFailure(Component.translatable("commands.summon.failed"));
            return 0;
        }
        entity.snapTo(position.x, position.y, position.z, source.getRotation().y, 0);
        entity.setYBodyRot(source.getRotation().y);
        entity.setYHeadRot(source.getRotation().y);
        if (!source.getLevel().addFreshEntity(entity)) {
            source.sendFailure(Component.translatable("commands.summon.failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned The Understudy"), false);
        return 1;
    }
}
