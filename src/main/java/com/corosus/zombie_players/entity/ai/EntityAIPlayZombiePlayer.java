package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CU;
import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class EntityAIPlayZombiePlayer extends Goal
{
    private final ZombiePlayer zombiePlayer;
    private LivingEntity target;
    private final double speed;
    private int playTime;

    public EntityAIPlayZombiePlayer(ZombiePlayer villagerIn, double speedIn)
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
        if (!zombiePlayer.isCalm() || zombiePlayer.shouldFollowOwner) return false;
        if (CU.rand().nextInt(200) != 0)
        {
            return false;
        } else if (!zombiePlayer.canPlay()) {
            return false;
        } else {
            List<ZombiePlayer> list = this.zombiePlayer.level.getEntitiesOfClass(ZombiePlayer.class, this.zombiePlayer.getBoundingBox().inflate(12.0D, 3.0D, 12.0D));
            double d0 = Double.MAX_VALUE;

            for (ZombiePlayer entityvillager : list)
            {
                if (entityvillager != this.zombiePlayer && !entityvillager.isPlaying())
                {
                    double d1 = entityvillager.distanceToSqr(this.zombiePlayer);

                    if (d1 <= d0)
                    {
                        d0 = d1;
                        this.target = entityvillager;
                    }
                }
            }

            if (this.target == null)
            {
                /*Vec3 vec3d = RandomPositionGenerator.findRandomTarget(this.zombiePlayer, 16, 3);

                if (vec3d == null)
                {
                    return false;
                }*/

                //only play with other zombies
                return false;
            }

            return true;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse()
    {
        return this.playTime > 0;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start()
    {
        if (this.target != null)
        {
            this.zombiePlayer.setPlaying(true);
        }

        zombiePlayer.markStartPlaying();

        this.playTime = 1000;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop()
    {
        this.zombiePlayer.setPlaying(false);
        this.target = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick()
    {
        --this.playTime;

        if (this.playTime % 20 == 0 && zombiePlayer.isOnGround()) {
            if (CU.rand().nextInt(3) == 0) {
                zombiePlayer.getJumpControl().jump();
            }
        }

        if (this.target != null)
        {
            if (this.zombiePlayer.distanceToSqr(this.target) > 4.0D)
            {
                this.zombiePlayer.getNavigation().moveTo(this.target, this.speed);
            }
        }
        else if (this.zombiePlayer.getNavigation().isDone())
        {
            Vec3 vec3d = DefaultRandomPos.getPos(this.zombiePlayer, 16, 3);

            if (vec3d == null)
            {
                return;
            }

            this.zombiePlayer.getNavigation().moveTo(vec3d.x, vec3d.y, vec3d.z, this.speed);
        }
    }
}