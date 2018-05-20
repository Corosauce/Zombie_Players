package com.corosus.zombie_players.entity;

import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.ai.EntityAIInteractChest;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
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
        }
        return zombie;
    }

    public static EntityZombiePlayer spawnInPlaceOfPlayer(World world, double x, double y, double z, GameProfile profile) {
        EntityZombiePlayer zombie = new EntityZombiePlayer(world);
        zombie.setPosition(x, y, z);
        zombie.setGameProfile(profile);
        zombie.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        world.spawnEntity(zombie);
        return zombie;
    }

    public GameProfile gameProfile;

    public int risingTime = 0;
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
            String[] names = new String[] {"PhoenixfireLune", "Cojomax99", "aidancbrady", "AlgorithmX2",
                    "amadornes"/*, "Amazig Jj"*/, "Aroma1997", "asie", "azanor", "Benimatic", "Blusunrize", "boni",
                    "blood", "brandon3055", "Buuz135", "chicken_bones", "CofH", "Corosus", "CovertJaguar", "cpw",
                    "CrazyPants", "dan200", "Darkhax", "DeflatedPickle", "Dinnerbone", "direwolf20", "Dockter",
                    "DoodleFungus", "Drullkus", "Eladkay", "Eloraam", "Elucent", "Emoniph", "Etho", "EmosewaGamer",
                    "FireBall1725", "Forecaster", "gabizou", "Glasspelican", "Glitchfiend", "greenphelm", "GWSheridan",
                    "HellFirePVP", "HyperionNexus", "iChun", "InsomniaKitten", "JamiesWhiteShirt", "jaquadro", "Jared",
                    "jeb", "kashike", "KingLemming", "LatvianModder", "LexManos", "LordSaad", "McJty", "mDiyo", "mezz",
                    "modmuss50", "NillerUdenDild", "Pam", "player", "Poke", "ProfessorProspector", "ProfMobius", "raphy",
                    "Reika", "rubensworks", "RWTema", "Sangar", "Shadowfacts", "Shadows_of_Fire", "SimeonRadivoev", "slowpoke",
                    "sokratis12GR", "SpitefulFox", "srs_bsns", "Tamaized", "techbrew", "TehNut", "TheCodedOne", "TheRealp455w0rd",
                    "tterrag", "Vazkii", "WayofTime", "wiiv", "wiresegal", "Xisuma", "Zidane"};
            profile = new GameProfile(null, names[world.rand.nextInt(names.length-1)]);

            //profile = new GameProfile(null, "Scratch");

            //profile = new GameProfile(UUIDTypeAdapter.fromString("a6484c2f-cd05-460f-81d1-36e92d8f8f9e"), "Cojomax99");
            //profile = new GameProfile(null, "Cojomax99");
            //profile = new GameProfile(null, "PhoenixfireLune");
            //profile = new GameProfile(UUIDTypeAdapter.fromString("ef29f2d6-14e1-4eda-9c53-d4eac41c0062"), "PhoenixfireLune");
            //profile = new GameProfile(null, "chicken_bones");
            //profile = new GameProfile(null, "CovertJaguar");
            //profile = new GameProfile(UUIDTypeAdapter.fromString("e0bc7f7a-0d68-4e85-bbc4-8bd17e52e9e5"), "Corosus");
            setGameProfile(profile);
        }

        this.setChild(false);
        this.enablePersistence();

        return super.onInitialSpawn(difficulty, livingdata);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        String playerName = compound.getString("playerName");
        String playerUUID = compound.getString("playerUUID");
        gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);

        risingTime = compound.getInteger("risingTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (compound != null) {
            compound.setString("playerName", gameProfile.getName());
            compound.setString("playerUUID", gameProfile.getId() != null ? gameProfile.getId().toString() : "");
        }

        compound.setInteger("risingTime", risingTime);

        return super.writeToNBT(compound);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        if (gameProfile != null) {
            ByteBufUtils.writeUTF8String(buffer, gameProfile.getName());
            ByteBufUtils.writeUTF8String(buffer, gameProfile.getId() != null ? gameProfile.getId().toString() : "");
        }
        buffer.writeInt(risingTime);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        try {
            String playerName = ByteBufUtils.readUTF8String(additionalData);
            String playerUUID = ByteBufUtils.readUTF8String(additionalData);
            gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        risingTime = additionalData.readInt();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("Zombie " + gameProfile.getName());
    }
}
