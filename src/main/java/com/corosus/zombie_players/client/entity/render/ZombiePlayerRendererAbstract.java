package com.corosus.zombie_players.client.entity.render;

import com.corosus.coroutil.util.CULog;
import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.client.entity.layer.ZombificationLayer;
import com.corosus.zombie_players.client.entity.model.ZombiePlayerModelZombieBased;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.corosus.zombie_players.util.UtilProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ZombiePlayerRendererAbstract<T extends ZombiePlayer, M extends ZombiePlayerModelZombieBased<T>> extends HumanoidMobRenderer<T, M> {
   private static final ResourceLocation ZOMBIE_LOCATION = new ResourceLocation("textures/entity/zombie/zombie.png");
   private static final ResourceLocation PLAYER_LOCATION = new ResourceLocation("textures/entity/steve.png");

   protected ZombiePlayerRendererAbstract(EntityRendererProvider.Context p_173910_, M p_173911_, M p_173912_, M p_173913_) {
      super(p_173910_, p_173911_, 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this, p_173912_, p_173913_));
      this.addLayer(new ZombificationLayer<>(this, p_173911_, new ResourceLocation(Zombie_Players.MODID,"textures/entity/zombification.png")));
   }

   public ResourceLocation getTextureLocation(ZombiePlayer p_113771_) {
      UtilProfile.CachedPlayerData cache = getCachedPlayerData(p_113771_);
      if (cache != null) {
         return cache.getTexture();
      } else {
         return PLAYER_LOCATION;
      }
   }

   public static UtilProfile.CachedPlayerData getCachedPlayerData(ZombiePlayer entity) {
      if (entity.getGameProfile() == null) {
         return null;
      }
      String name = entity.getGameProfile().getName();
      if (UtilProfile.getInstance().lookupNameToCachedData.containsKey(name)) {
         UtilProfile.CachedPlayerData data = UtilProfile.getInstance().lookupNameToCachedData.get(name);
         if (data.getTexture() == null) {
            if (data.getTemp() != null) {
               //actually load in the texture and data if its waiting to be loaded (must done done from gl context thread)
               data.setTexture(Minecraft.getInstance().getSkinManager().registerTexture(data.getTemp(), MinecraftProfileTexture.Type.SKIN));
               String model = data.getTemp().getMetadata("model");
               if (model != null) {
                  data.setSlim(model.equals("slim"));
               }
               CULog.dbg(String.format(" full data received for %s, is slim = " + data.isSlim(), name));
            } else {
               //means thread lookup failed, could be bad username, no internet, etc
               return null;
            }
         } else {
            //thread safe fallback?
            return data;
         }
         return data;
      } else {
         UtilProfile.getInstance().tryToAddProfileToLookupQueue(entity.getGameProfile());
      }

      return null;
   }

   protected boolean isShaking(T p_113773_) {
      return super.isShaking(p_113773_) || p_113773_.isUnderWaterConverting();
   }


}