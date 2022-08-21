package com.corosus.zombie_players.client.entity.layer;

import com.corosus.zombie_players.entity.ZombiePlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombificationLayer<T extends ZombiePlayer, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private final ResourceLocation textureLocation;
   private final M model;

   public ZombificationLayer(RenderLayerParent<T, M> p_117390_, M p_117391_, ResourceLocation p_117392_) {
      super(p_117390_);
      this.model = p_117391_;
      this.textureLocation = p_117392_;
   }

   /*public ZombificationLayer(RenderLayerParent<T, M> p_117390_) {
      super(p_117390_);
   }*/

   @Override
   public void render(PoseStack p_117394_, MultiBufferSource p_117395_, int p_117396_, T p_117397_, float p_117398_, float p_117399_, float p_117400_, float p_117401_, float p_117402_, float p_117403_) {
      //System.out.println("render zombification layer");
      VertexConsumer vertexconsumer = p_117395_.getBuffer(RenderType.outline(textureLocation));
      //this.getParentModel().renderToBuffer();
      float tintAdj = 0.6F;
      //GL11.glColor4f(tintAdj, 0.8F, tintAdj - 0.15F, 1.0F);
      //this.model.renderToBuffer(p_117394_, vertexconsumer, p_117396_, LivingEntityRenderer.getOverlayCoords(p_117397_, 0.0F), 0.0F, 0.0F, 0.0F, 1.0F);
      float r = tintAdj;
      float g = 0.8F;
      float b = tintAdj - 0.15F;
      /*r = 1;
      g = 0.5F;*/
      coloredCutoutModelCopyLayerRender(this.getParentModel(), this.model, textureLocation, p_117394_, p_117395_, p_117396_, p_117397_, p_117398_, p_117399_, p_117401_, p_117402_, p_117403_, p_117400_, r, g, b);
      //GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }
}