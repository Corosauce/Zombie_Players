package com.corosus.zombie_players.entity;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.ai.EntityAIInteractChest;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;

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

    public EntityZombiePlayer(World worldIn) {
        super(worldIn);
        ((PathNavigateGround) this.getNavigator()).setBreakDoors(ConfigZombiePlayers.opensDoors);
    }

    @Override
    protected void initEntityAI() {
        //super.initEntityAI();

        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIZombieAttack(this, 1.0D, false));
        this.tasks.addTask(4, new EntityAIOpenDoor(this, false));
        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));

        this.tasks.addTask(6, new EntityAIInteractChest(this, 1.0D, 20));

        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.applyEntityAI();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (risingTime < risingTimeMax) risingTime++;

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

        //temp
        if (gameProfile == null) {
            GameProfile profile;
            if (Zombie_Players.zombiePlayerNames != null && Zombie_Players.zombiePlayerNames.length > 0) {
                profile = new GameProfile(null, Zombie_Players.zombiePlayerNames[world.rand.nextInt(Zombie_Players.zombiePlayerNames.length)]);
            } else {
                profile = new GameProfile(null, "Corosus");
            }
            setGameProfile(profile);
        }

        this.setChild(false);

        return super.onInitialSpawn(difficulty, livingdata);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        String playerName = compound.getString("playerName");
        String playerUUID = compound.getString("playerUUID");
        gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);

        risingTime = compound.getInteger("risingTime");
        spawnedFromPlayerDeath = compound.getBoolean("spawnedFromPlayerDeath");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (compound != null) {
            compound.setString("playerName", gameProfile.getName());
            compound.setString("playerUUID", gameProfile.getId() != null ? gameProfile.getId().toString() : "");
        }

        compound.setInteger("risingTime", risingTime);
        compound.setBoolean("spawnedFromPlayerDeath", spawnedFromPlayerDeath);

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
        return new TextComponentString("Zombie " + gameProfile.getName());
    }
}
