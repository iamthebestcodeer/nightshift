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

    private static int effects(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.removeEffect(NightshiftEffects.HOLLOW);
        player.removeEffect(NightshiftEffects.SEEN);
        player.addEffect(new MobEffectInstance(NightshiftEffects.HOLLOW, EncounterRules.HOLLOW_DURATION, 0, false, false, true));
        player.addEffect(new MobEffectInstance(NightshiftEffects.SEEN, EncounterRules.SEEN_DURATION, 0, false, false, true));
        return reply(source, "Hollow: 30 seconds. Seen: 90 seconds.");
    }

    private static int clear(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        removeSightings(player);
        player.level().getEntitiesOfClass(nightshift.entity.Apparition.class, player.getBoundingBox().inflate(128),
                ghost -> ghost.belongsTo(player.getUUID())).forEach(nightshift.entity.Apparition::discard);
        player.removeEffect(NightshiftEffects.HOLLOW);
        player.removeEffect(NightshiftEffects.SEEN);
        return reply(source, "Your nearby encounters, apparitions, Hollow, and Seen cleared. Construction blocks remain.");
    }

    private static void removeSightings(ServerPlayer player) {
        player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96),
                entity -> entity.isWatching(player.getUUID())).forEach(Understudy::discard);
    }

    private static int reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
