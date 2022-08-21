package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;

public class EntityAIZombieAttackWeaponReach extends ZombieAttackGoal {

    private final Zombie zombie;

    public EntityAIZombieAttackWeaponReach(Zombie zombieIn, double speedIn, boolean longMemoryIn) {
        super(zombieIn, speedIn, longMemoryIn);
        this.zombie = zombieIn;
    }

    @Override
    protected double getAttackReachSqr(LivingEntity attackTarget) {
        if (zombie instanceof ZombiePlayer && !zombie.getMainHandItem().isEmpty()) {
            float itemReachBuff = 3F;
            return (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + attackTarget.getBbWidth() + itemReachBuff);
        } else {
            return super.getAttackReachSqr(attackTarget);
        }

    }
}
