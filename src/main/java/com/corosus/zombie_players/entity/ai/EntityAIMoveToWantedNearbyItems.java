package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class EntityAIMoveToWantedNearbyItems extends EntityAIBase
{
    private final EntityZombiePlayer zombiePlayer;
    private EntityItem target;
    private final double speed;
    private int pathTimeout;

    public EntityAIMoveToWantedNearbyItems(EntityZombiePlayer villagerIn, double speedIn)
    {
        this.zombiePlayer = villagerIn;
        this.speed = speedIn;
        this.setMutexBits(1);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        //let more hungry grab faster
        if (zombiePlayer.getCalmTime() > 20 * 90 && this.zombiePlayer.getRNG().nextInt(10) != 0/* && zombiePlayer.world.getTotalWorldTime() % 20 != 0*/) {
            return false;
        } else if (this.zombiePlayer.getRNG().nextInt(5) != 0)
        {
            return false;
        }
        else
        {
            double range = ConfigZombiePlayersAdvanced.calmItemSearchRange;
            List<EntityItem> list = this.zombiePlayer.world.getEntitiesWithinAABB(EntityItem.class, this.zombiePlayer.getEntityBoundingBox().grow(range, range/2D, range));
            double d0 = Double.MAX_VALUE;

            for (EntityItem entity : list)
            {
                if (!entity.getItem().isEmpty() && !entity.cannotPickup() && zombiePlayer.isRawMeat(entity.getItem()))
                {
                    double d1 = entity.getDistanceSqToEntity(this.zombiePlayer);

                    if (d1 <= d0 && zombiePlayer.canEntityBeSeen(entity))
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
    public boolean shouldContinueExecuting()
    {
        return this.pathTimeout > 0 && (target != null && !target.isDead);
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        if (this.target != null)
        {
            zombiePlayer.getNavigator().tryMoveToEntityLiving(target, speed);
        }

        this.pathTimeout = 80;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void resetTask()
    {
        this.target = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask()
    {
        --this.pathTimeout;
    }
}