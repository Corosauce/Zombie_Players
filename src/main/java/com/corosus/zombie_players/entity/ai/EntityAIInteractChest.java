package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CULog;
import com.corosus.coroutil.util.CoroUtilEntity;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.ZombiePlayerNew;
import com.corosus.zombie_players.util.UtilScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class EntityAIInteractChest extends Goal
{
    protected final ZombiePlayerNew entity;
    protected double x;
    protected double y;
    protected double z;
    protected final double speed;
    protected int executionChance;
    protected boolean mustUpdate;

    public int ticksChestOpen = 0;
    public int ticksChestOpenMax = 10;

    public EntityAIInteractChest(ZombiePlayerNew creatureIn, double speedIn, int chance)
    {
        this.entity = creatureIn;
        this.speed = speedIn;
        this.executionChance = chance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */

    @Override
    public boolean canUse()
    {

        if (!ConfigZombiePlayers.messUpChests ||
                (entity instanceof ZombiePlayerNew && !((ZombiePlayerNew)entity).spawnedFromPlayerDeath)) return false;

        if (entity instanceof ZombiePlayerNew && ((ZombiePlayerNew)entity).getCalmTime() > 0) return false;

        if (!this.mustUpdate)
        {
            /*if (this.entity.getIdleTime() >= 100)
            {
                return false;
            }*/

            if (this.entity.getRandom().nextInt(this.executionChance) != 0)
            {
                return false;
            }
        }

        Vec3 vec3d = this.getPosition();

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
    protected Vec3 getPosition()
    {
        BlockPos pos = UtilScanner.findBlock(entity, 16, 1, 10, UtilScanner::isChest);
        if (pos != null) {
            if (CoroUtilEntity.canSee(entity, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()))) {
                CULog.dbg("found visible chest coord, pathing");
                return new Vec3(pos.getX(), pos.getY(), pos.getZ());
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
    public boolean canContinueToUse()
    {
        return !this.entity.getNavigation().isDone() || ticksChestOpen < ticksChestOpenMax/* || !hasInteracted*/;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start()
    {
        //hasInteracted = false;
        this.entity.getNavigation().moveTo(this.x, this.y, this.z, this.speed);
    }

    @Override
    public void tick() {
        super.tick();

        double dist = Mth.sqrt((float) entity.distanceToSqr(x + 0.5D, y + 0.5D, z + 0.5D));
        if (dist < 2.5D) {
            entity.getNavigation().stop();

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
        BlockEntity tEnt = entity.level.getBlockEntity(pos);
        if (tEnt instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity) tEnt;
            int randSlot1 = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i) != ItemStack.EMPTY) {
                    randSlot1 = i;
                    break;
                }
            }
            int randSlot2 = entity.level.random.nextInt(chest.getContainerSize());

            ItemStack stack1 = chest.getItem(randSlot1);
            ItemStack stack2 = chest.getItem(randSlot2);

            chest.setItem(randSlot1, stack2);
            chest.setItem(randSlot2, stack1);

            entity.swing(InteractionHand.MAIN_HAND);
            if (!entity.hasOpenedChest) {
                entity.openChest(new BlockPos(x, y, z));
            }
            //hasInteracted = true;
            CULog.dbg("EntityAIInteractChest swapped item contents");
        } else {
            stop();
        }
    }

    @Override
    public void stop() {
        CULog.dbg("reset task");
        entity.getNavigation().stop();
        /*if (entity.hasOpenedChest) {
            entity.closeChest(new BlockPos(x, y, z));
        }*/
        ticksChestOpen = 0;
        super.stop();
    }
}