package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CULog;
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

public class EntityAIStayAtHomePosition extends Goal
{
    private final ZombiePlayer entity;
    Level world;
    private final double followSpeed;
    private final PathNavigation petPathfinder;
    private int timeToRecalcPath;
    private float oldWaterCost;

    public static double TP_RANGE_SQ = 32*32;

    public EntityAIStayAtHomePosition(ZombiePlayer tameableIn, double followSpeedIn)
    {
        this.entity = tameableIn;
        this.world = tameableIn.level;
        this.followSpeed = followSpeedIn;
        this.petPathfinder = tameableIn.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean canUse()
    {
        if (!entity.isCalm() || entity.shouldWander || entity.shouldFollowOwner) return false;

        if (entity.getRestrictCenter() == null || entity.getRestrictCenter().equals(BlockPos.ZERO)) {
            return false;
        }

        if (!isCloseEnough()) {
            return true;
        }

        return false;
    }

    public boolean isCloseEnough() {
        return entity.blockPosition().distSqr(entity.getRestrictCenter()) <= 0.5;
    }

    public boolean isFarEnoughToTeleport() {
        return entity.blockPosition().distSqr(entity.getRestrictCenter()) > TP_RANGE_SQ;
    }

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
        return canUse();
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
        this.petPathfinder.stop();
        this.entity.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick()
    {
        if (!this.entity.isLeashed() && !this.entity.isPassenger()) {
            if (isFarEnoughToTeleport()) {
                this.teleportToHome();
            }
        }

        if (true/*!this.tameable.isSitting()*/)
        {
            if (--this.timeToRecalcPath <= 0)
            {
                this.timeToRecalcPath = 10;

                if (!this.petPathfinder.moveTo(this.entity.getRestrictCenter().getX() + 0.5F, this.entity.getRestrictCenter().getY(), this.entity.getRestrictCenter().getZ() + 0.5F, this.followSpeed))
                {
                    if (!this.entity.isLeashed() && !this.entity.isPassenger())
                    {
                        if (isFarEnoughToTeleport())
                        {
                            int i = Mth.floor(this.entity.getRestrictCenter().getX()) - 2;
                            int j = Mth.floor(this.entity.getRestrictCenter().getZ()) - 2;
                            int k = Mth.floor(this.entity.getRestrictCenter().getY());

                            for (int l = 0; l <= 4; ++l)
                            {
                                for (int i1 = 0; i1 <= 4; ++i1)
                                {
                                    if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.canTeleportTo(new BlockPos(i, j, k)))
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

    private void teleportToHome() {
        BlockPos blockpos = this.entity.getRestrictCenter();

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
        if (!this.canTeleportTo(new BlockPos(p_25304_, p_25305_, p_25306_))) {
            return false;
        } else {
            this.entity.moveTo((double)p_25304_ + 0.5D, (double)p_25305_, (double)p_25306_ + 0.5D, this.entity.getYRot(), this.entity.getXRot());
            entity.getNavigation().stop();
            return true;
        }
    }
}