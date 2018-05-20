package com.corosus.zombie_players.client.entity;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.client.model.ModelZombiePlayer;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import CoroUtil.util.UtilProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;

public class RenderZombiePlayer extends RenderBiped<EntityZombiePlayer> {

    public ModelZombiePlayer modelPlayerThin = new ModelZombiePlayer(0.0F, true);

    public RenderZombiePlayer(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelZombiePlayer(0.0F, false), 0.5F);

        this.getMainModel().isChild = false;
        modelPlayerThin.isChild = false;

        this.addLayer(new LayerZombication(this));

        LayerBipedArmor layerbipedarmor = new LayerBipedArmor(this)
        {
            protected void initArmor()
            {
                this.modelLeggings = new ModelZombie(0.5F, true);
                this.modelArmor = new ModelZombie(1.0F, true);
            }
        };
        this.addLayer(layerbipedarmor);
    }

    @Override
    protected void renderModel(EntityZombiePlayer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor)
    {
        boolean flag = this.isVisible(entitylivingbaseIn);
        boolean flag1 = !flag && !entitylivingbaseIn.isInvisibleToPlayer(Minecraft.getMinecraft().player);

        if (flag || flag1)
        {

            UtilProfile.CachedPlayerData cache = getCachedPlayerData(entitylivingbaseIn);

            if (cache != null) {
                this.bindTexture(cache.getTexture());
            } else {
                //gets default player skin
                this.bindTexture(getEntityTexture(entitylivingbaseIn));
            }

            if (flag1)
            {
                GlStateManager.enableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
            }


            float tintAdj = 0.6F;
            GlStateManager.color(tintAdj, 0.8F, tintAdj - 0.15F, 1.0F);
            if (cache != null && cache.isSlim()) {
                modelPlayerThin.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            } else {
                this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            if (flag1)
            {
                GlStateManager.disableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
            }
        }
    }

    @Override
    protected void applyRotations(EntityZombiePlayer entityLiving, float p_77043_2_, float rotationYaw, float partialTicks)
    {
        if (entityLiving.risingTime < entityLiving.risingTimeMax)
        {
            float f = 1F - ((entityLiving.risingTime + partialTicks) / (float)entityLiving.risingTimeMax);

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            //GlStateManager.translate(0, f * 2F, 0);
            //GlStateManager.translate(0, -f * 3F, 0);
            GlStateManager.rotate(-0F + (f * 90F)/* * this.getDeathMaxRotation(entityLiving)*/, 1.0F, 0.0F, 0.0F);
            //GlStateManager.rotate(-0F + (f * 90F)/* * this.getDeathMaxRotation(entityLiving)*/, 1.0F, 0.0F, 0.0F);
        }

        super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
    }

    public static UtilProfile.CachedPlayerData getCachedPlayerData(EntityZombiePlayer entity) {
        if (entity.getGameProfile() == null) {
            return null;
        }
        String name = entity.getGameProfile().getName();
        if (UtilProfile.getInstance().lookupNameToCachedData.containsKey(name)) {
            UtilProfile.CachedPlayerData data = UtilProfile.getInstance().lookupNameToCachedData.get(name);
            if (data.getTexture() == null) {
                if (data.getTemp() != null) {
                    //actually load in the texture and data if its waiting to be loaded (must done done from gl context thread)
                    data.setTexture(Minecraft.getMinecraft().getSkinManager().loadSkin(data.getTemp(), MinecraftProfileTexture.Type.SKIN, null));
                    String model = data.getTemp().getMetadata("model");
                    if (model != null) {
                        data.setSlim(model.equals("slim"));
                    }
                    RenderZombiePlayer.dbg(String.format("full data received for %s, is slim = " + data.isSlim(), name));
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

    public static void dbg(String str) {
        CULog.dbg(str);
    }
}
