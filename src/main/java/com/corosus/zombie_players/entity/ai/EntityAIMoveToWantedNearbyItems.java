package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CU;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;
import java.util.List;

public class EntityAIMoveToWantedNearbyItems extends Goal
{
    private final ZombiePlayer zombiePlayer;
    private ItemEntity target;
    private final double speed;
    private int pathTimeout;

    public EntityAIMoveToWantedNearbyItems(ZombiePlayer villagerIn, double speedIn)
    {
        this.zombiePlayer = villagerIn;
        this.speed = speedIn;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean canUse()
    {

        if (ConfigZombiePlayersAdvanced.onlySeekFoodIfNeeded && !zombiePlayer.isFoodNeedUrgent()) {
            return false;
        }

        //let more hungry or hurt grab faster
        if (!zombiePlayer.isFoodNeedUrgent() &&
                CU.rand().nextInt(10) != 0/* && zombiePlayer.world.getTotalWorldTime() % 20 != 0*/) {
            return false;
        } else if (CU.rand().nextInt(5) != 0)
        {
            return false;
        }
        else
        {
            double range = ConfigZombiePlayersAdvanced.calmItemSearchRange;
            List<ItemEntity> list = this.zombiePlayer.level.getEntitiesOfClass(ItemEntity.class, this.zombiePlayer.getBoundingBox().inflate(range, range/2D, range));
            double d0 = Double.MAX_VALUE;

            for (ItemEntity entity : list)
            {
                if (!entity.getItem().isEmpty() && !entity.hasPickUpDelay() && zombiePlayer.isCalmingItem(entity.getItem()))
                {
                    double d1 = entity.distanceToSqr(this.zombiePlayer);

                    if (d1 <= d0 && zombiePlayer.hasLineOfSight(entity))
                    {
                        d0 = d1;
                        this.target = entity;
                    }
                }
            }

            return target != null;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse()
    {
        return this.pathTimeout > 0 && (target != null && target.isAlive());
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start()
    {
        if (this.target != null)
        {
            zombiePlayer.setTarget(null);
            zombiePlayer.getNavigation().moveTo(target, speed);
        }

        this.pathTimeout = 80;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop()
    {
        this.target = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick()
    {
        --this.pathTimeout;
    }
}