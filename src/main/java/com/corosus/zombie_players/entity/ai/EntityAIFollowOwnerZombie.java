package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

public class EntityAIFollowOwnerZombie extends Goal
{
    private final ZombiePlayer entity;
    private LivingEntity owner;
    Level world;
    private final double followSpeed;
    private final PathNavigation petPathfinder;
    private int timeToRecalcPath;
    float maxDist;
    float minDist;
    private float oldWaterCost;

    public static double TP_RANGE_SQ = 16 * 16;

    public EntityAIFollowOwnerZombie(ZombiePlayer tameableIn, double followSpeedIn, float minDistIn, float maxDistIn)
    {
        this.entity = tameableIn;
        this.world = tameableIn.level;
        this.followSpeed = followSpeedIn;
        this.petPathfinder = tameableIn.getNavigation();
        this.minDist = minDistIn;
        this.maxDist = maxDistIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean canUse()
    {

        if (!entity.isCalm() || !entity.shouldFollowOwner) return false;

        LivingEntity entitylivingbase = (LivingEntity) this.entity.getOwner();

        if (entitylivingbase == null)
        {
            return false;
        }
        else if (entitylivingbase instanceof Player && ((Player)entitylivingbase).isSpectator())
        {
            return false;
        }
        else if (this.entity.distanceToSqr(entitylivingbase) < (double)(this.minDist * this.minDist))
        {
            return false;
        }
        else
        {
            this.owner = entitylivingbase;
            return true;
        }
    }

    //used elsewhere for AI task checks, do not remove distance check here
    public static boolean needsToTeleportToOwner(ZombiePlayer entity) {
        if (!entity.isCalm() || !entity.shouldFollowOwner) return false;
        LivingEntity entitylivingbase = (LivingEntity) entity.getOwner();
        if (entitylivingbase == null)
        {
            return false;
        } else if (entitylivingbase instanceof Player && ((Player)entitylivingbase).isSpectator()) {
            return false;
        } else if (entity.distanceToSqr(entitylivingbase) < TP_RANGE_SQ) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse()
    {
        return (entity.isCalm() && entity.shouldFollowOwner) && (!this.petPathfinder.isDone() && this.entity.distanceToSqr(this.owner) > (double)(this.maxDist * this.maxDist));
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start()
    {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.entity.getPathfindingMalus(BlockPathTypes.WATER);
        this.entity.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop()
    {
        this.owner = null;
        this.petPathfinder.stop();
        this.entity.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick()
    {

        if (true/*!this.tameable.isSitting()*/)
        {
            if (--this.timeToRecalcPath <= 0)
            {
                this.timeToRecalcPath = 10;

                if (!this.petPathfinder.moveTo(this.owner, this.followSpeed))
                {
                    if (!this.entity.isLeashed() && !this.entity.isPassenger())
                    {
                        if (this.entity.distanceToSqr(this.owner) >= TP_RANGE_SQ)
                        {
                            int i = Mth.floor(this.owner.getX()) - 2;
                            int j = Mth.floor(this.owner.getZ()) - 2;
                            int k = Mth.floor(this.owner.getBoundingBox().minY);

                            for (int l = 0; l <= 4; ++l)
                            {
                                for (int i1 = 0; i1 <= 4; ++i1)
                                {
                                    if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.canTeleportTo(new BlockPos(i, j, k)) && entity.distanceTo(this.owner) > 2)
                                    {
                                        this.entity.moveTo((double)((float)(i + l) + 0.5F), (double)k, (double)((float)(j + i1) + 0.5F), this.entity.getYRot(), this.entity.getXRot());
                                        this.petPathfinder.stop();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canTeleportTo(BlockPos p_25308_) {
        BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(this.world, p_25308_.mutable());
        if (blockpathtypes != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = this.world.getBlockState(p_25308_.below());
            if (blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = p_25308_.subtract(this.entity.blockPosition());
                return this.world.noCollision(this.entity, this.entity.getBoundingBox().move(blockpos));
            }
        }
    }

    private void teleportToOwner() {
        BlockPos blockpos = this.owner.blockPosition();

        for(int i = 0; i < 10; ++i) {
            int j = this.randomIntInclusive(-3, 3);
            int k = this.randomIntInclusive(-1, 1);
            int l = this.randomIntInclusive(-3, 3);
            boolean flag = this.maybeTeleportTo(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
            if (flag) {
                return;
            }
        }

    }

    private int randomIntInclusive(int p_25301_, int p_25302_) {
        return this.entity.getRandom().nextInt(p_25302_ - p_25301_ + 1) + p_25301_;
    }

    private boolean maybeTeleportTo(int p_25304_, int p_25305_, int p_25306_) {
        if (Math.abs((double)p_25304_ - this.owner.getX()) < 2.0D && Math.abs((double)p_25306_ - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(p_25304_, p_25305_, p_25306_))) {
            return false;
        } else {
            this.entity.moveTo((double)p_25304_ + 0.5D, (double)p_25305_, (double)p_25306_ + 0.5D, this.entity.getYRot(), this.entity.getXRot());
            entity.getNavigation().stop();
            return true;
        }
    }
}