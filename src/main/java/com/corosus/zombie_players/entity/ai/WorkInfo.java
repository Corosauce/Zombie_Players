package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EnumTrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class WorkInfo {

    public static AABB CENTER_ZERO = new AABB(0, 0, 0, 0, 0, 0);

    private boolean performWork = false;
    private AABB posWorkArea = CENTER_ZERO;
    private BlockState stateWorkLastObserved = Blocks.AIR.defaultBlockState();
    private ItemStack itemNeededForWork = ItemStack.EMPTY;
    private EnumTrainType workClickLastObserved = EnumTrainType.BLOCK_LEFT_CLICK;
    private Direction workClickDirectionLastObserved = Direction.UP;
    private boolean inTrainingMode = false;
    private boolean inAreaSetMode = false;
    private boolean exactMatchMode = false;
    private BlockPos workAreaPos1 = BlockPos.ZERO;
    private BlockHitResult blockHitResult = null;

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
        this.itemNeededForWork = itemNeededForWork.copy();
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

    public boolean isWorkAreaSet() {
        return posWorkArea.getSize() > 0;
    }

    public BlockHitResult getBlockHitResult() {
        return blockHitResult;
    }

    public void setBlockHitResult(BlockHitResult blockHitResult) {
        this.blockHitResult = blockHitResult;
    }

    public boolean isExactMatchMode() {
        return exactMatchMode;
    }

    public void setExactMatchMode(boolean exactMatchMode) {
        this.exactMatchMode = exactMatchMode;
    }
}
