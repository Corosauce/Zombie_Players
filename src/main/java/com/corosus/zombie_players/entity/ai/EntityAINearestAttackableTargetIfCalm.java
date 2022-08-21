package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CU;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.google.common.base.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class EntityAINearestAttackableTargetIfCalm<T extends LivingEntity> extends TargetGoal
{
    protected final Class<T> targetClass;
    private final int targetChance;
    /** Instance of EntityAINearestAttackableTargetSorter. */
    protected final Sorter sorter;
    protected final Predicate <? super T > targetEntitySelector;
    protected T targetEntity;

    protected ZombiePlayer entity;

    //invert is calm logic
    public boolean invert = false;

    public EntityAINearestAttackableTargetIfCalm(ZombiePlayer creature, Class<T> classTarget, boolean checkSight, boolean attackIfHostile)
    {
        this(creature, classTarget, checkSight, false, attackIfHostile);
    }

    public EntityAINearestAttackableTargetIfCalm(ZombiePlayer creature, Class<T> classTarget, boolean checkSight, boolean onlyNearby, boolean attackIfHostile)
    {
        this(creature, classTarget, 10, checkSight, onlyNearby, (Predicate)null, attackIfHostile);
    }

    public EntityAINearestAttackableTargetIfCalm(ZombiePlayer creature, Class<T> classTarget, int chance, boolean checkSight, boolean onlyNearby, @Nullable final Predicate <? super T > targetSelector, boolean attackIfHostile)
    {
        super(creature, checkSight, onlyNearby);
        this.invert = attackIfHostile;
        this.entity = creature;
        this.targetClass = classTarget;
        this.targetChance = chance;
        this.sorter = new Sorter(creature);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.targetEntitySelector = new Predicate<T>()
        {
            public boolean apply(@Nullable T p_apply_1_)
            {
                if (p_apply_1_ == null)
                {
                    return false;
                }
                else if (targetSelector != null && !targetSelector.apply(p_apply_1_))
                {
                    return false;
                }
                else
                {
                    //return !EntitySelector.NO_SPECTATORS.test(p_apply_1_) ? false : EntityAINearestAttackableTargetIfCalm.this.isSuitableTarget((Mob)p_apply_1_, false);
                    //return fuck off
                    return false;//return !EntitySelector.NO_SPECTATORS.test(p_apply_1_) ? false : EntityAINearestAttackableTargetIfCalm.this.isSuitableTarget2((Mob)p_apply_1_, checkSight);
                    //return !EntitySelector.NO_SPECTATORS.test(p_apply_1_) ? false : EntityAINearestAttackableTargetIfCalm.this.test(entity, (Mob)p_apply_1_);
                }
            }
        };
    }

    public static boolean canAttackClass(Class <? extends LivingEntity > cls)
    {
        return cls != Ghast.class;
    }

    public static boolean isSuitableTarget2(Mob p_179445_0_, @Nullable LivingEntity p_179445_1_, boolean p_179445_3_) {
        if (p_179445_1_ == null) {
            return false;
        } else if (p_179445_1_ == p_179445_0_) {
            return false;
        } else if (!p_179445_1_.isAlive()) {
            return false;
        } else if (!canAttackClass(p_179445_1_.getClass())) {
            return false;
        /*} else if (p_179445_0_.isOnSameTeam(p_179445_1_)) {
            return false;*/
        } else {
            if (p_179445_0_ instanceof OwnableEntity && ((OwnableEntity)p_179445_0_).getOwnerUUID() != null) {
                if (p_179445_1_ instanceof OwnableEntity && ((OwnableEntity)p_179445_0_).getOwnerUUID().equals(((OwnableEntity)p_179445_1_).getOwnerUUID())) {
                    return false;
                }

                if (p_179445_1_ == ((OwnableEntity)p_179445_0_).getOwner()) {
                    return false;
                }
            }/* else if (p_179445_1_ instanceof Player && !p_179445_2_ && ((Player)p_179445_1_).capabilities.disableDamage) {
                return false;
            }*/

            return !p_179445_3_ || p_179445_0_.getSensing().hasLineOfSight(p_179445_1_);
        }
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean canUse()
    {

        if (invert) {
            if (entity.getCalmTime() > 0) {
                return false;
            }
        } else {
            if (entity.getCalmTime() == 0) {
                return false;
            }
        }

        if (entity.getCalmTime() > 0 && entity.isHealthLow()) {
            return false;
        }

        if (this.targetChance > 0 && CU.rand().nextInt(this.targetChance) != 0)
        {
            return false;
        }
        else if (this.targetClass != Player.class && this.targetClass != ServerPlayer.class)
        {
            List<T> list = this.entity.level.getEntitiesOfClass(this.targetClass, this.getTargetableArea(entity.getAttribute(Attributes.FOLLOW_RANGE).getValue()), this.targetEntitySelector);

            if (list.isEmpty())
            {
                return false;
            }
            else
            {
                Collections.sort(list, this.sorter);
                this.targetEntity = list.get(0);
                return true;
            }
        }
        else
        {
            this.targetEntity = (T)this.entity.level.getNearestPlayer(this.entity.getX(), this.entity.getY() + (double)this.entity.getEyeHeight(), this.entity.getZ(), this.getFollowDistance(), (Predicate<Entity>)this.targetEntitySelector);
            return this.targetEntity != null;
        }
    }

    protected double getFollowDistance() {
        return this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    protected AABB getTargetableArea(double targetDistance)
    {
        return this.entity.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start()
    {
        this.entity.setTarget(this.targetEntity);
        super.start();
    }

    public static class Sorter implements Comparator<Entity>
        {
            private final Entity entity;

            public Sorter(Entity entityIn)
            {
                this.entity = entityIn;
            }

            public int compare(Entity p_compare_1_, Entity p_compare_2_)
            {
                double d0 = this.entity.distanceToSqr(p_compare_1_);
                double d1 = this.entity.distanceToSqr(p_compare_2_);

                if (d0 < d1)
                {
                    return -1;
                }
                else
                {
                    return d0 > d1 ? 1 : 0;
                }
            }
        }
}