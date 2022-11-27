package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class RandomStrollGoalToggled extends WaterAvoidingRandomStrollGoal {

    private ZombiePlayer zombiePlayer;

    public RandomStrollGoalToggled(ZombiePlayer p_25987_, double p_25988_) {
        super(p_25987_, p_25988_);
        this.zombiePlayer = p_25987_;
    }

    @Override
    public boolean canUse() {
        if (!zombiePlayer.shouldWander) return false;
        return super.canUse();
    }
}
