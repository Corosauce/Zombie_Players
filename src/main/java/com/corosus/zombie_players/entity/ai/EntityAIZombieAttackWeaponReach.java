package com.corosus.zombie_players.entity.ai;

import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.monster.EntityZombie;

public class EntityAIZombieAttackWeaponReach extends EntityAIZombieAttack {

    private final EntityZombie zombie;

    public EntityAIZombieAttackWeaponReach(EntityZombie zombieIn, double speedIn, boolean longMemoryIn) {
        super(zombieIn, speedIn, longMemoryIn);
        this.zombie = zombieIn;
    }

    @Override
    protected double getAttackReachSqr(EntityLivingBase attackTarget) {
        if (zombie instanceof EntityZombiePlayer && !zombie.getHeldItemMainhand().isEmpty()) {
            float itemReachBuff = 0.5F;
            return (double)(this.attacker.width * 2.0F * this.attacker.width * 2.0F + attackTarget.width + itemReachBuff);
        } else {
            return super.getAttackReachSqr(attackTarget);
        }

    }
}
