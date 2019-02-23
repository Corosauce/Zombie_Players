package com.corosus.zombie_players.entity;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.ai.*;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.Arrays;

public class EntityZombiePlayer extends EntityZombie implements IEntityAdditionalSpawnData {

    public static EntityZombiePlayer spawnInPlaceOfPlayer(EntityPlayerMP player) {
        boolean spawn = true;
        EntityZombiePlayer zombie = null;
        if (player.getBedLocation() != null) {
            if (player.getBedLocation().getDistance(MathHelper.floor(player.posX), MathHelper.floor(player.posY), MathHelper.floor(player.posZ)) <
                    ConfigZombiePlayers.distanceFromPlayerSpawnPointToPreventZombieSpawn) {
                spawn = false;
            }
        }
        if (spawn) {
            zombie = spawnInPlaceOfPlayer(player.world, player.posX, player.posY, player.posZ, player.getGameProfile());
            if (player.getBedLocation() != null) {
                zombie.setHomePosAndDistance(player.getBedLocation(), 16);
            }
            //doesnt do much during rising...
            //zombie.setRotation(player.rotationYaw, player.rotationPitch);
        }
        return zombie;
    }

    public static EntityZombiePlayer spawnInPlaceOfPlayer(World world, double x, double y, double z, GameProfile profile) {
        EntityZombiePlayer zombie = new EntityZombiePlayer(world);
        zombie.setPosition(x, y, z);
        zombie.setGameProfile(profile);
        zombie.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        zombie.enablePersistence();
        zombie.spawnedFromPlayerDeath = true;
        world.spawnEntity(zombie);
        return zombie;
    }

    public boolean spawnedFromPlayerDeath = false;

    public GameProfile gameProfile;

    public int risingTime = -20;
    public int risingTimeMax = 40;

    public boolean quiet = false;

    private boolean isPlaying;

    private int calmTime = 0;

    public EntityZombiePlayer(World worldIn) {
        super(worldIn);
        ((PathNavigateGround) this.getNavigator()).setBreakDoors(ConfigZombiePlayers.opensDoors);
    }

