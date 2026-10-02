package nightshift.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Stationary checkpoint: no goals, encounters, natural spawning, or world edits. */
public final class Understudy extends PathfinderMob {
    public Understudy(EntityType<? extends Understudy> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setPermanentlyInvulnerable(true);
        setSilent(true);
        setNoAi(true);
    }
}
