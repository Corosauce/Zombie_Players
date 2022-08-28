package com.corosus.zombie_players.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

public class FakePlayerInventoryProxy extends FakePlayer {

    private ZombiePlayer zombiePlayer;

    public FakePlayerInventoryProxy(ServerLevel level, GameProfile name, ZombiePlayer zombiePlayer) {
        super(level, name);
        this.zombiePlayer = zombiePlayer;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot p_36257_) {
        //this ^ method is called in super constructor before parent can set zombie player reference
        if (getZombiePlayer() == null) return super.getItemBySlot(p_36257_);
        return getZombiePlayer().getItemBySlot(p_36257_);
    }

    public ZombiePlayer getZombiePlayer() {
        return zombiePlayer;
    }

    public void setZombiePlayer(ZombiePlayer zombiePlayer) {
        this.zombiePlayer = zombiePlayer;
    }

    @Override
    public int getUseItemRemainingTicks() {
        return getMainHandItem().getUseDuration() - 20;
        //return super.getUseItemRemainingTicks();
    }
}
