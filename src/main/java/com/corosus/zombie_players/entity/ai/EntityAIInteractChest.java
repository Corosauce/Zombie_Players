package com.corosus.zombie_players.entity.ai;

import javax.annotation.Nullable;

import CoroUtil.forge.CULog;
import CoroUtil.util.CoroUtilEntity;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import com.corosus.zombie_players.util.UtilScanner;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EntityAIInteractChest extends EntityAIBase
{
    protected final EntityZombiePlayer entity;
    protected double x;
    protected double y;
    protected double z;
    protected final double speed;
    protected int executionChance;
    protected boolean mustUpdate;

    public int ticksChestOpen = 0;
    public int ticksChestOpenMax = 10;

    public EntityAIInteractChest(EntityZombiePlayer creatureIn, double speedIn, int chance)
    {
        this.entity = creatureIn;
        this.speed = speedIn;
        this.executionChance = chance;
        this.setMutexBits(1);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */

    @Override
    public boolean shouldExecute()
    {

        if (!ConfigZombiePlayers.messUpChests ||
                (entity instanceof EntityZombiePlayer && !((EntityZombiePlayer)entity).spawnedFromPlayerDeath)) return false;

        if (entity instanceof EntityZombiePlayer && ((EntityZombiePlayer)entity).getCalmTime() > 0) return false;

        if (!this.mustUpdate)
        {
            /*if (this.entity.getIdleTime() >= 100)
            {
                return false;
            }*/

            if (this.entity.getRNG().nextInt(this.executionChance) != 0)
            {
                return false;
            }
        }

        Vec3d vec3d = this.getPosition();

        if (vec3d == null)
        {
            return false;
        }
        else
        {
            this.x = vec3d.x;
            this.y = vec3d.y;
            this.z = vec3d.z;
            this.mustUpdate = false;
            return true;
        }
    }

    @Nullable
    protected Vec3d getPosition()
    {
        BlockPos pos = UtilScanner.findBlock(entity, 16, 1, 10, UtilScanner::isChest);
        if (pos != null) {
            if (CoroUtilEntity.canCoordBeSeen(entity, pos.getX(), pos.getY() + 1, pos.getZ())) {
                CULog.dbg("found visible chest coord, pathing");
                return new Vec3d(pos);
            } else {
                //CULog.dbg("cant see");
            }
        }
        return null;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting()
    {
        return !this.entity.getNavigator().noPath() || ticksChestOpen < ticksChestOpenMax/* || !hasInteracted*/;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void startExecuting()
    {
        //hasInteracted = false;
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
    }

    @Override
    public void updateTask() {
        super.updateTask();

        double dist = entity.getDistance(x + 0.5D, y + 0.5D, z + 0.5D);
        if (dist < 2.5D) {
            entity.getNavigator().clearPathEntity();

            if (ticksChestOpen == 0) {
                shuffleSingleItemStack(new BlockPos(x, y, z));
            } else {

            }

            if (ticksChestOpen < ticksChestOpenMax) {
                ticksChestOpen++;
            }

        }
    }

    public void shuffleSingleItemStack(BlockPos pos) {
        TileEntity tEnt = entity.world.getTileEntity(pos);
        if (tEnt instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tEnt;
            int randSlot1 = 0;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                if (chest.getStackInSlot(i) != ItemStack.EMPTY) {
                    randSlot1 = i;
                    break;
                }
            }
            int randSlot2 = entity.world.rand.nextInt(chest.getSizeInventory());

            ItemStack stack1 = chest.getStackInSlot(randSlot1);
            ItemStack stack2 = chest.getStackInSlot(randSlot2);

            chest.setInventorySlotContents(randSlot1, stack2);
            chest.setInventorySlotContents(randSlot2, stack1);

            entity.swingArm(EnumHand.MAIN_HAND);
            if (!entity.hasOpenedChest) {
                entity.openChest(new BlockPos(x, y, z));
            }
            //hasInteracted = true;
            CULog.dbg("EntityAIInteractChest swapped item contents");
        } else {
            resetTask();
        }
    }

    @Override
    public void resetTask() {
        CULog.dbg("reset task");
        entity.getNavigator().clearPathEntity();
        /*if (entity.hasOpenedChest) {
            entity.closeChest(new BlockPos(x, y, z));
        }*/
        ticksChestOpen = 0;
        super.resetTask();
    }
}