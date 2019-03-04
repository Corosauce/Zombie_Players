package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;

public class EntityAIEatToHeal extends EntityAIBase
{
    private final EntityZombiePlayer entityObj;

    private int walkingTimeoutMax = 20*10;

    private int walkingTimeout;
    private int repathPentalty = 0;

    private int lookUpdateTimer = 0;

    private float missingHealthToHeal = 5;

    public BlockPos posCachedBestChest = null;

    public EntityAIEatToHeal(EntityZombiePlayer entityObjIn)
    {
        this.entityObj = entityObjIn;
        this.setMutexBits(3);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean shouldExecute()
    {
        if (entityObj.isFoodNeedUrgent()/*entityObj.getHealth() < entityObj.getMaxHealth() - missingHealthToHeal*/) {
            posCachedBestChest = getClosestChestPosWithFood();
            return posCachedBestChest != null;//hasFoodSource();
        } else {
            return false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting()
    {
        return entityObj.isFoodNeedUrgent() && verifyOrGetNewChest();
    }

    @Override
    public void updateTask() {
        super.updateTask();

        /*if (hasFoodSource(entityObj.inventory)) {
            consumeOneStackSizeOfFood(entityObj.inventory);
            entityObj.heal(5);
            entityObj.world.playSound(null, entityObj.getPosition(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.NEUTRAL, 1F, 1F);
            return;
        }*/

        if (posCachedBestChest != null) {
            boolean isClose = false;
            BlockPos blockposGoal = posCachedBestChest;

            if (blockposGoal == null) {
                resetTask();
                return;
            }

            //prevent walking into the fire
            double dist = entityObj.getPositionVector().distanceTo(new Vec3d(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 3D) {
                entityObj.openChest(posCachedBestChest);
                consumeOneStackSizeOfFoodAtChest();
                entityObj.ateCalmingItem(true);
                entityObj.getNavigator().clearPathEntity();
                return;
            }

            if (!isClose) {
                if ((this.entityObj.getNavigator().noPath() || walkingTimeout <= 0) && repathPentalty <= 0) {

                    int i = blockposGoal.getX();
                    int j = blockposGoal.getY();
                    int k = blockposGoal.getZ();

                    boolean success = false;

                    if (this.entityObj.getDistanceSq(blockposGoal) > 256.0D) {
                        Vec3d vec3d = RandomPositionGenerator.findRandomTargetBlockTowards(this.entityObj, 14, 3, new Vec3d((double) i + 0.5D, (double) j, (double) k + 0.5D));

                        if (vec3d != null) {
                            success = this.entityObj.getNavigator().tryMoveToXYZ(vec3d.x, vec3d.y, vec3d.z, 1.3D);
                        }
                    } else {
                        success = this.entityObj.getNavigator().tryMoveToXYZ((double) i + 0.5D, (double) j, (double) k + 0.5D, 1.3D);
                    }

                    if (!success) {
                        repathPentalty = 40;
                    } else {
                        walkingTimeout = walkingTimeoutMax;
                    }
                } else {
                    if (walkingTimeout > 0) {
                        walkingTimeout--;
                    } else {

                    }
                }
            }

            if (repathPentalty > 0) {
                repathPentalty--;
            }

            if (lookUpdateTimer > 0) {
                lookUpdateTimer--;
            }
        }


    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void startExecuting()
    {
        super.startExecuting();
        //this.insidePosX = -1;
        //reset any previous path so updateTask can start with a fresh path
        this.entityObj.getNavigator().clearPathEntity();
    }

    /**
     * Resets the task
     */
    @Override
    public void resetTask()
    {
        super.resetTask();
        walkingTimeout = 0;
    }

    public BlockPos getClosestChestPosWithFood() {
        Iterator<BlockPos> it = entityObj.listPosChests.iterator();
        double closestDist = Double.MAX_VALUE;
        BlockPos closestPos = null;
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (entityObj.isValidChest(pos, false)) {
                double dist = entityObj.getPosition().distanceSq(pos);

                if (dist < closestDist) {
                    closestDist = dist;
                    closestPos = pos;
                }
            }
        }

        return closestPos;
    }

    public ItemStack consumeOneStackSizeOfFoodAtChest() {
        if (posCachedBestChest != null) {
            TileEntity tile = entityObj.world.getTileEntity(posCachedBestChest);
            if (tile instanceof TileEntityChest) {
                TileEntityChest chest = (TileEntityChest) tile;

                return consumeOneStackSizeOfFood(chest);
            }
        }
        return null;
    }

    /**
     * Return a snapshot of what its consuming incase we want to scale healing based on item/amount
     *
     * @param inv
     * @return
     */
    public ItemStack consumeOneStackSizeOfFood(IInventory inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() > 0) {
                if (entityObj.isRawMeat(stack)) {
                    stack.shrink(1);
                    if (stack.getCount() <= 0) {
                        inv.setInventorySlotContents(i, ItemStack.EMPTY);
                    }
                    return new ItemStack(stack.getItem(), 1, stack.getMetadata());
                }
            }
        }
        return null;
    }

    public boolean verifyOrGetNewChest() {
        if (posCachedBestChest == null) return false;
        if (!entityObj.isValidChest(posCachedBestChest, false)) {
            posCachedBestChest = getClosestChestPosWithFood();
        }
        return posCachedBestChest != null;
    }
}