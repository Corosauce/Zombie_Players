package com.corosus.zombie_players.entity.ai;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class NearestAttackableTargetGoalIfCalm<T extends LivingEntity> extends TargetGoal {
   private static final int DEFAULT_RANDOM_INTERVAL = 10;
   protected final Class<T> targetType;
   protected final int randomInterval;
   @Nullable
   protected LivingEntity target;
   protected TargetingConditions targetConditions;

   protected ZombiePlayer entity;

   //invert is calm logic
   public boolean invert = false;


   /*public NearestAttackableTargetGoalIfCalm(ZombiePlayerNew p_26060_, Class<T> p_26061_, boolean p_26062_) {
      this(p_26060_, p_26061_, 10, p_26062_, false, (Predicate<LivingEntity>)null);
   }

   public NearestAttackableTargetGoalIfCalm(ZombiePlayerNew p_199891_, Class<T> p_199892_, boolean p_199893_, Predicate<LivingEntity> p_199894_) {
      this(p_199891_, p_199892_, 10, p_199893_, false, p_199894_);
   }

   public NearestAttackableTargetGoalIfCalm(ZombiePlayerNew p_26064_, Class<T> p_26065_, boolean p_26066_, boolean p_26067_) {
      this(p_26064_, p_26065_, 10, p_26066_, p_26067_, (Predicate<LivingEntity>)null);
   }*/

   public NearestAttackableTargetGoalIfCalm(ZombiePlayer p_26053_, Class<T> p_26054_, int p_26055_, boolean p_26056_, boolean p_26057_, @Nullable Predicate<LivingEntity> p_26058_, boolean attackIfHostile) {

      super(p_26053_, p_26056_, p_26057_);
      this.entity = p_26053_;
      this.invert = attackIfHostile;
      this.targetType = p_26054_;
      this.randomInterval = reducedTickDelay(p_26055_);
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
      this.targetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(p_26058_);
   }

   public boolean canUse() {
      if (invert) {
         if (entity.getCalmTime() > 0) {
            return false;
         }
      } else {
         if (entity.getCalmTime() == 0) {
            return false;
         }
      }

      if (!entity.isWithinRestriction()) return false;

      if (entity.getCalmTime() > 0 && entity.isHealthLow()) {
         return false;
      }
      if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
         return false;
      } else {
         this.findTarget();
         return this.target != null;
      }
   }

   protected AABB getTargetSearchArea(double p_26069_) {
      return this.mob.getBoundingBox().inflate(p_26069_, 4.0D, p_26069_);
   }

   protected void findTarget() {
      if (this.targetType != Player.class && this.targetType != ServerPlayer.class) {
         this.target = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), (p_148152_) -> {
            return true;
         }), this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
      } else {
         this.target = this.mob.level.getNearestPlayer(this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
      }

   }

   public void start() {
      this.mob.setTarget(this.target);
      super.start();
   }

   public void setTarget(@Nullable LivingEntity p_26071_) {
      this.target = p_26071_;
   }
}