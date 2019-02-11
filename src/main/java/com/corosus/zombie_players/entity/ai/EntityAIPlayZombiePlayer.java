package com.corosus.zombie_players.entity.ai;

import java.util.List;

import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

public class EntityAIPlayZombiePlayer extends EntityAIBase
{
    private final EntityZombiePlayer zombiePlayer;
    private EntityLivingBase target;
    private final double speed;
    private int playTime;

    public EntityAIPlayZombiePlayer(EntityZombiePlayer villagerIn, double speedIn)
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
        if (this.zombiePlayer.getRNG().nextInt(200) != 0)
        {
            return false;
        }
        else
        {
            List<EntityZombiePlayer> list = this.zombiePlayer.world.<EntityZombiePlayer>getEntitiesWithinAABB(EntityZombiePlayer.class, this.zombiePlayer.getEntityBoundingBox().grow(6.0D, 3.0D, 6.0D));
            double d0 = Double.MAX_VALUE;

            for (EntityZombiePlayer entityvillager : list)
            {
                if (entityvillager != this.zombiePlayer && !entityvillager.isPlaying())
                {
                    double d1 = entityvillager.getDistanceSqToEntity(this.zombiePlayer);

                    if (d1 <= d0)
                    {
                        d0 = d1;
                        this.target = entityvillager;
                    }
                }
            }

            if (this.target == null)
            {
                Vec3d vec3d = RandomPositionGenerator.findRandomTarget(this.zombiePlayer, 16, 3);

                if (vec3d == null)
                {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting()
    {
        return this.playTime > 0;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        if (this.target != null)
        {
            this.zombiePlayer.setPlaying(true);
        }

        this.playTime = 1000;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void resetTask()
    {
        this.zombiePlayer.setPlaying(false);
        this.target = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask()
    {
        --this.playTime;

        if (this.playTime % 20 == 0 && zombiePlayer.onGround) {
            if (zombiePlayer.world.rand.nextInt(3) == 0) {
                zombiePlayer.getJumpHelper().setJumping();
            }
        }

        if (this.target != null)
        {
            if (this.zombiePlayer.getDistanceSqToEntity(this.target) > 4.0D)
            {
                this.zombiePlayer.getNavigator().tryMoveToEntityLiving(this.target, this.speed);
            }
        }
        else if (this.zombiePlayer.getNavigator().noPath())
        {
            Vec3d vec3d = RandomPositionGenerator.findRandomTarget(this.zombiePlayer, 16, 3);

            if (vec3d == null)
            {
                return;
            }

            this.zombiePlayer.getNavigator().tryMoveToXYZ(vec3d.x, vec3d.y, vec3d.z, this.speed);
        }
    }
}