    @Override
    protected void initEntityAI() {
        //super.initEntityAI();

        int taskID = 0;

        /**
         * Theres a conflict between moving to wanted food and attacking player
         * - move to food triggers a pathfind, but then something else stops it instantly, only when theres an enemy to go after though, hrm
         */

        this.tasks.addTask(taskID++, new EntityAISwimming(this));
        this.tasks.addTask(taskID++, new EntityAIZombieAttackWeaponReach(this, 1.0D, false));
        this.tasks.addTask(taskID++, new EntityAIMoveToWantedNearbyItems(this, 1.15D));
        this.tasks.addTask(taskID++, new EntityAIOpenDoor(this, false));
        this.tasks.addTask(taskID++, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(taskID++, new EntityAITemptZombie(this, 1.2D, false/*, Sets.newHashSet(Zombie_Players.calmingItems)*/));

        //will this mess with calm state?
        this.tasks.addTask(taskID++, new EntityAIMoveThroughVillage(this, 1.0D, false));
        this.tasks.addTask(taskID++, new EntityAIInteractChest(this, 1.0D, 20));
        this.tasks.addTask(taskID++, new EntityAIPlayZombiePlayer(this, 1.15D));
        this.tasks.addTask(taskID++, new EntityAIWanderAvoidWater(this, 1.0D));

        this.tasks.addTask(taskID, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(taskID, new EntityAIWatchClosest(this, EntityZombiePlayer.class, 8.0F));
        this.tasks.addTask(taskID, new EntityAILookIdle(this));
        //this.applyEntityAI();

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[] {EntityPigZombie.class}));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTargetIfCalm(this, EntityPlayer.class, true, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTargetIfCalm(this, EntityVillager.class, false, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTargetIfCalm(this, EntityIronGolem.class, true, true));

        this.targetTasks.addTask(3, new EntityAINearestAttackableTargetIfCalm(this, EntityLiving.class, 0, true, false, new Predicate<EntityLiving>()
        {
            public boolean apply(@Nullable EntityLiving p_apply_1_)
            {
                return p_apply_1_ != null && IMob.VISIBLE_MOB_SELECTOR.apply(p_apply_1_) && !(p_apply_1_ instanceof EntityCreeper) && (!(p_apply_1_ instanceof EntityZombiePlayer) ||
                        (p_apply_1_ instanceof EntityZombiePlayer && ((EntityZombiePlayer) p_apply_1_).calmTime == 0 && p_apply_1_.getAttackTarget() != null));
            }
        }, false));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        //this.setHealth(10);

        if (risingTime < risingTimeMax) risingTime++;

        //calmTime = 120;
        //calmTime = 20*93;

        if (!world.isRemote) {
            if (calmTime > 0) {

                if (calmTime <= 100 && calmTime % 10 == 0) {
                    this.playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, getSoundVolume(), getSoundPitch());
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.VILLAGER_ANGRY, true, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                            1, 0.3D, 0D, 0.3D, 1D, 0);
                } else if (calmTime <= 20 * 90 && calmTime % (20 * 5) == 0) {
                    this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, getSoundVolume(), getSoundPitch());
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.VILLAGER_ANGRY, true, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                            1, 0.3D, 0D, 0.3D, 1D, 0);
                } else if (isFoodNeedUrgent()) {
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.REDSTONE, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                            1, 0.1D, 0D, 0.1D, 0D, 0);
                }

                calmTime--;

                if (calmTime == 0) {
                    this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
                }
            }

            //pickup items we want, slow rate if pickup if well fed so others more hungry grab it first
            if (isFoodNeedUrgent() || (!ConfigZombiePlayersAdvanced.onlySeekFoodIfNeeded && world.getTotalWorldTime() % 20 == 0)) {
                for (EntityItem entityitem : this.world.getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().grow(1.0D, 0.0D, 1.0D))) {
                    if (!entityitem.isDead && !entityitem.getItem().isEmpty() && !entityitem.cannotPickup() && isItemWeWant(entityitem.getItem())) {
                        //this.updateEquipmentIfNeeded(entityitem);
                        this.onItemPickup(entityitem, entityitem.getItem().getCount());
                        entityitem.setDead();
                        ateCalmingItem();

                        //only have 1 ate a time to stagger hogging
                        break;
                    }
                }
            }

            if (world.getTotalWorldTime() % 20 == 0 && isItemWeWant(getHeldItemMainhand())) {
                for (int i = 0; i < getHeldItemMainhand().getCount(); i++) {

                    //only do effect sounds and visuals once
                    if (i == 0) {
                        ateCalmingItem(true);
                    } else {
                        ateCalmingItem(false);
                    }

                }
                setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        if (risingTime < risingTimeMax) {
            this.setNoAI(true);
            if (world.isRemote) {
                IBlockState state = world.getBlockState(getPosition().down());
                double speed = 0.2D;
                for (int i = 0; i < 5; i++) {
                    double x1 = world.rand.nextDouble() - world.rand.nextDouble();
                    double z1 = world.rand.nextDouble() - world.rand.nextDouble();
                    double x2 = (world.rand.nextDouble() - world.rand.nextDouble()) * speed;
                    double y2 = (world.rand.nextDouble() - world.rand.nextDouble()) * speed;
                    double z2 = (world.rand.nextDouble() - world.rand.nextDouble()) * speed;
                    world.spawnParticle(EnumParticleTypes.BLOCK_DUST, posX + x1, posY, posZ + z1, x2, y2, z2, Block.getStateId(state));
                }
            }
        } else {
            this.setNoAI(false);
        }
    }

    public boolean isItemWeWant(ItemStack stack) {
        return isRawMeat(stack);
    }

    public void ateCalmingItem() {
        ateCalmingItem(true);
    }

    public void ateCalmingItem(boolean effect) {
        this.calmTime += ConfigZombiePlayersAdvanced.calmTimePerUse;
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);

        this.setAttackTarget(null);
        this.setRevengeTarget(null);

        this.heal((float) ConfigZombiePlayersAdvanced.healPerUse);
        if (effect) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                    1, 0.3D, 0D, 0.3D, 1D, 0);
            world.playSound((EntityPlayer) null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public void setAttackTarget(@Nullable EntityLivingBase entitylivingbaseIn) {
        //cancel player targetting if calm
        if (calmTime > 0 && entitylivingbaseIn instanceof EntityPlayer) {
            super.setAttackTarget(null);
            //usefull until zombieawareness update that patches this better
            this.getNavigator().clearPathEntity();
        //cancel other AI making us target other zombie players, mainly the call for help logic from EntityAIHurtByTarget
        //also only if that other zombie player is calm too
        } else if (calmTime > 0 && entitylivingbaseIn instanceof EntityZombiePlayer && ((EntityZombiePlayer) entitylivingbaseIn).calmTime > 0) {
            //super.setAttackTarget(null);
        } else {
            super.setAttackTarget(entitylivingbaseIn);
        }
    }

    public boolean isRawMeat(ItemStack stack) {
        Item item = stack.getItem();
        return Zombie_Players.calmingItems.contains(item);
    }

    @Override
    public void playLivingSound()
    {
        if (!quiet) {
            SoundEvent soundevent = this.getAmbientSound();

            if (soundevent != null) {
                this.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {

        if (!world.isRemote && player != null && hand == EnumHand.MAIN_HAND) {
            ItemStack itemstack = player.getHeldItem(hand);

            boolean itemUsed = false;
            int particleCount = 5;
            EnumParticleTypes particle = EnumParticleTypes.HEART;

            if (itemstack.getItem() == Items.APPLE) {
                itemUsed = true;
                quiet = !quiet;

                particle = EnumParticleTypes.NOTE;
            } else if (itemstack.getItem() == Items.GLASS_BOTTLE) {
                itemUsed = true;
                this.setChild(!this.isChild());

                particle = EnumParticleTypes.SPELL_MOB;
            } else if (itemstack.getItem() == Items.ROTTEN_FLESH) {
                itemUsed = true;

                this.dropEquipment(true, 0);
                setCanEquip(ConfigZombiePlayers.pickupLoot);
                this.clearInventory();

                particle = EnumParticleTypes.SPELL_WITCH;
            } else if (itemstack.getItem() == Items.BED) {
                if (calmTime > 0) {
                    if (this.hasHome()) {
                        if (this.getMaximumHomeDistance() == ConfigZombiePlayersAdvanced.stayNearHome_range1) {
                            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 1.0D, this.posZ,
                                    particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                            this.setHomePosAndDistance(getPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range2);
                        } else if (this.getMaximumHomeDistance() == ConfigZombiePlayersAdvanced.stayNearHome_range2) {
                            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 1.5D, this.posZ,
                                    particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                            this.setHomePosAndDistance(getPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range3);
                        } else if (this.getMaximumHomeDistance() == ConfigZombiePlayersAdvanced.stayNearHome_range3) {
                            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.REDSTONE, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                                    particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                            this.setHomePosAndDistance(BlockPos.ORIGIN, -1);
                        } else {
                            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                                    particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                            this.setHomePosAndDistance(getPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1);
                        }

                    } else {
                        ((WorldServer) this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                                particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                        this.setHomePosAndDistance(getPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1);
                    }
                }


            } else if (isRawMeat(itemstack)) {
                itemUsed = true;

                ateCalmingItem();

                particle = null;
            }

            if (itemUsed) {
                if (particle != null) {
                    ((WorldServer) this.world).spawnParticle(particle, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                            particleCount, 0.3D, 0D, 0.3D, 1D, 0);
                    world.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
                }

                this.consumeItemFromStack(player, itemstack);
                return true;
            }
        }

        return super.processInteract(player, hand);
    }

    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack)
    {
        if (!player.capabilities.isCreativeMode)
        {
            stack.shrink(1);
        }
    }

    @Override
    public void setFire(int seconds) {
        //super.setFire(seconds);
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {

        if (gameProfile == null) {
            GameProfile profile;
            if (Zombie_Players.zombiePlayerNames != null && Zombie_Players.zombiePlayerNames.length > 0) {
                profile = new GameProfile(null, Zombie_Players.zombiePlayerNames[world.rand.nextInt(Zombie_Players.zombiePlayerNames.length-1)]);
            } else {
                profile = new GameProfile(null, "Corosus");
            }
            setGameProfile(profile);
        }

        //make sure super run before our overrides
        IEntityLivingData data = super.onInitialSpawn(difficulty, livingdata);

        this.clearInventory();
        this.setChild(false);
        setCanEquip(ConfigZombiePlayers.pickupLoot);

        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16F);
        this.getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20);

        return data;
    }

    public void clearInventory() {
        for (EntityEquipmentSlot entityequipmentslot : EntityEquipmentSlot.values()) {
            this.setItemStackToSlot(entityequipmentslot, ItemStack.EMPTY);
        }
    }

    public void setCanEquip(boolean pickupLoot) {
        this.setCanPickUpLoot(pickupLoot);
        Arrays.fill(this.inventoryArmorDropChances, pickupLoot ? 1F : 0F);
        Arrays.fill(this.inventoryHandsDropChances, pickupLoot ? 1F : 0F);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        String playerName = compound.getString("playerName");
        String playerUUID = compound.getString("playerUUID");
        gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);

        if (compound.hasKey("home_X")) {
            this.setHomePosAndDistance(new BlockPos(compound.getInteger("home_X"), compound.getInteger("home_Y"), compound.getInteger("home_Z")), (int)compound.getFloat("home_Dist"));
        }

        risingTime = compound.getInteger("risingTime");
        spawnedFromPlayerDeath = compound.getBoolean("spawnedFromPlayerDeath");
        quiet = compound.getBoolean("quiet");
        calmTime = compound.getInteger("calmTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (compound != null) {
            compound.setString("playerName", gameProfile.getName());
            compound.setString("playerUUID", gameProfile.getId() != null ? gameProfile.getId().toString() : "");
        }

        compound.setInteger("home_X", getHomePosition().getX());
        compound.setInteger("home_Y", getHomePosition().getY());
        compound.setInteger("home_Z", getHomePosition().getZ());
        compound.setFloat("home_Dist", getMaximumHomeDistance());

        compound.setInteger("risingTime", risingTime);
        compound.setBoolean("spawnedFromPlayerDeath", spawnedFromPlayerDeath);
        compound.setBoolean("quiet", quiet);
        compound.setInteger("calmTime", calmTime);

        return super.writeToNBT(compound);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(risingTime);
        if (gameProfile != null) {
            ByteBufUtils.writeUTF8String(buffer, gameProfile.getName());
            ByteBufUtils.writeUTF8String(buffer, gameProfile.getId() != null ? gameProfile.getId().toString() : "");
        }
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        try {
            risingTime = additionalData.readInt();
            String playerName = ByteBufUtils.readUTF8String(additionalData);
            String playerUUID = ByteBufUtils.readUTF8String(additionalData);
            gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);
        } catch (Exception ex) {
            //just log simple message and debug if needed
            CULog.dbg("exception for EntityZombiePlayer.readSpawnData: " + ex.toString());
            //ex.printStackTrace();
        }
    }

    @Override
    public ITextComponent getDisplayName() {
        if (hasCustomName()) {
            return new TextComponentString(getCustomNameTag());
        } else {
            return new TextComponentString("Zombie " + gameProfile.getName());
        }
    }

    public void setPlaying(boolean playing)
    {
        this.isPlaying = playing;
    }

    public boolean isPlaying()
    {
        return this.isPlaying;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player)
    {
        return !this.getLeashed();
    }

    public int getCalmTime() {
        return calmTime;
    }

    public void setCalmTime(int calmTime) {
        this.calmTime = calmTime;
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        boolean result = super.attackEntityAsMob(entityIn);

        if (!world.isRemote && entityIn instanceof EntityLivingBase) {
            if (((EntityLivingBase) entityIn).getHealth() <= 0) {
                heal((float)ConfigZombiePlayersAdvanced.healPerKill);
                ((WorldServer)this.world).spawnParticle(EnumParticleTypes.HEART, false, this.posX, this.posY + this.getEyeHeight() + 0.5D, this.posZ,
                        1, 0.3D, 0D, 0.3D, 1D, 0);
                world.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
            } else {
                heal((float)ConfigZombiePlayersAdvanced.healPerHit);
            }
        }

        return result;
    }

    public boolean isFoodNeedUrgent() {
        if (getCalmTime() < 20 * 60 * 5 ||
                isHealthLow()) {
            return true;
        }
        return false;
    }

    public boolean isHealthLow() {
        return getHealth() <= getMaxHealth() - 5;
    }
}
