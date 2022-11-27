package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.google.common.base.Predicates;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class EntityAIAvoidEntityOnLowHealth<T extends Entity> extends Goal
{
    private final Predicate<Entity> canBeSeenSelector;
    /** The entity we are attached to */
    protected ZombiePlayer theEntity;
    private final double farSpeed;
    private final double nearSpeed;
    protected T closestLivingEntity;
    private final float avoidDistance;
    /** The PathEntity of our entity */
    private Path entityPathEntity;
    /** The PathNavigate of our entity */
    private final PathNavigation entityPathNavigate;
    /** Class of entity this behavior seeks to avoid */
    private final Class<T> classToAvoid;
    private final Predicate <? super T > avoidTargetSelector;
    private float healthToAvoid = 0F;

    public EntityAIAvoidEntityOnLowHealth(ZombiePlayer theEntityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, float healthToAvoid)
    {
        this(theEntityIn, classToAvoidIn, Predicates.<T>alwaysTrue(), avoidDistanceIn, farSpeedIn, nearSpeedIn, healthToAvoid);
    }

    public EntityAIAvoidEntityOnLowHealth(ZombiePlayer theEntityIn, Class<T> classToAvoidIn, Predicate <? super T > avoidTargetSelectorIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, float healthToAvoid)
    {
        this.canBeSeenSelector = new Predicate<Entity>()
        {
            @Override
            public boolean test(@Nullable Entity p_apply_1_)
            {
                return p_apply_1_.isAlive() && EntityAIAvoidEntityOnLowHealth.this.theEntity.getSensing().hasLineOfSight(p_apply_1_);
            }
        };
        this.theEntity = theEntityIn;
        this.classToAvoid = classToAvoidIn;
        this.avoidTargetSelector = avoidTargetSelectorIn;
        this.avoidDistance = avoidDistanceIn;
        this.farSpeed = farSpeedIn;
        this.nearSpeed = nearSpeedIn;
        this.entityPathNavigate = theEntityIn.getNavigation();
        //this.setMutexBits(1);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.healthToAvoid = healthToAvoid;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse()
    {
        if (!this.theEntity.isCalm()) return false;
        if (this.theEntity.getHealth() > healthToAvoid) return false;

        if (theEntity instanceof ZombiePlayer && EntityAIFollowOwnerZombie.needsToTeleportToOwner(theEntity)) {
            return false;
        }

        List<T> list = this.theEntity.level.getEntitiesOfClass(this.classToAvoid,
                this.theEntity.getBoundingBox().expandTowards((double)this.avoidDistance, 3.0D, (double)this.avoidDistance),
                ZombiePlayer.ENEMY_PREDICATE);

        if (list.isEmpty())
        {
            return false;
        }
        else
        {
            this.closestLivingEntity = list.get(0);
            //hack home distance limit
            int prevDist = (int) theEntity.getRestrictRadius();
            theEntity.restrictTo(theEntity.getRestrictCenter(), -1);
            Vec3 vec3d = DefaultRandomPos.getPosAway(this.theEntity, 16, 7, new Vec3(this.closestLivingEntity.getX(), this.closestLivingEntity.getY(), this.closestLivingEntity.getZ()));
            theEntity.restrictTo(theEntity.getRestrictCenter(), prevDist);

            if (vec3d == null)
            {
                return false;
            }
            else if (this.closestLivingEntity.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < this.closestLivingEntity.distanceToSqr(this.theEntity))
            {
                return false;
            }
            else
            {
                int reachRange = 0;
                this.entityPathEntity = this.entityPathNavigate.createPath(vec3d.x, vec3d.y, vec3d.z, reachRange);
                return this.entityPathEntity != null;
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse()
    {
        return !this.entityPathNavigate.isDone();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start()
    {
        this.entityPathNavigate.moveTo(this.entityPathEntity, this.farSpeed);
        this.theEntity.setTarget(null);
        //this.theEntity.setRevengeTarget(null);
    }

    /**
     * Resets the task
     */
    @Override
    public void stop()
    {
        this.closestLivingEntity = null;
    }

    /**
     * Updates the task
     */
    @Override
    public void tick()
    {
        if (this.theEntity.distanceToSqr(this.closestLivingEntity) < 49.0D)
        {
            this.theEntity.getNavigation().setSpeedModifier(this.nearSpeed);
        }
        else
        {
            this.theEntity.getNavigation().setSpeedModifier(this.farSpeed);
        }
    }
}