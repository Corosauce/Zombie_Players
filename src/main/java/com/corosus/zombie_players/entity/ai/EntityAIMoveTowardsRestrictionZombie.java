package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EntityAIMoveTowardsRestrictionZombie extends EntityAIBase
{
    private final EntityZombiePlayer creature;
    private double movePosX;
    private double movePosY;
    private double movePosZ;
    private final double movementSpeed;

    public EntityAIMoveTowardsRestrictionZombie(EntityZombiePlayer creatureIn, double speedIn)
    {
        this.creature = creatureIn;
        this.movementSpeed = speedIn;
        this.setMutexBits(1);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        if (creature.shouldFollowOwner) return false;
        if (this.creature.isWithinHomeDistanceCurrentPosition())
        {
            return false;
        }
        else
        {
            BlockPos blockpos = this.creature.getHomePosition();
            Vec3d vec3d = RandomPositionGenerator.findRandomTargetBlockTowards(this.creature, 16, 7, new Vec3d((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ()));

            if (vec3d == null)
            {
                return false;
            }
            else
            {
                this.movePosX = vec3d.x;
                this.movePosY = vec3d.y;
                this.movePosZ = vec3d.z;
                return true;
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting()
    {
        return !this.creature.getNavigator().noPath();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.creature.getNavigator().tryMoveToXYZ(this.movePosX, this.movePosY, this.movePosZ, this.movementSpeed);
    }
}