package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayerNew;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class EntityAIMoveTowardsRestrictionZombie extends Goal
{
    private final ZombiePlayerNew creature;
    private double movePosX;
    private double movePosY;
    private double movePosZ;
    private final double movementSpeed;

    public EntityAIMoveTowardsRestrictionZombie(ZombiePlayerNew creatureIn, double speedIn)
    {
        this.creature = creatureIn;
        this.movementSpeed = speedIn;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse()
    {
        if (creature.shouldFollowOwner) return false;
        if (this.creature.isWithinRestriction())
        {
            return false;
        }
        else
        {
            BlockPos blockpos = this.creature.getRestrictCenter();
            Vec3 vec3d = DefaultRandomPos.getPosTowards(this.creature, 16, 7, new Vec3((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ()), ((float)Math.PI / 2F));

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
    @Override
    public boolean canContinueToUse()
    {
        return !this.creature.getNavigation().isDone();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start()
    {
        this.creature.getNavigation().moveTo(this.movePosX, this.movePosY, this.movePosZ, this.movementSpeed);
    }
}