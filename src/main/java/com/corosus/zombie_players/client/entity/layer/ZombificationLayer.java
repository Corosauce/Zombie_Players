package com.corosus.zombie_players.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Saddleable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombificationLayer<T extends Entity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private final ResourceLocation textureLocation;
   private final M model;

   public ZombificationLayer(RenderLayerParent<T, M> p_117390_, M p_117391_, ResourceLocation p_117392_) {
      super(p_117390_);
      this.model = p_117391_;
      this.textureLocation = p_117392_;
   }

   public void render(PoseStack p_117394_, MultiBufferSource p_117395_, int p_117396_, T p_117397_, float p_117398_, float p_117399_, float p_117400_, float p_117401_, float p_117402_, float p_117403_) {

   }
}