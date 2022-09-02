package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Iterator;

public class EntityAIEatToHeal extends Goal
{
    private final ZombiePlayer entityObj;

    private int walkingTimeoutMax = 20*10;

    private int walkingTimeout;
    private int repathPentalty = 0;

    private int lookUpdateTimer = 0;

    private float missingHealthToHeal = 5;

    public BlockPos posCachedBestChest = null;

    public EntityAIEatToHeal(ZombiePlayer entityObjIn)
    {
        this.entityObj = entityObjIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse()
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
    public boolean canContinueToUse()
    {
        return entityObj.isFoodNeedUrgent() && verifyOrGetNewChest();
    }

    @Override
    public void tick() {
        super.tick();

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
                stop();
                return;
            }

            double dist = entityObj.position().distanceTo(new Vec3(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 3.8D) {
                entityObj.openChest(posCachedBestChest);
                for (int i = 0; i < 5 && (entityObj.getHealth() < entityObj.getMaxHealth() || entityObj.isCalmTimeLow()); i++) {
                    consumeOneStackSizeOfFoodAtChest();
                    entityObj.ateCalmingItem(true);
                }
                entityObj.getNavigation().stop();
                return;
            }

            if (!isClose) {
                if ((this.entityObj.getNavigation().isDone() || walkingTimeout <= 0) && repathPentalty <= 0) {

                    int i = blockposGoal.getX();
                    int j = blockposGoal.getY();
                    int k = blockposGoal.getZ();

                    boolean success = false;

                    if (this.entityObj.distanceToSqr(Vec3.atCenterOf(blockposGoal)) > 256.0D) {
                        Vec3 vec3d = DefaultRandomPos.getPosTowards(this.entityObj, 14, 3, new Vec3((double) i + 0.5D, (double) j, (double) k + 0.5D), (double)((float)Math.PI / 2F));

                        if (vec3d != null) {
                            //on top of chest
                            success = this.entityObj.getNavigation().moveTo(vec3d.x, vec3d.y+1, vec3d.z, 1.3D);
                        }
                    } else {
                        success = this.entityObj.getNavigation().moveTo((double) i + 0.5D, (double) j, (double) k + 0.5D, 1.3D);
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
    public void start()
    {
        super.start();
        //this.insidePosX = -1;
        //reset any previous path so tick can start with a fresh path
        this.entityObj.getNavigation().stop();
    }

    /**
     * Resets the task
     */
    @Override
    public void stop()
    {
        super.stop();
        walkingTimeout = 0;
    }

    public BlockPos getClosestChestPosWithFood() {
        Iterator<BlockPos> it = entityObj.listPosChests.iterator();
        double closestDist = Double.MAX_VALUE;
        BlockPos closestPos = null;
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (entityObj.isValidChestForFood(pos, false)) {
                double dist = entityObj.blockPosition().distSqr(pos);

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
            BlockEntity tile = entityObj.level.getBlockEntity(posCachedBestChest);
            if (tile instanceof ChestBlockEntity) {
                ChestBlockEntity chest = (ChestBlockEntity) tile;

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
    public ItemStack consumeOneStackSizeOfFood(Container inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getCount() > 0) {
                if (entityObj.isCalmingItem(stack)) {
                    stack.shrink(1);
                    if (stack.getCount() <= 0) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                    return new ItemStack(stack.getItem(), 1, stack.getTag());
                }
            }
        }
        return null;
    }

    public boolean verifyOrGetNewChest() {
        if (posCachedBestChest == null) return false;
        if (!entityObj.isValidChestForFood(posCachedBestChest, false)) {
            posCachedBestChest = getClosestChestPosWithFood();
        }
        return posCachedBestChest != null;
    }
}