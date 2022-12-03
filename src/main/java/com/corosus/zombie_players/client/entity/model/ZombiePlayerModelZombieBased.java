package com.corosus.zombie_players.client.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombiePlayerModelZombieBased<T extends Zombie> extends AbstractZombieModel<T> {
   public ZombiePlayerModelZombieBased(ModelPart p_171090_) {
      super(p_171090_);
   }

   public boolean isAggressive(T p_104155_) {
      return p_104155_.isAggressive();
   }

   @Override
   public void setupAnim(T p_102001_, float p_102002_, float p_102003_, float p_102004_, float p_102005_, float p_102006_) {
      super.setupAnim(p_102001_, p_102002_, p_102003_, p_102004_, p_102005_, p_102006_);

      this.body.zRot = 0;
      float headbob = (Mth.sin(0.05F * p_102004_) * 5 + 15) * Mth.DEG_TO_RAD;
      this.head.xRot = headbob;
      this.hat.xRot = headbob;
      this.rightArm.xRot += 30 * Mth.DEG_TO_RAD;
      this.leftArm.xRot += 30 * Mth.DEG_TO_RAD;
   }

   @Override
   public void renderToBuffer(PoseStack p_102034_, VertexConsumer p_102035_, int p_102036_, int p_102037_, float p_102038_, float p_102039_, float p_102040_, float p_102041_) {
      float tintAdj = 0.6F;
      float r = tintAdj;
      float g = 0.8F;
      float b = tintAdj - 0.15F;
      super.renderToBuffer(p_102034_, p_102035_, p_102036_, p_102037_, r, g, b, p_102041_);
   }
}