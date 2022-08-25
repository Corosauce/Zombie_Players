package com.corosus.zombie_players.entity;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

import com.corosus.coroutil.util.CULog;
import com.corosus.coroutil.util.CoroUtilEntity;
import com.corosus.zombie_players.EntityRegistry;
import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.ai.*;
import com.mojang.authlib.GameProfile;
import com.mojang.math.Vector3f;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class ZombiePlayer extends Zombie implements IEntityAdditionalSpawnData, OwnableEntity, ContainerListener {
   private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = (p_34284_) -> {
      return p_34284_ == Difficulty.HARD;
   };
   private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE);
   private boolean canBreakDoors;
   private int inWaterTime;
   public boolean spawnedFromPlayerDeath = false;
   public GameProfile gameProfile;
   public int risingTime = -20;
   public int risingTimeMax = 40;
   public boolean quiet = false;
   public boolean canEatFromChests = false;
   public boolean shouldFollowOwner = false;
   private boolean isPlaying;
   private int calmTime = 0;
   public List<BlockPos> listPosChests = new ArrayList<>();
   public static int MAX_CHESTS = 4;
   public String ownerName = "";
   public WeakReference<Player> playerWeakReference = new WeakReference<>(null);
   public boolean hasOpenedChest = false;
   public BlockPos posChestUsing = null;
   public int chestUseTime = 0;
   public BlockPos homePositionBackup = null;
   public int homeDistBackup = -1;
   public long lastTimeStartedPlaying = 0;
   public int calmTicksVeryLow = 100;
   public int calmTicksLow = 20 * 90;
   protected SimpleContainer inventory;
   private WorkInfo workInfo = new WorkInfo();
   private boolean isDepositingInChest = false;

   public ZombiePlayer(EntityType<ZombiePlayer> entityEntityType, Level level) {
      super(entityEntityType, level);
      ((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
      this.createInventory();
   }

   public static ZombiePlayer spawnInPlaceOfPlayer(Player player) {
      boolean spawn = true;
      ZombiePlayer zombie = null;
      if (player.getSleepingPos().get() != null) {
         if (player.getSleepingPos().get().distSqr(new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()))) <
                 ConfigZombiePlayers.distanceFromPlayerSpawnPointToPreventZombieSpawn * ConfigZombiePlayers.distanceFromPlayerSpawnPointToPreventZombieSpawn) {
            spawn = false;
         }
      }
      if (spawn) {

         if (player.level instanceof ServerLevelAccessor) {
            zombie = spawnInPlaceOfPlayer(player.level, player.getX(), player.getY(), player.getZ(), player.getGameProfile());
            if (player.getSleepingPos().get() != null) {
               zombie.setHomePosAndDistance(player.getSleepingPos().get(), 16, true);
            }
         }
      }
      return zombie;
   }

   public static ZombiePlayer spawnInPlaceOfPlayer(Level world, double x, double y, double z, GameProfile profile) {
         ZombiePlayer zombie = new ZombiePlayer(EntityRegistry.zombie_player, world);
         zombie.setPos(x, y, z);
         zombie.setGameProfile(profile);
         zombie.finalizeSpawn((ServerLevelAccessor) world, world.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.NATURAL, (SpawnGroupData)null, (CompoundTag)null);
         zombie.setPersistenceRequired();
         zombie.spawnedFromPlayerDeath = true;
         world.addFreshEntity(zombie);
         return zombie;
   }

   private static com.google.common.base.Predicate<Entity> VISIBLE_MOB_SELECTOR = p_apply_1_ -> !p_apply_1_.isInvisible();

   public static com.google.common.base.Predicate<Entity> ENEMY_PREDICATE = p_apply_1_ -> p_apply_1_ != null &&
           VISIBLE_MOB_SELECTOR.apply(p_apply_1_) &&
           (p_apply_1_ instanceof Enemy) &&
           !(p_apply_1_ instanceof Creeper) &&
           (!(p_apply_1_ instanceof ZombiePlayer) || (p_apply_1_ instanceof ZombiePlayer && ((ZombiePlayer) p_apply_1_).calmTime == 0 && ((ZombiePlayer) p_apply_1_).getTarget() != null));

   /*public ZombiePlayerNew(Level p_34274_) {
      this(EntityRegistry.zombie_player, p_34274_);
   }*/

   /*public ZombiePlayerNew(EntityType<ZombiePlayerNew> entityEntityType, Level level) {
      super(entityEntityType, level);
   }*/



   protected void registerGoals() {

      int taskID = 0;

      /*//this.goalSelector.addGoal(4, new ZombiePlayerNew.ZombieAttackTurtleEggGoal(this, 1.0D, 3));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      //this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
      //this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0D, true, 4, this::canBreakDoors));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
      //this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      //this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      //this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      //this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));*/

      this.goalSelector.addGoal(taskID++, new FloatGoal(this));
      this.goalSelector.addGoal(taskID++, new EntityAITemptZombie(this, 1.2D, false));
      this.goalSelector.addGoal(taskID++, new EntityAIAvoidEntityOnLowHealth(this, LivingEntity.class, ENEMY_PREDICATE,
              16.0F, 1.4D, 1.4D, 10F));
      this.goalSelector.addGoal(taskID++, new EntityAIEatToHeal(this));
      this.goalSelector.addGoal(taskID++, new EntityAIZombieAttackWeaponReach(this, 1.0D, false));
      this.goalSelector.addGoal(taskID++, new EntityAIMoveToWantedNearbyItems(this, 1.15D));
      this.goalSelector.addGoal(taskID++, new OpenDoorGoal(this, false));
      this.goalSelector.addGoal(taskID++, new EntityAIFollowOwnerZombie(this, 1.2D, 10.0F, 2.0F));
      this.goalSelector.addGoal(taskID++, new EntityAIMoveTowardsRestrictionZombie(this, 1.0D) {});
      this.goalSelector.addGoal(taskID++, new EntityAIWorkTrainingMode(this, 1.2D, false));

      this.goalSelector.addGoal(taskID++, new EntityAIInteractChest(this, 1.0D, 20));
      this.goalSelector.addGoal(taskID++, new EntityAIWorkKeepItemInHandAndResupply(this));
      this.goalSelector.addGoal(taskID++, new EntityAIWorkInArea(this));
      this.goalSelector.addGoal(taskID++, new EntityAIWorkMoveToWantedNearbyItems(this, 1.0D));
      this.goalSelector.addGoal(taskID++, new EntityAIWorkDepositPickupsInChest(this));
      this.goalSelector.addGoal(taskID++, new EntityAIPlayZombiePlayer(this, 1.15D));
      this.goalSelector.addGoal(taskID++, new WaterAvoidingRandomStrollGoal(this, 1.0D));

      this.goalSelector.addGoal(taskID, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(taskID, new LookAtPlayerGoal(this, ZombiePlayer.class, 8.0F));
      this.goalSelector.addGoal(taskID, new RandomLookAroundGoal(this));

      //this.targetSelector.addGoal(1, new EntityAIHurtByTarget(this, false, new Class[] {EntityPigZombie.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoalIfCalm(this, Player.class, 10,true, true, null, true));

      this.targetSelector.addGoal(3, new NearestAttackableTargetGoalIfCalm(this, Mob.class, 0, true, false, ENEMY_PREDICATE, false));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 35.0D).add(Attributes.MOVEMENT_SPEED, (double)0.23F).add(Attributes.ATTACK_DAMAGE, 3.0D).add(Attributes.ARMOR, 2.0D).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
   }

   public boolean canBreakDoors() {
      return this.canBreakDoors;
   }

   public void setCanBreakDoors(boolean p_34337_) {
      if (this.supportsBreakDoorGoal() && GoalUtils.hasGroundPathNavigation(this)) {
         if (this.canBreakDoors != p_34337_) {
            this.canBreakDoors = p_34337_;
            ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(p_34337_);
            if (p_34337_) {
               this.goalSelector.addGoal(1, this.breakDoorGoal);
            } else {
               this.goalSelector.removeGoal(this.breakDoorGoal);
            }
         }
      } else if (this.canBreakDoors) {
         this.goalSelector.removeGoal(this.breakDoorGoal);
         this.canBreakDoors = false;
      }
   }

   protected boolean supportsBreakDoorGoal() {
      return true;
   }

   protected int getExperienceReward(Player p_34322_) {
      if (this.isBaby()) {
         this.xpReward = (int)((double)this.xpReward * 2.5D);
      }

      return super.getExperienceReward(p_34322_);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> p_34307_) {

      super.onSyncedDataUpdated(p_34307_);
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }



   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      
      UUID uuid = new UUID(0, 0);

      if (!level.isClientSide() && player != null && hand == InteractionHand.MAIN_HAND) {
         ItemStack itemstack = player.getItemInHand(hand);

         boolean itemUsed = false;
         int particleCount = 5;
         SimpleParticleType particle = null;

         if (isCalm() && itemstack.getItem() == Items.APPLE) {
            itemUsed = true;
            quiet = !quiet;
            if (quiet) {
               player.sendMessage(new TextComponent("Zombie Player quieted"), uuid);
            } else {
               player.sendMessage(new TextComponent("Zombie Player unquieted"), uuid);
            }

            setSilent(quiet);

            particle = ParticleTypes.NOTE;
         } else if (isCalm() && itemstack.getItem() == Items.GLASS_BOTTLE) {
            itemUsed = true;
            this.setBaby(!this.isBaby());

            particle = ParticleTypes.WITCH;
         } else if (isCalm() && itemstack.getItem() == Items.SPIDER_EYE) {
            itemUsed = true;
            player.sendMessage(new TextComponent("Set Zombie Player Owner to " + player.getName().getString()), uuid);
            setOwnerName(player.getName().getString());

            particle = ParticleTypes.WITCH;
         } else if (isCalm() && itemstack.getItem() == Item.BY_BLOCK.get(Blocks.CHEST)) {
            //itemUsed = true;
            this.setCanEatFromChests(!this.canEatFromChests);
            if (this.canEatFromChests) {
               player.sendMessage(new TextComponent("Set to eat food from chests"), uuid);
            } else {
               player.sendMessage(new TextComponent("Set to not eat food from chests"), uuid);
            }


            particle = this.canEatFromChests ? ParticleTypes.HEART : ParticleTypes.WITCH;
         } else if (isCalm() && itemstack.getItem() == Items.ROTTEN_FLESH) {
            itemUsed = true;

            player.sendMessage(new TextComponent("Dropped all Equipment"), uuid);
            this.dropCustomDeathLoot(DamageSource.IN_WALL, 0, true);
            //this.dropEquipment(true, 0);
            this.clearInventory();

            particle = ParticleTypes.WITCH;
         } else if (itemstack.getItem() instanceof BedItem) {
            if (isCalm()) {
               particle = null;
               if (this.isLeashed()) {
                  ((ServerLevel)this.level).sendParticles(DustParticleOptions.REDSTONE, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                  player.sendMessage(new TextComponent("Can't set home while leashed"), uuid);
               } else {
                  if (this.hasRestriction()) {
                     if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range1) {
                        ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range2, true);
                        player.sendMessage(new TextComponent("Home set to current position with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range2), uuid);
                     } else if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range2) {
                        ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range3, true);
                        player.sendMessage(new TextComponent("Home set to current position with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range3), uuid);
                     } else if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range3) {
                        ((ServerLevel)this.level).sendParticles(DustParticleOptions.REDSTONE, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                        this.setHomePosAndDistance(BlockPos.ZERO, -1, true);
                        player.sendMessage(new TextComponent("Home removed"), uuid);
                     } else {
                        ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1, true);
                        player.sendMessage(new TextComponent("Home set to current position with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range1), uuid);
                     }

                  } else {
                     ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                     this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1, true);
                     player.sendMessage(new TextComponent("Home set to current position with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range1), uuid);
                  }
               }

            }


         } else if (isRawMeat(itemstack) && (getHealth() < getMaxHealth() || getOwner() == null || isCalmTimeLow())) {
            itemUsed = true;

            if (!hasOwner()) {
               player.sendMessage(new TextComponent("Zombie Player Tamed"), uuid);
               setOwnerName(player.getName().getString());

            }
            ateCalmingItem();

            particle = null;
         } else if (itemstack.getItem() == Items.DIAMOND_HOE) {
            player.sendMessage(new TextComponent("Set work center"), uuid);
            getWorkInfo().setPosWorkCenter(blockPosition());
         } else if (itemstack.getItem() == Items.GOLDEN_HOE) {
            getWorkInfo().setInTrainingMode(!getWorkInfo().isInTrainingMode());
            if (getWorkInfo().isInTrainingMode()) {
               getWorkInfo().setStateWorkLastObserved(Blocks.AIR.defaultBlockState());
               player.sendMessage(new TextComponent("Training Zombie Player"), uuid);
            } else {
               if (getWorkInfo().getStateWorkLastObserved().isAir()) {
                  player.sendMessage(new TextComponent("Training ended, no work set"), uuid);
                  getWorkInfo().setPerformWork(false);
               } else {
                  player.sendMessage(new TextComponent("Training ended, work starting"), uuid);
                  getWorkInfo().setPerformWork(true);
                  restrictTo(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1);
               }

            }

         } else if (isCalm() && itemstack.isEmpty()) {
            shouldFollowOwner = !shouldFollowOwner;

            particle = this.shouldFollowOwner ? ParticleTypes.HEART : ParticleTypes.WITCH;

            if (shouldFollowOwner) {
               player.sendMessage(new TextComponent("Following"), uuid);
               this.restrictTo(null, -1);
            } else {
               player.sendMessage(new TextComponent("Wandering"), uuid);
               this.restrictTo(homePositionBackup, homeDistBackup);
            }
         }

         if (particle != null) {
            ((ServerLevel)this.level).sendParticles(particle, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
            level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
         }

         if (itemUsed) {

            this.consumeItemFromStack(player, itemstack);
            return InteractionResult.CONSUME;
         }
      }

      return super.mobInteract(player, hand);
   }

   public void particle(float r, float g, float b, float x, float y, float z) {
      ((ServerLevel)this.level).sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 1f), x, y, z, 1, 0.3D, 0D, 0.3D, 1D);
   }

   public void tick() {
      if (risingTime < risingTimeMax) risingTime++;

      //calmTime = this.calmTicksLow;

      if (!level.isClientSide()) {
         if (calmTime > 0) {

            boolean debugState = true;

            if (debugState && level.getGameTime() % 1 == 0) {
               for (int x = -getWorkDistance(); x < getWorkDistance(); x++) {
                  for (int z = -getWorkDistance(); z < getWorkDistance(); z++) {
                     float dist = (float) getWorkInfo().getPosWorkCenter().distSqr(new BlockPos(getWorkInfo().getPosWorkCenter().getX() + x, getWorkInfo().getPosWorkCenter().getY() + 0.5, getWorkInfo().getPosWorkCenter().getZ() + z));
                     if (dist > (getWorkDistance()-1) * (getWorkDistance()-1) && dist < getWorkDistance() * getWorkDistance()) {
                        particle(0, 0, 1, getWorkInfo().getPosWorkCenter().getX() + x, getWorkInfo().getPosWorkCenter().getY() + 1.5F, getWorkInfo().getPosWorkCenter().getZ() + z);
                     }

                     //((ServerLevel)entityObj.level).sendParticles(new DustParticleOptions(new Vector3f(0f, 0f, 1f), 1f), entityObj.getX(), entityObj.getY() + 1.5, entityObj.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
                  }
               }
            }

            if (chestUseTime > 0 && !isDepositingInChest()) {
               chestUseTime--;
               if (chestUseTime == 0) {
                  if (posChestUsing != null) {
                     closeChest(posChestUsing);
                  }
               }
            }

            //getWorkInfo().setPerformWork(true);

            if (calmTime <= this.calmTicksVeryLow && calmTime % 10 == 0) {
               this.playSound(SoundEvents.ZOMBIE_INFECT, getSoundVolume(), getVoicePitch());
               ((ServerLevel)this.level).sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
            } else if (calmTime <= this.calmTicksLow && calmTime % (20 * 5) == 0) {
               this.playSound(SoundEvents.ZOMBIE_VILLAGER_AMBIENT, getSoundVolume(), getVoicePitch());
               ((ServerLevel)this.level).sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
            }

            if (isHealthLow()) {
               ((ServerLevel)this.level).sendParticles(DustParticleOptions.REDSTONE, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
            }
            if (isCalmTimeLow()) {
               ((ServerLevel)this.level).sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
            }

            calmTime--;

            if (calmTime == 0) {
               onBecomeHostile();
            }
         }

         if (level.getGameTime() % ConfigZombiePlayersAdvanced.heal1HealthPerXTicks == 0) {
            this.heal(1);
         }

         //pickup items we want, slow rate if pickup if well fed so others more hungry grab it first
         if (isFoodNeedUrgent() || (!ConfigZombiePlayersAdvanced.onlySeekFoodIfNeeded && level.getGameTime() % 20 == 0)) {
            for (ItemEntity entityitem : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D))) {
               if (entityitem.isAlive() && !entityitem.getItem().isEmpty() && !entityitem.hasPickUpDelay() && isItemWeWant(entityitem.getItem())) {
                  //this.updateEquipmentIfNeeded(entityitem);
                  this.onItemPickup(entityitem/*, entityitem.getItem().getCount()*/);
                  entityitem.kill();
                  ateCalmingItem();

                  //only have 1 ate a time to stagger hogging
                  break;
               }
            }
         }

         if (level.getGameTime() % 20 == 0) {
            if (isItemWeWant(getMainHandItem())) {
               for (int i = 0; i < getMainHandItem().getCount(); i++) {

                  //only do effect sounds and visuals once
                  if (i == 0) {
                     ateCalmingItem(true);
                  } else {
                     ateCalmingItem(false);
                  }

               }
               setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
         }
      }

      if (risingTime < risingTimeMax) {
         this.setNoAi(true);
         if (level.isClientSide()) {
            BlockState state = level.getBlockState(blockPosition().below());
            double speed = 0.2D;
            for (int i = 0; i < 5; i++) {
               double x1 = level.random.nextDouble() - level.random.nextDouble();
               double z1 = level.random.nextDouble() - level.random.nextDouble();
               double x2 = (level.random.nextDouble() - level.random.nextDouble()) * speed;
               double y2 = (level.random.nextDouble() - level.random.nextDouble()) * speed;
               double z2 = (level.random.nextDouble() - level.random.nextDouble()) * speed;
               this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), false, getX() + x1, getY(), getZ() + z1, 0, 0, 0);
            }
         }
      } else {
         this.setNoAi(false);
      }

      super.tick();

      //run after super to let vanilla equipment and armor grabbing go first
      if (!level.isClientSide()) {
         if (calmTime > 0) {
            //pickup other items when in work mode
            if (isCalm() && getWorkInfo().isPerformingWork() && this.isAlive() && !this.dead) {
               for (ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D))) {
                  if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay()/* && this.wantsToPickUp(itementity.getItem())*/) {
                     //inverting this so that Mob.java can equip armor, and our above code can eat food, and this will handle everything else
                     //if (!this.wantsToPickUp(itementity.getItem())) {
                        this.pickUpItemForExtraInventory(itementity);
                     //}
                  }
               }
            }
         }
      }
   }

   protected void pickUpItemForExtraInventory(ItemEntity p_35467_) {
      ItemStack itemstack = p_35467_.getItem();
      //if (this.wantsToPickUp(itemstack)) {
         SimpleContainer simplecontainer = this.getExtraInventory();
         boolean flag = simplecontainer.canAddItem(itemstack);
         if (!flag) {
            return;
         }

         this.onItemPickup(p_35467_);
         this.take(p_35467_, itemstack.getCount());
         ItemStack itemstack1 = simplecontainer.addItem(itemstack);
         if (itemstack1.isEmpty()) {
            p_35467_.discard();
         } else {
            itemstack.setCount(itemstack1.getCount());
         }
      //}

   }

   public boolean hasAnyItemsInExtra() {
      for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty()) {
            return true;
         }
      }
      return false;
   }

   public boolean hasChestToUse() {
      return listPosChests.size() > 0;
   }

   public boolean needsMoreWorkItem() {
      if (getWorkInfo().getItemNeededForWork() == ItemStack.EMPTY) return false;
      ItemStack stack = getMainHandItem();
      if (!stack.isEmpty()) return false;
      //if (getMainHandItem() != ItemStack.EMPTY) return false;
      return true;
   }

   public boolean hasNeededWorkItemInExtra() {
      for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty()) {
            if (itemstackMatches(itemstack, getWorkInfo().getItemNeededForWork())) {
               return true;
            }
         }
      }
      return false;
   }

   public BlockPos getNearestChestWithNeededWorkItem() {
      Iterator<BlockPos> it = listPosChests.iterator();
      double closestDist = Double.MAX_VALUE;
      BlockPos closestPos = BlockPos.ZERO;
      while (it.hasNext()) {
         BlockPos pos = it.next();
         if (chestHasNeededWorkItem(pos)) {
            double dist = blockPosition().distSqr(pos);

            if (dist < closestDist) {
               closestDist = dist;
               closestPos = pos;
            }
         }
      }
      return closestPos;
   }

   public boolean chestHasNeededWorkItem(BlockPos pos) {
      BlockEntity tile = level.getBlockEntity(pos);
      if (tile instanceof ChestBlockEntity && tile instanceof Container) {
         Container inv = (Container) tile;
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (itemstackMatches(stack, getWorkInfo().getItemNeededForWork())) {
               return true;
            }
         }
      }
      return false;
   }

   public BlockPos getClosestChestPosWithSpace() {
      Iterator<BlockPos> it = listPosChests.iterator();
      double closestDist = Double.MAX_VALUE;
      BlockPos closestPos = null;
      while (it.hasNext()) {
         BlockPos pos = it.next();
         if (isValidChestForWork(pos, false)) {
            double dist = blockPosition().distSqr(pos);

            if (dist < closestDist) {
               closestDist = dist;
               closestPos = pos;
            }
         }
      }

      return closestPos;
   }

   /**
    * Checks for any empty or partially full slots that we can merge into
    *
    * @param pos
    * @return
    */
   public boolean chestHasRoom(BlockPos pos) {
      BlockEntity tile = level.getBlockEntity(pos);
      if (tile instanceof ChestBlockEntity && tile instanceof Container) {
         Container inv = (Container) tile;
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
               return true;
            } else {
               if (stack.getCount() < stack.getMaxStackSize()) {
                  for(int ii = 0; ii < getExtraInventory().getContainerSize(); ++ii) {
                     if (!getExtraInventory().getItem(ii).isEmpty()) {
                        if (canMergeItems(getExtraInventory().getItem(ii), stack)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   public static boolean itemstackMatches(ItemStack item1, ItemStack item2) {
      if (!item1.is(item2.getItem())) {
         return false;
      } else if (item1.getDamageValue() != item2.getDamageValue()) {
         return false;/*
      } else if (mergeFrom.getCount() > mergeFrom.getMaxStackSize()) {
         return false;*/
      } else {
         return ItemStack.tagMatches(item1, item2);
      }
   }

   public static boolean canMergeItems(ItemStack mergeFrom, ItemStack mergeInto) {
      if (!mergeFrom.is(mergeInto.getItem())) {
         return false;
      } else if (mergeFrom.getDamageValue() != mergeInto.getDamageValue()) {
         return false;
      } else if (mergeFrom.getCount() > mergeFrom.getMaxStackSize()) {
         return false;
      } else {
         return ItemStack.tagMatches(mergeFrom, mergeInto);
      }
   }

   public Container getChest(BlockPos pos) {
      BlockEntity tile = level.getBlockEntity(pos);
      if (tile instanceof ChestBlockEntity && tile instanceof Container) {
         return (Container) tile;
      }
      return null;
   }

   public boolean takeUpTo1StackOfWorkItemFromChest(BlockPos pos) {
      Container container = getChest(pos);
      return takeUpTo1StackOfWorkItemFromContainer(container);
   }

   public boolean takeUpTo1StackOfWorkItemFromContainer(Container container) {
      if (container == null) {
         return false;
      } else {
         Direction direction = Direction.UP;
         for(int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack chestItem = container.getItem(i);
            if (!chestItem.isEmpty()) {
               if (itemstackMatches(getWorkInfo().getItemNeededForWork(), chestItem)) {
                  ItemStack itemstack = chestItem.copy();
                  SimpleContainer tempContainer = new SimpleContainer(getMainHandItem());
                  ItemStack itemstack1 = HopperBlockEntity.addItem(container, tempContainer, container.removeItem(i, 64), direction);
                  if (itemstack1.isEmpty()) {
                     tempContainer.setChanged();
                  } else {
                     container.setItem(i, itemstack);
                  }

                  //swap item from temp container into main hand now that itemstack has been topped up
                  setItemInHand(InteractionHand.MAIN_HAND, tempContainer.getItem(0));

                  if (getMainHandItem().getCount() >= getMainHandItem().getMaxStackSize()) {
                     break;
                  }
               }
            }
         }

         return false;
      }
   }

   public boolean ejectItems(BlockPos pos) {
      Container container = getChest(pos);
      if (container == null) {
         return false;
      } else {
         Direction direction = Direction.UP;
         if (isFullContainer(container, direction)) {
            return false;
         } else {
            for(int i = 0; i < getExtraInventory().getContainerSize(); ++i) {
               if (!getExtraInventory().getItem(i).isEmpty()) {
                  ItemStack itemstack = getExtraInventory().getItem(i).copy();
                  ItemStack itemstack1 = HopperBlockEntity.addItem(getExtraInventory(), container, getExtraInventory().removeItem(i, 1), direction);
                  if (itemstack1.isEmpty()) {
                     container.setChanged();
                     return true;
                  }

                  getExtraInventory().setItem(i, itemstack);
               }
            }

            return false;
         }
      }
   }

   private static boolean isFullContainer(Container p_59386_, Direction p_59387_) {
      return getSlots(p_59386_, p_59387_).allMatch((p_59379_) -> {
         ItemStack itemstack = p_59386_.getItem(p_59379_);
         return itemstack.getCount() >= itemstack.getMaxStackSize();
      });
   }

   private static IntStream getSlots(Container p_59340_, Direction p_59341_) {
      return p_59340_ instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer)p_59340_).getSlotsForFace(p_59341_)) : IntStream.range(0, p_59340_.getContainerSize());
   }


   public SimpleContainer getExtraInventory() {
      return this.inventory;
   }

   public void aiStep() {
      if (isCalm()/* && (isCanEatFromChests() || workInfo.isPerformingWork())*/) {
         tickScanForChests();
      }

      super.aiStep();
   }

   protected boolean isSunSensitive() {
      return false;
   }

   public boolean hurt(DamageSource p_34288_, float p_34289_) {
      if (!super.hurt(p_34288_, p_34289_)) {
         return false;
      } else if (!(this.level instanceof ServerLevel)) {
         return false;
      } else {

         //if gets stuck in wall (trees), break block at head
         if (p_34288_ == DamageSource.IN_WALL) {
            BlockPos posHead = blockPosition().above();
            if (level.getBlockState(posHead).getDestroySpeed(level, posHead) >= 0) {
               level.destroyBlock(posHead, true);
            }

            //just head block wasnt enough, need to mimic code from isInWall()
            float f = getBbWidth() * 0.8F;
            AABB aabb = AABB.ofSize(this.getEyePosition(), (double)f, 1.0E-6D, (double)f);
            BlockPos.betweenClosedStream(aabb).anyMatch((p_201942_) -> {
               BlockState blockstate = this.level.getBlockState(p_201942_);
               if (!blockstate.isAir() && blockstate.isSuffocating(this.level, p_201942_) && Shapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level, p_201942_).move((double)p_201942_.getX(), (double)p_201942_.getY(), (double)p_201942_.getZ()), Shapes.create(aabb), BooleanOp.AND)) {
                  level.destroyBlock(p_201942_, true);
                  return true;
               }
               return false;
            });
         }

         return true;
      }
   }

   public boolean doHurtTarget(Entity entityIn) {
      boolean result = super.doHurtTarget(entityIn);

      if (!level.isClientSide() && entityIn instanceof LivingEntity) {
         if (((LivingEntity) entityIn).getHealth() <= 0 && isCalm()) {
            heal((float)ConfigZombiePlayersAdvanced.healPerKill);
            ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
            level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
         } else if (isCalm()) {
            heal((float)ConfigZombiePlayersAdvanced.healPerHit);
         }
      }

      return result;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource p_34327_) {
      return SoundEvents.ZOMBIE_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_DEATH;
   }

   protected SoundEvent getStepSound() {
      return SoundEvents.ZOMBIE_STEP;
   }

   protected void playStepSound(BlockPos p_34316_, BlockState p_34317_) {
      this.playSound(this.getStepSound(), 0.15F, 1.0F);
   }

   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   protected void populateDefaultEquipmentSlots(DifficultyInstance p_34286_) {
      super.populateDefaultEquipmentSlots(p_34286_);
      if (this.random.nextFloat() < (this.level.getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
         int i = this.random.nextInt(3);
         if (i == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
         } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
         }
      }

   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putBoolean("IsBaby", this.isBaby());
      compound.putBoolean("CanBreakDoors", this.canBreakDoors());
      compound.putInt("InWaterTime", this.isInWater() ? this.inWaterTime : -1);

      if (compound != null) {
         compound.putString("playerName", gameProfile.getName());
         compound.putString("playerUUID", gameProfile.getId() != null ? gameProfile.getId().toString() : "");
      }

      for (int i = 0; i < listPosChests.size(); i++) {
         compound.putInt("chest_" + i + "_X", listPosChests.get(i).getX());
         compound.putInt("chest_" + i + "_Y", listPosChests.get(i).getY());
         compound.putInt("chest_" + i + "_Z", listPosChests.get(i).getZ());
      }

      if (homePositionBackup != null) {
         compound.putInt("home_Backup_X", homePositionBackup.getX());
         compound.putInt("home_Backup_Y", homePositionBackup.getY());
         compound.putInt("home_Backup_Z", homePositionBackup.getZ());
         compound.putFloat("home_Backup_Dist", homeDistBackup);
      }

      if (getRestrictCenter() != null) {
         compound.putInt("home_X", getRestrictCenter().getX());
         compound.putInt("home_Y", getRestrictCenter().getY());
         compound.putInt("home_Z", getRestrictCenter().getZ());
      }
      compound.putFloat("home_Dist", getRestrictRadius());

      compound.putInt("risingTime", risingTime);
      compound.putBoolean("spawnedFromPlayerDeath", spawnedFromPlayerDeath);
      compound.putBoolean("quiet", quiet);
      compound.putBoolean("canEatFromChests", canEatFromChests);
      compound.putBoolean("shouldFollowOwner", shouldFollowOwner);
      compound.putInt("calmTime", calmTime);

      compound.putString("ownerName", ownerName);

      compound.putLong("lastTimeStartedPlaying", lastTimeStartedPlaying);

      compound.putInt("work_center_X", getWorkInfo().getPosWorkCenter().getX());
      compound.putInt("work_center_Y", getWorkInfo().getPosWorkCenter().getY());
      compound.putInt("work_center_Z", getWorkInfo().getPosWorkCenter().getZ());

      compound.put("work_blockstate", NbtUtils.writeBlockState(getWorkInfo().getStateWorkLastObserved()));

      compound.put("work_item", getWorkInfo().getItemNeededForWork().save(new CompoundTag()));

      compound.putString("work_direction", getWorkInfo().getWorkClickDirectionLastObserved().getName());

      compound.putInt("work_click", getWorkInfo().getWorkClickLastObserved().ordinal());

      compound.putBoolean("work_active", getWorkInfo().isPerformingWork());

      ListTag listtag = new ListTag();

      for(int i = 2; i < this.inventory.getContainerSize(); ++i) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty()) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putByte("Slot", (byte)i);
            itemstack.save(compoundtag);
            listtag.add(compoundtag);
         }
      }

      compound.put("Items", listtag);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.setBaby(compound.getBoolean("IsBaby"));
      this.setCanBreakDoors(compound.getBoolean("CanBreakDoors"));
      this.inWaterTime = compound.getInt("InWaterTime");

      if (compound.contains("playerName")) {
         String playerName = compound.getString("playerName");
         String playerUUID = compound.getString("playerUUID");
         gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);
      }

      for (int i = 0; i < MAX_CHESTS; i++) {
         if (compound.contains("chest_" + i + "_X")) {
            this.listPosChests.add(new BlockPos(compound.getInt("chest_" + i + "_X"),
                    compound.getInt("chest_" + i + "_Y"),
                    compound.getInt("chest_" + i + "_Z")));
         }
      }

      if (compound.contains("home_X")) {
         this.restrictTo(new BlockPos(compound.getInt("home_X"), compound.getInt("home_Y"), compound.getInt("home_Z")), (int)compound.getFloat("home_Dist"));
      }

      if (compound.contains("home_Backup_X")) {
         homePositionBackup = new BlockPos(compound.getInt("home_Backup_X"), compound.getInt("home_Backup_X"), compound.getInt("home_Backup_X"));
         homeDistBackup = (int)compound.getFloat("home_Backup_Dist");
      }

      risingTime = compound.getInt("risingTime");
      spawnedFromPlayerDeath = compound.getBoolean("spawnedFromPlayerDeath");
      quiet = compound.getBoolean("quiet");
      canEatFromChests = compound.getBoolean("canEatFromChests");
      shouldFollowOwner = compound.getBoolean("shouldFollowOwner");
      calmTime = compound.getInt("calmTime");

      ownerName = compound.getString("ownerName");

      lastTimeStartedPlaying = compound.getLong("lastTimeStartedPlaying");

      if (compound.contains("work_center_X")) {
         getWorkInfo().setPosWorkCenter(new BlockPos(compound.getInt("work_center_X"), compound.getInt("work_center_Y"), compound.getInt("work_center_Z")));
      }

      if (compound.contains("work_blockstate")) getWorkInfo().setStateWorkLastObserved(NbtUtils.readBlockState(compound.getCompound("work_blockstate")));

      if (compound.contains("work_item")) getWorkInfo().setItemNeededForWork(ItemStack.of(compound.getCompound("work_item")));
      CULog.dbg(getWorkInfo().getItemNeededForWork().toString());
      if (compound.contains("work_direction")) getWorkInfo().setWorkClickDirectionLastObserved(Direction.byName(compound.getString("work_direction")));

      if (compound.contains("work_click")) getWorkInfo().setWorkClickLastObserved(EnumTrainType.get(compound.getInt("work_click")));

      if (compound.contains("work_active")) getWorkInfo().setPerformWork(compound.getBoolean("work_active"));

      this.createInventory();
      ListTag listtag = compound.getList("Items", 10);

      for(int i = 0; i < listtag.size(); ++i) {
         CompoundTag compoundtag = listtag.getCompound(i);
         int j = compoundtag.getByte("Slot") & 255;
         if (j >= 2 && j < this.inventory.getContainerSize()) {
            this.inventory.setItem(j, ItemStack.of(compoundtag));
         }
      }

   }

   public void killed(ServerLevel p_34281_, LivingEntity p_34282_) {
      super.killed(p_34281_, p_34282_);

   }

   protected float getStandingEyeHeight(Pose p_34313_, EntityDimensions p_34314_) {
      return this.isBaby() ? 0.93F : 1.74F;
   }

   public boolean canHoldItem(ItemStack p_34332_) {
      return p_34332_.is(Items.EGG) && this.isBaby() && this.isPassenger() ? false : super.canHoldItem(p_34332_);
   }

   public boolean wantsToPickUp(ItemStack p_182400_) {
      return p_182400_.is(Items.GLOW_INK_SAC) ? false : super.wantsToPickUp(p_182400_);
   }

   @Override
   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_34297_, DifficultyInstance difficulty, MobSpawnType p_34299_, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag p_34301_) {
   //public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {

      if (gameProfile == null) {
         GameProfile profile = null;
         boolean fallback = false;

         try {
            if (Zombie_Players.zombiePlayerNames != null && Zombie_Players.zombiePlayerNames.length > 0) {
               String name = Zombie_Players.zombiePlayerNames[level.random.nextInt(Zombie_Players.zombiePlayerNames.length)];
               if (name.equals("") || name.equals(" ")) {
                  fallback = true;
               } else {
                  profile = new GameProfile(null, name);
               }
            } else {
               fallback = true;
            }
         } catch (Exception ex) {
            fallback = true;
         }

         if (fallback) {
            profile = new GameProfile(null, "Corosus");
         }

         setGameProfile(profile);
      }

      //make sure super run before our overrides
      SpawnGroupData data = super.finalizeSpawn(p_34297_, difficulty, p_34299_, livingdata, p_34301_);

      this.clearInventory();
      this.setBaby(false);
      setCanEquip(ConfigZombiePlayers.pickupLootWhenHostile);

      this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16F);
      this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);

      return data;
   }

   protected void handleAttributes(float p_34340_) {
      this.randomizeReinforcementsChance();
      this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("Random spawn bonus", this.random.nextDouble() * (double)0.05F, AttributeModifier.Operation.ADDITION));
      double d0 = this.random.nextDouble() * 1.5D * (double)p_34340_;
      if (d0 > 1.0D) {
         this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random zombie-spawn bonus", d0, AttributeModifier.Operation.MULTIPLY_TOTAL));
      }

      if (this.random.nextFloat() < p_34340_ * 0.05F) {
         this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(new AttributeModifier("Leader zombie bonus", this.random.nextDouble() * 0.25D + 0.5D, AttributeModifier.Operation.ADDITION));
         this.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("Leader zombie bonus", this.random.nextDouble() * 3.0D + 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
         this.setCanBreakDoors(this.supportsBreakDoorGoal());
      }

   }

   protected void randomizeReinforcementsChance() {
      this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(this.random.nextDouble() * net.minecraftforge.common.ForgeConfig.SERVER.zombieBaseSummonChance.get());
   }

   public double getMyRidingOffset() {
      return this.isBaby() ? 0.0D : -0.45D;
   }

   protected void dropCustomDeathLoot(DamageSource p_34291_, int p_34292_, boolean p_34293_) {
      super.dropCustomDeathLoot(p_34291_, p_34292_, p_34293_);
   }

   protected ItemStack getSkull() {
      return new ItemStack(Items.ZOMBIE_HEAD);
   }

   public WorkInfo getWorkInfo() {
      return workInfo;
   }

   public void setWorkInfo(WorkInfo workInfo) {
      this.workInfo = workInfo;
   }

   public static class ZombieGroupData implements SpawnGroupData {
      public final boolean isBaby;
      public final boolean canSpawnJockey;

      public ZombieGroupData(boolean p_34357_, boolean p_34358_) {
         this.isBaby = p_34357_;
         this.canSpawnJockey = p_34358_;
      }
   }
   
   @Override
   public Component getDisplayName() {
      if (hasCustomName()) {
         return getCustomName();
      } else {
         if (gameProfile != null) {
            return new TextComponent("Zombie " + gameProfile.getName());
         } else {
            return new TextComponent("Zombie " + "???????");
         }
      }
   }

   @Override
   public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
      //cancel player targetting if calm
      if (calmTime > 0 && entitylivingbaseIn instanceof Player) {
         super.setTarget(null);
         //usefull until zombieawareness update that patches this better
         this.getNavigation().stop();
         //cancel other AI making us target other zombie players, mainly the call for help logic from EntityAIHurtByTarget
         //also only if that other zombie player is calm too
      } else if (calmTime > 0 && entitylivingbaseIn instanceof ZombiePlayer && ((ZombiePlayer) entitylivingbaseIn).calmTime > 0) {
         //super.setAttackTarget(null);
      } else {
         super.setTarget(entitylivingbaseIn);
      }
   }

   /**
    * new methods
    * **/

   protected void consumeItemFromStack(Player player, ItemStack stack)
   {
      if (!player.isCreative())
      {
         stack.shrink(1);
      }
   }

   public void onBecomeCalm() {
      setCanEquip(ConfigZombiePlayers.pickupLootWhenCalm);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28D);
      this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1F);
   }

   public void onBecomeHostile() {
      setCanEquip(ConfigZombiePlayers.pickupLootWhenHostile);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.23D);
      this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0F);
   }

   public boolean isItemWeWant(ItemStack stack) {
      return isRawMeat(stack);
   }

   public void ateCalmingItem() {
      ateCalmingItem(true);
   }

   public void ateCalmingItem(boolean effect) {

      if (!isCalm()) {
         onBecomeCalm();
      }

      if (!hasOwner()) {
         Player player = level.getNearestPlayer(this, 16);
         if (player != null) {
            setOwnerName(player.getName().getString());
         }
      }

      this.calmTime += ConfigZombiePlayersAdvanced.calmTimePerUse;

      this.setTarget(null);
      //this.setRevengeTarget(null);

      this.heal((float) ConfigZombiePlayersAdvanced.healPerUse);
      if (effect) {
         ((ServerLevel)this.level).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
         level.playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
      }
   }

   public boolean isCalm() {
      return calmTime > 0;
   }

   public boolean isRawMeat(ItemStack stack) {
      Item item = stack.getItem();
      return Zombie_Players.listCalmingItems.contains(item);
   }

   public GameProfile getGameProfile() {
      return gameProfile;
   }

   public void setGameProfile(GameProfile gameProfile) {
      this.gameProfile = gameProfile;
   }
   
   public void clearInventory() {
      for (EquipmentSlot entityequipmentslot : EquipmentSlot.values()) {
         this.setItemSlot(entityequipmentslot, ItemStack.EMPTY);
      }
   }

   public void setCanEquip(boolean pickupLoot) {
      this.setCanPickUpLoot(pickupLoot);
        /*Arrays.fill(this.inventoryArmorDropChances, pickupLoot ? 1F : 0F);
        Arrays.fill(this.inventoryHandsDropChances, pickupLoot ? 1F : 0F);*/
      //play it safe and make things always droppable
      //case: picked up important item while loot on, died while loot off, item lost forever
      Arrays.fill(this.armorDropChances, 1F);
      Arrays.fill(this.handDropChances, 1F);
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
   public boolean canBeLeashed(Player player)
   {
      return !this.isLeashed();
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0D, (double)this.getEyeHeight() / 1.5, -(double)(this.getBbWidth() * 0.3F));
   }

   public int getCalmTime() {
      return calmTime;
   }

   public void setCalmTime(int calmTime) {
      this.calmTime = calmTime;
   }

   public boolean isFoodNeedUrgent() {
      if (isCalmTimeLow() ||
              isHealthLow()) {
         return true;
      }
      return false;
   }

   public boolean isHealthLow() {
      return getHealth() <= getMaxHealth() - 5;
   }

   public boolean isCalmTimeLow() {
      return getCalmTime() < 20 * 60 * 5;
   }

   public boolean isMeatyChest(BlockPos pos) {
      BlockEntity tile = level.getBlockEntity(pos);
      if (tile instanceof ChestBlockEntity && tile instanceof Container) {
         Container inv = (Container) tile;
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getCount() > 0) {
               if (isRawMeat(stack)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public boolean isValidChestForFood(BlockPos pos, boolean sightCheck) {
      return isWithinRestriction(pos) && isMeatyChest(pos) && (!sightCheck || CoroUtilEntity.canSee(this, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())));
   }

   public boolean isValidChestForWork(BlockPos pos, boolean sightCheck) {
      return isWithinRestriction(pos) && chestHasRoom(pos) && (!sightCheck || CoroUtilEntity.canSee(this, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())));
   }

   public void tickScanForChests() {
      if ((level.getGameTime()+this.getId()) % (20*30) != 0) return;

      //CULog.dbg("scanning for chests");
      if (gameProfile.getName().equals("PhoenixfireLune")) {
         int asdasd = 0;
      }

      Iterator<BlockPos> it = listPosChests.iterator();
      while (it.hasNext()) {
         BlockPos pos = it.next();
         if (!isValidChestForFood(pos, false) && !isValidChestForWork(pos, false)) {
            it.remove();
         }
      }

      if (listPosChests.size() >= MAX_CHESTS) {
         return;
      }

      List<ZombiePlayer> listEnts = level.getEntitiesOfClass(ZombiePlayer.class, new AABB(this.blockPosition()).inflate(20, 20, 20));
      Collections.shuffle(listEnts);
      for (ZombiePlayer ent : listEnts) {
         if (!ent.isCalm()) {
            continue;
         }
         if (listPosChests.size() >= MAX_CHESTS) {
            return;
         }
         //prevent getting coords from other zombies we cant see incase we cant access those areas
            /*if (!this.canEntityBeSeen(ent)) {
                return;
            }*/
         Iterator<BlockPos> it2 = ent.listPosChests.iterator();
         while (it2.hasNext()) {
            BlockPos pos = it2.next();

            if (!hasChestAlready(pos) && (isValidChestForFood(pos, true) || isValidChestForWork(pos, true))) {
               addChestPos(pos);
            }

            if (listPosChests.size() >= MAX_CHESTS) {
               return;
            }
         }
      }

      int range = ConfigZombiePlayersAdvanced.chestSearchRange;
      for (int x = -range; x <= range; x++) {
         for (int y = -range/2; y <= range/2; y++) {
            for (int z = -range; z <= range; z++) {
               BlockPos pos = this.blockPosition().offset(x, y, z);
               if (isValidChestForFood(pos, true) || isValidChestForWork(pos, true)) {
                  if (!hasChestAlready(pos)) {
                     addChestPos(pos);
                  }

                  if (listPosChests.size() >= MAX_CHESTS) {
                     return;
                  }
               }
            }
         }
      }
   }

   public void addChestPos(BlockPos pos) {
      CULog.dbg("zombie player adding chest pos: " + pos);
      listPosChests.add(pos);
   }

   public boolean hasChestAlready(BlockPos pos) {
      Iterator<BlockPos> it = listPosChests.iterator();
      while (it.hasNext()) {
         BlockPos pos2 = it.next();
         if (pos.equals(pos2)) {
            return true;
         }
      }
      return false;
   }

   public boolean isCanEatFromChests() {
      return canEatFromChests;
   }

   public void setCanEatFromChests(boolean canEatFromChests) {
      this.canEatFromChests = canEatFromChests;
   }

   @Nullable
   @Override
   public UUID getOwnerUUID() {
      return getPlayer() != null ? getPlayer().getUUID() : null;
   }

   @Nullable
   @Override
   public Entity getOwner() {
      return getPlayer();
   }

   public void setOwnerName(String name) {
      this.ownerName = name;
      setPersistenceRequired();
      this.setTarget(null);
   }

   public Player getPlayer() {
      if (playerWeakReference.get() != null && playerWeakReference.get().isDeadOrDying()) {
         playerWeakReference.clear();
      }
      if (playerWeakReference.get() != null && !playerWeakReference.get().isDeadOrDying()) {
         return playerWeakReference.get();
      }
      if (hasOwner()) {
         Player player = getPlayerEntityByName(ownerName);
         if (player != null) {
            playerWeakReference = new WeakReference<>(player);
            return playerWeakReference.get();
         }
      }
      return null;
   }

   public boolean hasOwner() {
      return ownerName != null && !ownerName.equals("");
   }

   public void openChest(BlockPos pos) {
      if (!hasOpenedChest) {
         hasOpenedChest = true;
         //keep higher than EntityAIInteractChest.ticksChestOpenMax to avoid bugging it out until i merge code better
         chestUseTime = 15;
         posChestUsing = pos;
         BlockEntity tEnt = level.getBlockEntity(pos);
         if (tEnt instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity) tEnt;
            FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));

            CULog.dbg("open chest" + gameProfile.getName());
            chest.startOpen(fakePlayer);
         }
      }
   }

   public void closeChest(BlockPos pos) {
      if (hasOpenedChest) {
         hasOpenedChest = false;
         BlockEntity tEnt = level.getBlockEntity(pos);
         if (tEnt instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity) tEnt;
            FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
            CULog.dbg("close chest" + gameProfile.getName());
            //vanilla is insta closing the chests, making this break the internal counter, so this line is off until we figure out that other issue, probably temp fake player related
            //chest.stopOpen(fakePlayer);
         }
      }
   }
   
   public void setHomePosAndDistanceBackup(BlockPos pos, int distance) {
      this.homePositionBackup = pos;
      this.homeDistBackup = distance;
   }

   public void setHomePosAndDistance(BlockPos pos, int distance, boolean setBackup) {
      super.restrictTo(pos, distance);
      if (setBackup) {
         setHomePosAndDistanceBackup(pos, distance);
      }
   }

   public void markStartPlaying() {
      lastTimeStartedPlaying = level.getGameTime();
   }

   public boolean canPlay() {
      return lastTimeStartedPlaying + ConfigZombiePlayersAdvanced.tickDelayBetweenPlaying < level.getGameTime();
   }

   @Override
   public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
      return false;
   }

   @Override
   public Packet<?> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   @Override
   public void writeSpawnData(FriendlyByteBuf buffer) {
      buffer.writeInt(risingTime);
      if (gameProfile != null) {
         buffer.writeUtf(gameProfile.getName());
         buffer.writeUtf(gameProfile.getId() != null ? gameProfile.getId().toString() : "");
         /*ByteBufUtil.writeUtf8(buffer, gameProfile.getName());
         ByteBufUtil.writeUtf8(buffer, gameProfile.getId() != null ? gameProfile.getId().toString() : "");*/
      }
   }

   @Override
   public void readSpawnData(FriendlyByteBuf additionalData) {
      try {
         risingTime = additionalData.readInt();
         /*String playerName = readUTF8String(additionalData);
         String playerUUID = readUTF8String(additionalData);*/
         String playerName = additionalData.readUtf();
         String playerUUID = additionalData.readUtf();
         gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);
      } catch (Exception ex) {
         //just log simple message and debug if needed
         CULog.dbg("exception for EntityZombiePlayer.readSpawnData: " + ex.toString());
         //ex.printStackTrace();
      }
   }

   @Nullable
   public Player getPlayerEntityByName(String name)
   {
      for (int j2 = 0; j2 < this.level.players().size(); ++j2)
      {
         Player entityplayer = this.level.players().get(j2);

         if (name.equals(entityplayer.getName().getString()))
         {
            return entityplayer;
         }
      }

      return null;
   }

   @Override
   public boolean shouldShowName() {
      return true;
   }

   @Override
   protected boolean shouldDespawnInPeaceful() {
      return !isCalm();
   }

   private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;

   @Override
   public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
      if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && itemHandler != null)
         return itemHandler.cast();
      return super.getCapability(capability, facing);
   }

   @Override
   public void invalidateCaps() {
      super.invalidateCaps();
      if (itemHandler != null) {
         net.minecraftforge.common.util.LazyOptional<?> oldHandler = itemHandler;
         itemHandler = null;
         oldHandler.invalidate();
      }
   }

   public boolean hasInventoryChanged(Container p_149512_) {
      return this.inventory != p_149512_;
   }

   protected int getInventorySize() {
      return 9*3;
   }

   protected void createInventory() {
      SimpleContainer simplecontainer = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (simplecontainer != null) {
         simplecontainer.removeListener(this);
         int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

         for(int j = 0; j < i; ++j) {
            ItemStack itemstack = simplecontainer.getItem(j);
            if (!itemstack.isEmpty()) {
               this.inventory.setItem(j, itemstack.copy());
            }
         }
      }

      this.inventory.addListener(this);
      this.updateContainerEquipment();
      this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.inventory));
   }

   protected void updateContainerEquipment() {
      /*if (!this.level.isClientSide) {
         this.setFlag(4, !this.inventory.getItem(0).isEmpty());
      }*/
   }

   @Override
   public void containerChanged(Container p_30548_) {
      /*boolean flag = this.isSaddled();
      this.updateContainerEquipment();
      if (this.tickCount > 20 && !flag && this.isSaddled()) {
         this.playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
      }*/

   }

   public boolean isDepositingInChest() {
      return isDepositingInChest;
   }

   public void setDepositingInChest(boolean depositingInChest) {
      isDepositingInChest = depositingInChest;
   }

   public int getWorkDistance() {
      return 10;
   }
}
