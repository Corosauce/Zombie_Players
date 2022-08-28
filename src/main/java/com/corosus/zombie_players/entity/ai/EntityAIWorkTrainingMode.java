package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class EntityAIWorkTrainingMode extends Goal
{
    /** The entity using this AI that is tempted by the player. */
    private final ZombiePlayer temptedEntity;
    private final double speed;
    /** X position of player tempting this mob */
    private double targetX;
    /** Y position of player tempting this mob */
    private double targetY;
    /** Z position of player tempting this mob */
    private double targetZ;
    /** Tempting player's pitch */
    private double pitch;
    /** Tempting player's yaw */
    private double yaw;
    /** The player that is tempting the entity that is using this AI. */
    private Player temptingPlayer;
    /**
     * A counter that is decremented each time the canUse method is called. The canUse method will always
     * return false if delayTemptCounter is greater than 0.
     */
    private int delayTemptCounter;
    /** True if this EntityAITempt task is running */
    private boolean isRunning;
    //private final Set<Item> temptItem;
    /** Whether the entity using this AI will be scared by the tempter's sudden movement. */
    private final boolean scaredByPlayerMovement;

    /*public EntityAITemptZombie(EntityCreature temptedEntityIn, double speedIn, Item temptItemIn, boolean scaredByPlayerMovementIn)
    {
        this(temptedEntityIn, speedIn, scaredByPlayerMovementIn, Sets.newHashSet(temptItemIn));
    }*/

    public EntityAIWorkTrainingMode(ZombiePlayer temptedEntityIn, double speedIn, boolean scaredByPlayerMovementIn/*, Set<Item> temptItemIn*/)
    {
        this.temptedEntity = temptedEntityIn;
        this.speed = speedIn;
        //this.temptItem = temptItemIn;
        this.scaredByPlayerMovement = scaredByPlayerMovementIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean canUse()
    {
        if (temptedEntity.getTarget() != null) return false;
        if (this.delayTemptCounter > 0)
        {
            --this.delayTemptCounter;
            return false;
        }
        else
        {
            this.temptingPlayer = this.temptedEntity.level.getNearestPlayer(this.temptedEntity, 18.0D);

            if (this.temptingPlayer == null)
            {
                return false;
            }
            else
            {
                return this.isTempting(this.temptingPlayer.getMainHandItem()) || this.isTempting(this.temptingPlayer.getOffhandItem());
            }
        }
    }

    protected boolean isTempting(ItemStack stack)
    {
        return temptedEntity.getWorkInfo().isInTrainingMode() || temptedEntity.getWorkInfo().isInAreaSetMode();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse()
    {
        if (this.scaredByPlayerMovement)
        {
            if (this.temptedEntity.distanceToSqr(this.temptingPlayer) < 36.0D)
            {
                if (this.temptingPlayer.distanceToSqr(this.targetX, this.targetY, this.targetZ) > 0.010000000000000002D)
                {
                    return false;
                }

                if (Math.abs((double)this.temptingPlayer.getXRot() - this.pitch) > 5.0D || Math.abs((double)this.temptingPlayer.getYRot() - this.yaw) > 5.0D)
                {
                    return false;
                }
            }
            else
            {
                this.targetX = this.temptingPlayer.getX();
                this.targetY = this.temptingPlayer.getY();
                this.targetZ = this.temptingPlayer.getZ();
            }

            this.pitch = (double)this.temptingPlayer.getXRot();
            this.yaw = (double)this.temptingPlayer.getYRot();
        }

        return this.canUse();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start()
    {
        this.targetX = this.temptingPlayer.getX();
        this.targetY = this.temptingPlayer.getY();
        this.targetZ = this.temptingPlayer.getZ();
        this.isRunning = true;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop()
    {
        this.temptingPlayer = null;
        this.temptedEntity.getNavigation().stop();
        this.delayTemptCounter = 20;
        this.isRunning = false;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick()
    {
        this.temptedEntity.getLookControl().setLookAt(this.temptingPlayer, (float)(this.temptedEntity.getMaxHeadYRot() + 20), (float)this.temptedEntity.getMaxHeadXRot());

        if (this.temptedEntity.distanceToSqr(this.temptingPlayer) < 6.25D)
        {
            this.temptedEntity.getNavigation().stop();
        }
        else
        {
            this.temptedEntity.getNavigation().moveTo(this.temptingPlayer, this.speed);
        }
    }

    /**
     * @see #isRunning
     */
    public boolean isRunning()
    {
        return this.isRunning;
    }
}