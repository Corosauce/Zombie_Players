package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EnumTrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WorkInfo {

    private BlockPos posWorkCenter = BlockPos.ZERO;

    private BlockState stateWorkLastObserved = Blocks.AIR.defaultBlockState();

    //0 = left, 1 = right
    private EnumTrainType workClickLastObserved = EnumTrainType.BLOCK_LEFT_CLICK;

    private boolean inTrainingMode = false;

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
}
