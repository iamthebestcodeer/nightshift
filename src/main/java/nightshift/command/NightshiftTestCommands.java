package nightshift.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import nightshift.effect.NightshiftEffects;
import nightshift.encounter.EncounterDirector;
import nightshift.encounter.EncounterRules;
import nightshift.entity.Understudy;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Operator-only, explicit test actions; ordinary encounters use their normal rules. */
public final class NightshiftTestCommands {
    private NightshiftTestCommands() {}

    /** Registers the operator-only encounter and scene test commands under {@code /nightshift test}. */
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                Commands.literal("nightshift")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("test")
                                .executes(context -> reply(context.getSource(), "Test commands: watch, tunnel, bridge, wall, replica, effects, clear."))
                                .then(Commands.literal("watch").executes(context -> watch(context.getSource())))
                                .then(Commands.literal("effects").executes(context -> effects(context.getSource())))
                                .then(SceneTestCommands.tunnel())
                                .then(SceneTestCommands.construction("bridge", registry))
                                .then(SceneTestCommands.construction("wall", registry))
                                .then(SceneTestCommands.replica())
                                .then(Commands.literal("clear").executes(context -> clear(context.getSource()))))));
    }

    /** Replaces the living command player's nearby sighting with one ahead; returns zero if placement fails. */
    private static int watch(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isAlive() || player.isSpectator()) {
            source.sendFailure(Component.literal("Use a living player outside spectator mode."));
            return 0;
        }
        removeSightings(player);
        if (!EncounterDirector.startTestEncounter(player)) {
            source.sendFailure(Component.literal("No safe spot ahead. Try level outdoor terrain with loaded chunks."));
            return 0;
        }
        return reply(source, "Sighting ahead. It moves while unseen and leaves when approached or after 30 seconds.");
    }

    /** Replaces the command player's Hollow and Seen effects with fresh, unstacked durations. */
    private static int effects(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.removeEffect(NightshiftEffects.HOLLOW);
        player.removeEffect(NightshiftEffects.SEEN);
        player.addEffect(new MobEffectInstance(NightshiftEffects.HOLLOW, EncounterRules.HOLLOW_DURATION, 0, false, false, true));
        player.addEffect(new MobEffectInstance(NightshiftEffects.SEEN, EncounterRules.SEEN_DURATION, 0, false, false, true));
        return reply(source, "Hollow: 30 seconds. Seen: 90 seconds.");
    }

    /** Removes the command player's nearby encounters and effects while retaining constructed blocks. */
    private static int clear(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        removeSightings(player);
        player.level().getEntitiesOfClass(nightshift.entity.Apparition.class, player.getBoundingBox().inflate(128),
                ghost -> ghost.belongsTo(player.getUUID())).forEach(nightshift.entity.Apparition::discard);
        player.removeEffect(NightshiftEffects.HOLLOW);
        player.removeEffect(NightshiftEffects.SEEN);
        return reply(source, "Your nearby encounters, apparitions, Hollow, and Seen cleared. Construction blocks remain.");
    }

    /** Discards this player's owned Understudies within the search box extending 96 blocks around them. */
    private static void removeSightings(ServerPlayer player) {
        player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96),
                entity -> entity.isWatching(player.getUUID())).forEach(Understudy::discard);
    }

    /** Sends a success message to the command source without broadcasting and returns one. */
    private static int reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
