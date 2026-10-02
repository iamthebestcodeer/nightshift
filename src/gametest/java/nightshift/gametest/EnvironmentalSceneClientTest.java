package nightshift.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import nightshift.Nightshift;
import nightshift.entity.Apparition;
import nightshift.entity.Understudy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.block.Blocks;

public final class EnvironmentalSceneClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setAllowCommands(true);
        }).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            context.waitTicks(40);
            verifyConstructionRegressions(context, world);
            world.getServer().runCommand("fill -8 -64 12 8 -61 18 air");
            world.getServer().runCommand("setblock 0 -61 15 iron_block");
            command(world, "bridge -8 -61 15 8 -61 15 stone");
            context.waitTicks(20);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int x = -8; x <= 8; x++) check(level.getBlockState(new BlockPos(x, -61, 15)).is(x == 0 ? Blocks.IRON_BLOCK : Blocks.STONE),
                        "straight bridge completes without replacing existing blocks");
                var actor = actor(server);
                check(actor.getX() == 8.5 && actor.getZ() == 15.5, "builder reaches endpoint and stops");
            });
            context.getInput().lookAt(0, 20);
            context.takeScreenshot("scene-straight-ravine-bridge");
            command(world, "wall -4 -60 10 4 -57 10 stone");
            context.waitTicks(25);
            world.getServer().runOnServer(server -> {
                for (int x = -4; x <= 4; x++) for (int y = -60; y <= -57; y++)
                    check(server.overworld().getBlockState(new BlockPos(x, y, 10)).is(Blocks.STONE), "wall completes marked plane");
                check(server.overworld().getEntitiesOfClass(Understudy.class, server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).size() == 1,
                        "scene replaces prior actor");
            });
            context.getInput().lookAt(0, 0);
            context.takeScreenshot("scene-finished-wall");
            world.getServer().runOnServer(server -> {
                check(execute(server, "wall -4 -60 20 4 -57 20 stone") == 1, "attack fixture starts");
                actor(server).setAggressive(true);
            });
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(server.overworld().getBlockState(new BlockPos(-4, -60, 20)).isAir(), "attack cancels construction before placement");
                check(server.overworld().getEntitiesOfClass(Understudy.class, server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).isEmpty(),
                        "attacking builder retreats");
            });
            command(world, "replica -4 -60 10 4 -57 10 -4 -60 30");
            context.waitTicks(10);
            int ghostId = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var ghost = player.level().getEntitiesOfClass(Apparition.class, player.getBoundingBox().inflate(96)).getFirst();
                check(ghost.blocks().size() == 36, "replica carries all reference shapes");
                check(server.overworld().getBlockState(new BlockPos(0, -60, 30)).isAir(), "replica makes no physical edits");
                return ghost.getId();
            });
            // Move sideways so the real reference wall does not hide its distant copy.
            world.getServer().runCommand("tp @a 12.5 -60 0.5 22 0");
            context.waitTicks(10);
            context.getInput().lookAt(22, 0);
            context.waitFor(client -> client.level.getEntity(ghostId) instanceof Apparition ghost && ghost.blocks().size() == 36, 60);
            // Remove the reference only as a test fixture, to isolate the apparition's actual renderer.
            world.getServer().runCommand("fill -4 -60 10 4 -57 10 air");
            context.waitTicks(10);
            context.runOnClient(client -> check(client.level.getEntity(ghostId).shouldRenderAtSqrDistance(
                    client.level.getEntity(ghostId).distanceToSqr(client.player)), "distant apparition is eligible to render"));
            context.takeScreenshot("scene-distant-apparition");
            context.waitTicks(610);
            world.getServer().runOnServer(server -> check(server.overworld().getEntity(ghostId) == null, "apparition expires after 30 seconds"));
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            world.getServer().runCommand("fill -1 -60 -8 -1 -58 1 stone");
            world.getServer().runCommand("fill 1 -60 -8 1 -58 1 stone");
            world.getServer().runCommand("fill 0 -58 -8 0 -58 1 stone");
            context.waitTicks(10);
            context.getInput().lookAt(0, 0);
            command(world, "tunnel");
            context.waitTicks(10);
            world.getServer().runOnServer(server -> check(actor(server).position().distanceTo(server.getPlayerList().getPlayers().getFirst().position()) >= 4,
                    "tunnel actor waits behind player"));
            context.getInput().lookAt(180, 0);
            context.waitTicks(10);
            context.takeScreenshot("scene-quiet-tunnel-look-back");
            world.getServer().runCommand("tp @a 0.5 -60 -2.5 180 0");
            context.waitTicks(10);
            world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Understudy.class,
                    server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).isEmpty(), "tunnel actor leaves within two blocks"));
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            command(world, "replica -8 -61 15 8 -61 15 -8 -61 30");
            command(world, "replica -8 -61 15 8 -61 15 -8 -61 30");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Apparition.class,
                    server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).size() == 1, "repeat replica replaces previous apparition"));
            world.getServer().runCommand("tp @a -7.5 -60 29.5 0 0");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Apparition.class,
                    server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).isEmpty(), "apparition leaves on owner approach"));
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            command(world, "replica -8 -61 15 8 -61 15 -8 -61 30");
            command(world, "clear");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Apparition.class,
                    server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).isEmpty(), "clear removes apparition"));
            world.getServer().runCommand("fill -40 -61 -40 40 -61 40 water");
            world.getServer().runCommand("fill -40 -60 -40 40 -57 40 air");
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            world.getServer().runOnServer(server -> check(execute(server, "watch") == 0, "sightings reject unsupported ocean surface"));
            Nightshift.LOGGER.info("In-world environmental scenes: bridge, wall, attack cancellation, replica sync/no edits/timeout, tunnel and approach passed");
        }
    }
    private static void verifyConstructionRegressions(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("setblock -5 -60 25 redstone_block");
        world.getServer().runOnServer(server -> {
            for (String material : new String[]{"tnt", "sponge", "wet_sponge", "carved_pumpkin", "jack_o_lantern"}) {
                check(execute(server, "wall -4 -60 25 -4 -60 25 " + material) == 0, "reject callback material: " + material);
            }
            check(server.overworld().getBlockState(new BlockPos(-4, -60, 25)).isAir(), "rejected material makes no edit beside redstone");
        });
        // An owner already inside the retreat radius must prevent even the first edit.
        world.getServer().runCommand("fill 2 -61 0 4 -61 0 air");
        world.getServer().runOnServer(server -> {
            check(execute(server, "bridge 2 -61 0 4 -61 0 stone") == 1, "approach fixture starts");
            actor(server).setPos(2.4, -60, 0.5);
        });
        context.waitTicks(2);
        world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Understudy.class,
                server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).isEmpty(), "approach retreats before construction movement"));
        world.getServer().runOnServer(server -> check(server.overworld().getBlockState(new BlockPos(2, -61, 0)).isAir(), "approach prevents placement"));
        world.getServer().runCommand("setblock -4 -60 24 air");
        command(world, "wall -4 -60 24 -3 -60 24 stone");
        int blocker = world.getServer().computeOnServer(server -> {
            var item = new net.minecraft.world.entity.item.ItemEntity(server.overworld(), -3.5, -59.9, 24.5,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
            item.setNoGravity(true);
            item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            server.overworld().addFreshEntity(item);
            return item.getId();
        });
        context.waitTicks(10);
        world.getServer().runOnServer(server -> {
            check(server.overworld().getBlockState(new BlockPos(-4, -60, 24)).isAir(), "occupied cell is deferred");
            server.overworld().getEntity(blocker).discard();
        });
        context.waitTicks(5);
        world.getServer().runOnServer(server -> check(server.overworld().getBlockState(new BlockPos(-4, -60, 24)).is(Blocks.STONE),
                "temporarily occupied cell is retried"));
        command(world, "clear");
        // The first cell has no body clearance; a later cell must provide the spawn spot.
        world.getServer().runCommand("setblock -4 -59 25 stone");
        command(world, "wall -4 -60 25 4 -60 25 stone");
        world.getServer().runOnServer(server -> check(server.overworld().noCollision(actor(server)), "initial builder has body clearance"));
        context.waitTicks(10);
        world.getServer().runOnServer(server -> {
            check(server.overworld().getBlockState(new BlockPos(-4, -60, 25)).is(Blocks.STONE), "gap below preserved upper block is filled");
            check(server.overworld().getBlockState(new BlockPos(-4, -59, 25)).is(Blocks.STONE), "upper block preserved");
            check(server.overworld().noCollision(actor(server)), "builder never moves into preserved upper block");
            check(execute(server, "wall -4 -60 25 -4 -60 25 stone") == 0, "reject scene without any clear spawn spot");
        });
        // Dynamic obstruction after spawning tests movement rather than only spawn validation.
        command(world, "wall -4 -60 28 4 -60 28 stone");
        world.getServer().runOnServer(server -> server.overworld().setBlock(new BlockPos(-3, -59, 28), Blocks.STONE.defaultBlockState(), 3));
        for (int tick = 0; tick < 10; tick++) {
            context.waitTicks(1);
            world.getServer().runOnServer(server -> check(server.overworld().noCollision(actor(server)), "construction tick retains body clearance"));
        }
        command(world, "clear");
        world.getServer().runCommand("fill -20 -60 35 19 -60 35 stone");
        command(world, "replica -20 -60 35 19 -60 35 -20 -60 45");
        world.getServer().runOnServer(server -> check(server.overworld().getEntitiesOfClass(Apparition.class,
                server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(96)).getFirst().blocks().size() == 40,
                "40-cell reference accepted independently of wall width"));
        command(world, "clear");
        world.getServer().runCommand("fill 0 -60 2 31 -60 33 air");
        world.getServer().runCommand("setblock 31 -60 33 stone");
        context.runOnClient(client -> { client.options.renderDistance().set(10); client.options.broadcastOptions(); });
        world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(10));
        context.waitTicks(40);
        command(world, "replica 0 -60 2 31 -60 33 -95 -60 -95");
        int sparseId = world.getServer().computeOnServer(server -> {
            var ghost = server.overworld().getEntitiesOfClass(Apparition.class,
                    server.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(192)).getFirst();
            check(ghost.distanceToSqr(server.getPlayerList().getPlayers().getFirst()) <= 96 * 96, "sparse replica anchored to visible geometry");
            return ghost.getId();
        });
        context.waitFor(client -> client.level.getEntity(sparseId) instanceof Apparition, 300);
        context.runOnClient(client -> check(client.level.getEntity(sparseId).shouldRenderAtSqrDistance(
                client.level.getEntity(sparseId).distanceToSqr(client.player)), "sparse replica is eligible to render"));
        command(world, "clear");
        Nightshift.LOGGER.info("Construction regressions: destructive materials rejected, spawn/movement clearance and long replica passed");
    }

    private static Understudy actor(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        return player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96)).getFirst();
    }
    private static void command(TestSingleplayerContext world, String action) {
        world.getServer().runOnServer(server -> check(execute(server, action) == 1, "command succeeds: " + action));
    }
    private static int execute(MinecraftServer server, String action) {
        try {
            return server.getCommands().getDispatcher().execute("nightshift test " + action,
                    server.getPlayerList().getPlayers().getFirst().createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER));
        } catch (CommandSyntaxException exception) { throw new AssertionError(exception); }
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
