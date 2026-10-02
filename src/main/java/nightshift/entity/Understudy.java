package nightshift.entity;

import java.util.UUID;
import nightshift.effect.NightshiftEffects;
import nightshift.encounter.EncounterRules;
import nightshift.world.EncounterSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Understudy extends Mob {
    private UUID watchedPlayer;
    private long expiresAt;
    private boolean marked;
    private boolean stationaryScene;
    private double vanishDistanceSquared = 64;
    private nightshift.encounter.PassiveConstruction construction;

    public Understudy(EntityType<? extends Understudy> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setSilent(true);
    }

    public void watch(ServerPlayer player) {
        watchedPlayer = player.getUUID();
        expiresAt = player.level().getServer().overworld().getGameTime() + EncounterRules.LIFETIME;
    }

    public void quietScene(ServerPlayer player, double vanishDistance) {
        watch(player);
        stationaryScene = true;
        vanishDistanceSquared = vanishDistance * vanishDistance;
    }

    public void buildScene(ServerPlayer player, nightshift.encounter.PassiveConstruction plan) {
        quietScene(player, 2);
        construction = plan;
    }

    public boolean isWatching(UUID player) { return player.equals(watchedPlayer); }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }

    @Override public void tick() {
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        ServerPlayer target = watchedPlayer == null ? null : world.getServer().getPlayerList().getPlayer(watchedPlayer);
        if (target == null || target.level() != world || !target.isAlive() || target.isSpectator()) {
            discard();
            return;
        }
        long now = world.getServer().overworld().getGameTime();
        if (now >= expiresAt || distanceToSqr(target) <= vanishDistanceSquared) {
            discard();
            return;
        }
        if (construction != null) {
            construction.tick(world, this);
            if (isRemoved()) return;
        }
        boolean seenByTarget = visibleTo(target);
        if (tickCount % 10 == 0) snapHead(target);
        if (seenByTarget && !marked) {
            marked = true;
            target.addEffect(new MobEffectInstance(NightshiftEffects.HOLLOW, EncounterRules.HOLLOW_DURATION, 0, false, false, true));
            target.addEffect(new MobEffectInstance(NightshiftEffects.SEEN, EncounterRules.SEEN_DURATION, 0, false, false, true));
            shortenSeenCooldown(world, target);
        }
        if (distanceToSqr(target) <= vanishDistanceSquared || tickCount >= EncounterRules.LIFETIME) {
            discard();
            return;
        }
        // Every potential observer can freeze it, including another player's target.
        boolean watched = seenByTarget;
        if (!watched) {
            for (ServerPlayer observer : world.players()) {
                if (observer != target && !observer.isSpectator() && distanceToSqr(observer) <= 96 * 96 && visibleTo(observer)) {
                    watched = true;
                    break;
                }
            }
        }
        if (!stationaryScene && !watched && distanceToSqr(target) > 12 * 12) moveUnseen(world, target);
    }

    private boolean visibleTo(ServerPlayer player) {
        Vec3 direction = getEyePosition().subtract(player.getEyePosition()).normalize();
        double dot = player.getLookAngle().dot(direction);
        return dot > 0.1 && EncounterRules.inView(dot, player.hasLineOfSight(this));
    }

    private void snapHead(ServerPlayer player) {
        Vec3 direction = player.getEyePosition().subtract(getEyePosition());
        yHeadRot = (float) (Mth.atan2(direction.z, direction.x) * 180 / Math.PI) - 90;
        setXRot((float) (-Mth.atan2(direction.y, direction.horizontalDistance()) * 180 / Math.PI));
    }

    private void moveUnseen(ServerLevel world, ServerPlayer player) {
        Vec3 step = player.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.28);
        var box = getBoundingBox().move(step);
        // No block edits, chunk loads, climbing, or path searches in this milestone.
        if (world.hasChunkAt(blockPosition().offset((int) Math.signum(step.x), 0, (int) Math.signum(step.z)))
                && world.noCollision(this, box) && !world.containsAnyLiquid(box)
                && !world.noCollision(this, box.move(0, -0.1, 0))) {
            setPos(position().add(step));
        }
    }

    private void shortenSeenCooldown(ServerLevel world, ServerPlayer target) {
        var data = world.getServer().getDataStorage().computeIfAbsent(EncounterSavedData.TYPE);
        var deadlines = data.get(target.getUUID());
        if (deadlines == null) return;
        long next = expiresAt - EncounterRules.LIFETIME
                + EncounterRules.encounterDelay(true, target.getRandom().nextDouble());
        data.set(target.getUUID(), Math.min(deadlines.encounter(), next), deadlines.scare());
    }

}
