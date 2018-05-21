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
    public void doRender(EntityZombiePlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {

        GlStateManager.pushMatrix();
        float shadowMaxSize = 0.5F;
        if (entity.risingTime < entity.risingTimeMax) {

            //request data ASAP, load while still invis
            UtilProfile.CachedPlayerData cache = getCachedPlayerData(entity);

            float f = 1F - ((entity.risingTime + partialTicks) / (float) entity.risingTimeMax);
            float shadowDelay = 20;
            float fInv = ((entity.risingTime - shadowDelay + partialTicks) / (float) (entity.risingTimeMax - shadowDelay));

            if (f > 1.0F) {
                f = 1.0F;
            }


            GlStateManager.translate(0, -f * 0.7F, 0);
            /*double yy = Math.sin(f * 20D) * 2D;
            yy *= Math.sin(f);
            GlStateManager.translate(0, yy, 0);*/
            //GlStateManager.translate(0, -Math.sin(Math.cos((Math.PI) + f) * 20F) * 1F, 0);

            if (entity.risingTime > shadowDelay) {
                shadowSize = fInv * shadowMaxSize;
            } else {
                shadowSize = 0;
            }

        } else {
            shadowSize = shadowMaxSize;
        }

        //be invisible if less than 0
        if (entity.risingTime >= 0) {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        GlStateManager.popMatrix();


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

            /**
             * angle = rise
             * angle2 = get up from side
             * angle3 = shake
             */

            double rotScale = Math.sin(f);
            //use 180 to play with rate change scale lazily
            double angle = (f * 90F) * rotScale;
            double angle2 = 0;//(f * 90F) * rotScale;

            double shakeRange = 30;
            double shakeSpeed = 70;
            double timeVal = ((entityLiving.world.getTotalWorldTime() + partialTicks) * shakeSpeed);
            double angle3 = ((shakeRange/2) + Math.sin(Math.toRadians((timeVal % 360))) * shakeRange) * rotScale;

            //GlStateManager.rotate((float) entityLiving.rotationYaw, 0.0F, 1.0F, 0.0F);

            GlStateManager.rotate((float) angle, 1.0F, 0.0F, 0.0F);

            GlStateManager.rotate((float) (angle2 + angle3), 0.0F, 1.0F, 0.0F);

            /*double rotY = Math.sin(f) * 360 * 15F;
            rotY *= Math.sin(f);
            GlStateManager.rotate((float) rotY, 0.0F, 1.0F, 0.0F);*/
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
                    RenderZombiePlayer.dbg(String.format(" full data received for %s, is slim = " + data.isSlim(), name));
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
