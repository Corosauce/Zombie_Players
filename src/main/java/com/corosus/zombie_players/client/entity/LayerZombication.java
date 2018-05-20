package com.corosus.zombie_players.client.entity;

import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import CoroUtil.util.UtilProfile;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerZombication implements LayerRenderer<EntityZombiePlayer>
{
    private static final ResourceLocation TEXTURE_ZOMBIFIED = new ResourceLocation(Zombie_Players.modID, "textures/entity/zombification.png");
    private final RenderZombiePlayer renderer;

    public LayerZombication(RenderZombiePlayer renderer)
    {
        this.renderer = renderer;
    }


    @Override
    public void doRenderLayer(EntityZombiePlayer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        this.renderer.bindTexture(TEXTURE_ZOMBIFIED);
        /*GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(!entitylivingbaseIn.isInvisible());
        int i = 61680;
        int j = 61680;
        int k = 0;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680.0F, 0.0F);
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);*/

        float tintAdj = 0.6F;
        GlStateManager.color(tintAdj, 0.8F, tintAdj - 0.15F, 1.0F);
        UtilProfile.CachedPlayerData cache = RenderZombiePlayer.getCachedPlayerData(entitylivingbaseIn);
        if (cache != null && cache.isSlim()) {
            this.renderer.modelPlayerThin.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } else {
            this.renderer.getMainModel().render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        /*Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
        this.endermanRenderer.setLightmap(entitylivingbaseIn);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();*/
    }

    @Override
    public boolean shouldCombineTextures()
    {
        //for hurt damage overlay etc, see dog collar
        return true;
    }
}