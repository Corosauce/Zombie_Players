package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EnumTrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class WorkInfo {

    private boolean performWork = false;
    private BlockPos posWorkCenter = BlockPos.ZERO;
    private AABB posWorkArea = new AABB(0, 0, 0, 0, 0, 0);
    private BlockState stateWorkLastObserved = Blocks.AIR.defaultBlockState();
    private ItemStack itemNeededForWork = ItemStack.EMPTY;
    private EnumTrainType workClickLastObserved = EnumTrainType.BLOCK_LEFT_CLICK;
    private Direction workClickDirectionLastObserved = Direction.UP;
    private boolean inTrainingMode = false;
    private boolean inAreaSetMode = false;
    private BlockPos workAreaPos1 = BlockPos.ZERO;

    public BlockPos getPosWorkCenter() {
        return posWorkCenter;
    }

    public void setPosWorkCenter(BlockPos posWorkCenter) {
        this.posWorkCenter = posWorkCenter;
    }

    public BlockState getStateWorkLastObserved() {
        return stateWorkLastObserved;
    }

    public void setStateWorkLastObserved(BlockState stateWorkLastObserved) {
        this.stateWorkLastObserved = stateWorkLastObserved;
    }

    public EnumTrainType getWorkClickLastObserved() {
        return workClickLastObserved;
    }

    public void setWorkClickLastObserved(EnumTrainType workClickLastObserved) {
        this.workClickLastObserved = workClickLastObserved;
    }

    public boolean isInTrainingMode() {
        return inTrainingMode;
    }

    public void setInTrainingMode(boolean inTrainingMode) {
        this.inTrainingMode = inTrainingMode;
    }

    public Direction getWorkClickDirectionLastObserved() {
        return workClickDirectionLastObserved;
    }

    public void setWorkClickDirectionLastObserved(Direction workClickDirectionLastObserved) {
        this.workClickDirectionLastObserved = workClickDirectionLastObserved;
    }

    public boolean isPerformingWork() {
        return performWork;
    }

    public void setPerformWork(boolean performWork) {
        this.performWork = performWork;
    }

    public ItemStack getItemNeededForWork() {
        return itemNeededForWork;
    }

    public void setItemNeededForWork(ItemStack itemNeededForWork) {
        this.itemNeededForWork = itemNeededForWork;
    }

    public boolean isInAreaSetMode() {
        return inAreaSetMode;
    }

    public void setInAreaSetMode(boolean inAreaSetMode) {
        this.inAreaSetMode = inAreaSetMode;
    }

    public BlockPos getWorkAreaPos1() {
        return workAreaPos1;
    }

    public void setWorkAreaPos1(BlockPos workCenterPos1) {
        this.workAreaPos1 = workCenterPos1;
    }

    public AABB getPosWorkArea() {
        return posWorkArea;
    }

    public void setPosWorkArea(AABB posWorkArea) {
        this.posWorkArea = posWorkArea;
    }
}
