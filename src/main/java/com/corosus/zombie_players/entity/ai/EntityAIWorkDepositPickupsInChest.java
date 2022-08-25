package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class EntityAIWorkDepositPickupsInChest extends Goal
{
    private final ZombiePlayer entityObj;

    private int walkingTimeoutMax = 20*10;

    private int walkingTimeout;
    private int repathPentalty = 0;

    private int lookUpdateTimer = 0;

    public BlockPos posCachedBestChest = BlockPos.ZERO;

    public EntityAIWorkDepositPickupsInChest(ZombiePlayer entityObjIn)
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
        if (entityObj.hasAnyItemsInExtra() && entityObj.hasChestToUse()) {
            posCachedBestChest = entityObj.getClosestChestPosWithSpace();
            return posCachedBestChest != BlockPos.ZERO;
        }
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse()
    {
        return entityObj.hasAnyItemsInExtra() && verifyOrGetNewChest();
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

        if (posCachedBestChest != BlockPos.ZERO) {
            boolean isClose = false;
            BlockPos blockposGoal = posCachedBestChest;

            if (blockposGoal == BlockPos.ZERO) {
                stop();
                return;
            }

            //prevent walking into the fire
            double dist = entityObj.position().distanceTo(new Vec3(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 3D) {
                entityObj.setDepositingInChest(true);
                entityObj.openChest(posCachedBestChest);

                entityObj.ejectItems(posCachedBestChest);
                entityObj.swing(InteractionHand.MAIN_HAND);
                entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(posCachedBestChest.getX() + 0.5, posCachedBestChest.getY() + 0.5, posCachedBestChest.getZ() + 0.5));

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
                            success = this.entityObj.getNavigation().moveTo(vec3d.x, vec3d.y, vec3d.z, 1.3D);
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
        entityObj.setDepositingInChest(false);
        ((ServerLevel)entityObj.level).sendParticles(DustParticleOptions.REDSTONE, entityObj.getX(), entityObj.getY() + 1.5, entityObj.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
    }

    public boolean verifyOrGetNewChest() {
        if (posCachedBestChest == BlockPos.ZERO) return false;
        if (!entityObj.isValidChestForWork(posCachedBestChest, false)) {
            posCachedBestChest = entityObj.getClosestChestPosWithSpace();
        }
        return posCachedBestChest != BlockPos.ZERO;
    }
}