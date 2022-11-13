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
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
   public boolean canPickupExtraItems = false;
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
   /**
    * Now using inventory from fakeplayer for extra slots, and ai entity inventory for hands and armor, and also redirecting players hand lookups to zombie player hands
    */
   //protected SimpleContainer inventory;
   private WorkInfo workInfo = new WorkInfo();
   private boolean isDepositingInChest = false;
   private FakePlayer fakePlayer = null;
   private int itemUseTime = 0;
   private boolean showWorkInfo = false;

   public ZombiePlayer(EntityType<ZombiePlayer> entityEntityType, Level level) {
      super(entityEntityType, level);
      ((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
      //this.createInventory();
   }

   public static ZombiePlayer spawnInPlaceOfPlayer(ServerPlayer player) {
      boolean spawn = true;
      ZombiePlayer zombie = null;
      if (player.getRespawnPosition() != null && player.getRespawnPosition() != BlockPos.ZERO) {
         if (player.getRespawnPosition().distSqr(new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()))) <
                 ConfigZombiePlayers.distanceFromPlayerSpawnPointToPreventZombieSpawn * ConfigZombiePlayers.distanceFromPlayerSpawnPointToPreventZombieSpawn) {
            spawn = false;
         }
      }
      if (spawn) {

         if (player.level instanceof ServerLevelAccessor) {
            zombie = spawnInPlaceOfPlayer(player.level, player.getX(), player.getY(), player.getZ(), player.getGameProfile());
            if (player.getRespawnPosition() != null && player.getRespawnPosition() != BlockPos.ZERO) {
               zombie.setHomePosAndDistance(player.getRespawnPosition(), 16, true);
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

   public static com.google.common.base.Predicate<Entity> ENEMY_PREDICATE_WITHIN_RESTRICTION = p_apply_1_ -> p_apply_1_ != null &&
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
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers(ZombiePlayer.class));
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
            if (player.isCrouching()) {
               setCanPickupExtraItems(!canPickupExtraItems);
               if (canPickupExtraItems) {
                  player.sendMessage(new TextComponent("Set to pickup extra items"), uuid);
               } else {
                  player.sendMessage(new TextComponent("Set to not pickup extra items"), uuid);
               }
            } else {
               //itemUsed = true;
               this.setCanEatFromChests(!this.canEatFromChests);
               if (this.canEatFromChests) {
                  player.sendMessage(new TextComponent("Set to eat food from chests"), uuid);
               } else {
                  player.sendMessage(new TextComponent("Set to not eat food from chests"), uuid);
               }


               particle = this.canEatFromChests ? ParticleTypes.HEART : ParticleTypes.WITCH;
            }
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
                  ((ServerLevel) this.level).sendParticles(DustParticleOptions.REDSTONE, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                  player.sendMessage(new TextComponent("Can't set home while leashed"), uuid);
               } else if (this.getWorkInfo().isPerformingWork()) {
                  ((ServerLevel) this.level).sendParticles(DustParticleOptions.REDSTONE, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), particleCount, 0.3D, 0D, 0.3D, 1D);
                  player.sendMessage(new TextComponent("Can't set home while working, work area force set as home"), uuid);
                  if (getWorkInfo().getPosWorkArea() != WorkInfo.CENTER_ZERO) {
                     Vec3 center = getWorkInfo().getPosWorkArea().getCenter();
                     restrictTo(new BlockPos(center.x, center.y, center.z), (int) getWorkInfo().getPosWorkArea().getSize());
                  }
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


         } else if (isCalmingItem(itemstack) && (getHealth() < getMaxHealth() || getOwner() == null || isCalmTimeLow())) {
            itemUsed = true;

            if (!hasOwner()) {
               player.sendMessage(new TextComponent("Zombie Player Tamed"), uuid);
               setOwnerName(player.getName().getString());

            }
            ateCalmingItem();

            particle = null;
         /*} else if (itemstack.getItem() == Items.DIAMOND_HOE) {
            player.sendMessage(new TextComponent("Set work center"), uuid);
            getWorkInfo().setPosWorkCenter(blockPosition());
         */} else if (itemstack.getItem() == Items.GOLDEN_HOE) {
            /*if (player.isSprinting()) {
               CULog.dbg("sprint click");
               this.showWorkInfo = !this.showWorkInfo;
            } else */if (player.isCrouching()) {
               if (player.getPersistentData().getInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE) == 0) {
                  player.getPersistentData().putInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE, 1);
                  getWorkInfo().setInAreaSetMode(true);
                  player.sendMessage(new TextComponent("Setting work area, right click first block with golden hoe, or zombie player to remove work area"), uuid);
               } else {
                  getWorkInfo().setInAreaSetMode(false);
                  player.sendMessage(new TextComponent("Removing work area"), uuid);
                  getWorkInfo().setPosWorkArea(WorkInfo.CENTER_ZERO);
               }
            } else {
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

   public void particle(float r, float g, float b, double x, double y, double z) {
      ((ServerLevel)this.level).sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 1f), x, y, z, 1, 0.1D, 0.1D, 0.1D, 0D);
   }

   public void tick() {
      if (risingTime < risingTimeMax) risingTime++;

      //calmTime = this.calmTicksLow;

      if (!level.isClientSide()) {
         if (calmTime > 0) {

            boolean debugState = showWorkInfo;

            if (debugState && (level.getGameTime()+getId()) % 5 == 0) {
               AABB aabb = getWorkInfo().getPosWorkArea();
               if (aabb != null) {
                  drawAABBAsParticles(aabb);
               }
               BlockPos pos = getRestrictCenter();
               if (pos != BlockPos.ZERO && getRestrictRadius() != -1) {
                  drawCircleAsParticles(new Vec3(pos.getX()+0.5F, pos.getY()+0.5F, pos.getZ()+0.5F), getRestrictRadius());
               }
               Iterator<BlockPos> it = listPosChests.iterator();
               while (it.hasNext()) {
                  BlockPos pos2 = it.next();
                  particle(0.75F, 0.3F, 0.3F, pos2.getX() + 0.5F, pos2.getY() + 1.0F, pos2.getZ() + 0.5F);
               }
            }

            boolean testProjectile = false;

            if (testProjectile && level.getGameTime() % 20 == 0) {
               //if (getMainHandItem().getItem() == Items.BOW) {
                  //BowItem bow = (BowItem) Items.BOW;
               ItemStack stack = getMainHandItem();
               Item item = stack.getItem();

               stack.releaseUsing(level, getFakePlayer(), item.getUseDuration(stack) - 20);
               level.getEntities(EntityType.ARROW, this.getBoundingBox().inflate(2), Entity::isAlive).stream().forEach(entity -> entity.setOwner(this));
               //}
            }

            if (getWorkInfo().isPerformingWork()) {
               if (!getWorkInfo().getItemNeededForWork().isEmpty() && !getMainHandItem().isEmpty()) {
                  if (!itemstackMatches(getWorkInfo().getItemNeededForWork(), getMainHandItem())) {
                     ItemStack stack = getMainHandItem();
                     this.spawnAtLocation(stack);
                     this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
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

            boolean testProjectile2 = false;
            if (testProjectile2) {
               if (itemUseTime == 0) {
                  getFakePlayer().startUsingItem(InteractionHand.MAIN_HAND);
                  //this.startUsingItem(InteractionHand.MAIN_HAND);
               } else if (itemUseTime == 60) {
                  //getMainHandItem().use(level, getFakePlayer(), InteractionHand.MAIN_HAND); //crossbow
                  //getFakePlayer().stopUsingItem();
                  //this.stopUsingItem();
                  getFakePlayer().releaseUsingItem();
                  level.getEntities(EntityType.ARROW, this.getBoundingBox().inflate(2), Entity::isAlive).stream().forEach(entity -> entity.setOwner(this));
               }

               itemUseTime++;
               if (itemUseTime > 60) {
                  itemUseTime = 0;
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
            if (isCalm() && shouldPickupExtraItems() && this.isAlive() && !this.dead) {
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


   public Inventory getExtraInventory() {
      //return this.inventory;
      return this.getFakePlayer().getInventory();
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

      if (!this.level.isClientSide() && p_34288_.getEntity() instanceof Player) {
         Player player = (Player) p_34288_.getEntity();
         if (player.getMainHandItem().getItem() == Items.GOLDEN_HOE) {
            CULog.dbg("hoe attack");
            this.showWorkInfo = !this.showWorkInfo;
            if (this.showWorkInfo) {
               player.sendMessage(new TextComponent("Work info visible"), uuid);
            } else {
               player.sendMessage(new TextComponent("Work info hidden"), uuid);
            }
            return false;
         }
      }

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
            AABB aabb = AABB.ofSize(this.getEyePosition(), f, 1.0E-6D, f);
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
      if (!(entityIn instanceof LivingEntity)) return false;
      //boolean result = super.doHurtTarget(entityIn);
      boolean result = true;
      getFakePlayer().getAttributes().addTransientAttributeModifiers(getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND));
      getFakePlayer().attack(entityIn);
      List<Entity> list = level.getEntities((EntityTypeTest<Entity, Entity>) entityIn.getType(), this.getBoundingBox().inflate(24), Entity::isAlive);
      for (Entity ent : list) {
         if (ent instanceof Mob) {
            Mob livingEntity = (Mob) ent;
            if (livingEntity.getTarget() instanceof FakePlayerInventoryProxy) {
               livingEntity.setTarget(this);
            }
         }
      }

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

      compound.putDouble("work_area_X", getWorkInfo().getPosWorkArea().minX);
      compound.putDouble("work_area_Y", getWorkInfo().getPosWorkArea().minY);
      compound.putDouble("work_area_Z", getWorkInfo().getPosWorkArea().minZ);
      compound.putDouble("work_area_X2", getWorkInfo().getPosWorkArea().maxX);
      compound.putDouble("work_area_Y2", getWorkInfo().getPosWorkArea().maxY);
      compound.putDouble("work_area_Z2", getWorkInfo().getPosWorkArea().maxZ);

      compound.put("work_blockstate", NbtUtils.writeBlockState(getWorkInfo().getStateWorkLastObserved()));

      compound.put("work_item", getWorkInfo().getItemNeededForWork().save(new CompoundTag()));

      compound.putString("work_direction", getWorkInfo().getWorkClickDirectionLastObserved().getName());

      compound.putInt("work_click", getWorkInfo().getWorkClickLastObserved().ordinal());

      compound.putBoolean("work_active", getWorkInfo().isPerformingWork());

      compound.putBoolean("work_visible", isShowWorkInfo());

      compound.putBoolean("pickup_extra_items", isCanPickupExtraItems());

      //BlockHitResult data
      if (getWorkInfo().getBlockHitResult() != null) {
         compound.putBoolean("bhr_miss", getWorkInfo().getBlockHitResult().getType() == HitResult.Type.MISS);
         compound.putBoolean("bhr_inside", getWorkInfo().getBlockHitResult().isInside());
         compound.putDouble("bhr_vecpos_X", getWorkInfo().getBlockHitResult().getLocation().x);
         compound.putDouble("bhr_vecpos_Y", getWorkInfo().getBlockHitResult().getLocation().y);
         compound.putDouble("bhr_vecpos_Z", getWorkInfo().getBlockHitResult().getLocation().z);
         compound.putInt("bhr_pos_X", getWorkInfo().getBlockHitResult().getBlockPos().getX());
         compound.putInt("bhr_pos_Y", getWorkInfo().getBlockHitResult().getBlockPos().getY());
         compound.putInt("bhr_pos_Z", getWorkInfo().getBlockHitResult().getBlockPos().getZ());
      }

      ListTag listtag = new ListTag();

      for(int i = 2; i < this.getExtraInventory().getContainerSize(); ++i) {
         ItemStack itemstack = this.getExtraInventory().getItem(i);
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

      if (compound.contains("work_area_X")) {
         getWorkInfo().setPosWorkArea(new AABB(compound.getDouble("work_area_X"), compound.getDouble("work_area_Y"), compound.getDouble("work_area_Z"),
                 compound.getDouble("work_area_X2"), compound.getDouble("work_area_Y2"), compound.getDouble("work_area_Z2")));
      }

      if (compound.contains("work_blockstate")) getWorkInfo().setStateWorkLastObserved(NbtUtils.readBlockState(compound.getCompound("work_blockstate")));

      if (compound.contains("work_item")) getWorkInfo().setItemNeededForWork(ItemStack.of(compound.getCompound("work_item")));
      CULog.dbg(getWorkInfo().getItemNeededForWork().toString());
      if (compound.contains("work_direction")) getWorkInfo().setWorkClickDirectionLastObserved(Direction.byName(compound.getString("work_direction")));

      if (compound.contains("work_click")) getWorkInfo().setWorkClickLastObserved(EnumTrainType.get(compound.getInt("work_click")));

      if (compound.contains("work_active")) getWorkInfo().setPerformWork(compound.getBoolean("work_active"));

      if (compound.contains("work_visible")) setShowWorkInfo(compound.getBoolean("work_visible"));

      if (compound.contains("pickup_extra_items")) setCanPickupExtraItems(compound.getBoolean("pickup_extra_items"));

      //this.createInventory();
      ListTag listtag = compound.getList("Items", 10);

      for(int i = 0; i < listtag.size(); ++i) {
         CompoundTag compoundtag = listtag.getCompound(i);
         int j = compoundtag.getByte("Slot") & 255;
         if (j >= 2 && j < this.getExtraInventory().getContainerSize()) {
            this.getExtraInventory().setItem(j, ItemStack.of(compoundtag));
         }
      }

      if (compound.contains("bhr_vecpos_X")) {
         BlockPos pos = new BlockPos(compound.getInt("bhr_pos_X"), compound.getInt("bhr_pos_Y"), compound.getInt("bhr_pos_Z"));
         Vec3 vec3 = new Vec3(compound.getDouble("bhr_vecpos_X"), compound.getDouble("bhr_vecpos_Y"), compound.getDouble("bhr_vecpos_Z"));

         BlockHitResult result = compound.getBoolean("bhr_miss")
                 ? BlockHitResult.miss(vec3, getWorkInfo().getWorkClickDirectionLastObserved(), pos)
                 : new BlockHitResult(vec3, getWorkInfo().getWorkClickDirectionLastObserved(), pos, compound.getBoolean("bhr_inside"));

         getWorkInfo().setBlockHitResult(result);
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
      return isCalmingItem(stack);
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

   public boolean isCalmingItem(ItemStack stack) {
      Item item = stack.getItem();
      if (Zombie_Players.matchAnyFood && stack.getItem().isEdible() && stack.getItem() != Items.ROTTEN_FLESH) return true;
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
               if (isCalmingItem(stack)) {
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

   public boolean isDepositingInChest() {
      return isDepositingInChest;
   }

   public void setDepositingInChest(boolean depositingInChest) {
      isDepositingInChest = depositingInChest;
   }

   public int getWorkDistance() {
      return 10;
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return super.getMaxSpawnClusterSize();
   }

   public FakePlayer getFakePlayer() {
      if (fakePlayer == null) {
         //fakePlayer = FakePlayerFactory.get((ServerLevel) level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
         fakePlayer = new FakePlayerInventoryProxy((ServerLevel)level, new GameProfile(UUID.randomUUID(), "Zombie_Player"), this);
      }
      syncFakePlayer(fakePlayer);
      return fakePlayer;
   }

   public void syncFakePlayer() {
      syncFakePlayer(getFakePlayer());
   }

   public void syncFakePlayer(FakePlayer player) {
      /*Vec3 extraPos = this.getLookAngle().scale(0.5);
      player.setPos(getX()+extraPos.x, getY()+extraPos.y, getZ()+extraPos.z);*/
      player.setPos(getX(), getY(), getZ());

      player.setYRot(this.getYHeadRot());
      player.setXRot(this.getXRot());
      //player.getInventory().setItem(0, new ItemStack(Items.ARROW, 64));
   }

   public FakePlayer getFakePlayer2() {
      FakePlayer player = FakePlayerFactory.get((ServerLevel) level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
      Vec3 extraPos = this.getLookAngle().scale(0.5);
      player.setPos(getX()+extraPos.x, getY()+extraPos.y, getZ()+extraPos.z);

      player.setYRot(this.getYHeadRot());
      player.setXRot(this.getXRot());
      player.getInventory().setItem(0, new ItemStack(Items.ARROW, 64));
      return player;
   }

   /**
    * INVENTORY METHODS
    *
    * stolen from HopperBlockEntity, Inventory, and maybe SimpleContainer, a bunch made from scratch for chests and extra inventory accessing
    */

   protected void pickUpItemForExtraInventory(ItemEntity p_35467_) {
      ItemStack itemstack = p_35467_.getItem();
      //if (this.wantsToPickUp(itemstack)) {
      Inventory simplecontainer = this.getExtraInventory();
      boolean flag = canAddItem(simplecontainer, itemstack);
      if (!flag) {
         return;
      }

      this.onItemPickup(p_35467_);
      this.take(p_35467_, itemstack.getCount());
      ItemStack itemstack1 = addItem(simplecontainer, itemstack);
      if (itemstack1.isEmpty()) {
         p_35467_.discard();
      } else {
         itemstack.setCount(itemstack1.getCount());
      }
      //}

   }

   public boolean hasAnyItemsInExtra() {
      for(int i = 0; i < this.getExtraInventory().getContainerSize(); ++i) {
         ItemStack itemstack = this.getExtraInventory().getItem(i);
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
      if (getWorkInfo().getItemNeededForWork().isEmpty()) return false;
      ItemStack stack = getMainHandItem();
      if (!stack.isEmpty()) return false;
      //if (getMainHandItem() != ItemStack.EMPTY) return false;
      return true;
   }

   public boolean hasNeededWorkItemInExtra() {
      for(int i = 0; i < this.getExtraInventory().getContainerSize(); ++i) {
         ItemStack itemstack = this.getExtraInventory().getItem(i);
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
      BlockPos closestPos = BlockPos.ZERO;
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
      return this.getExtraInventory() != p_149512_;
   }

   protected int getInventorySize() {
      return 9*4;
   }

   /*protected void createInventory() {
      SimpleContainer simplecontainer = this.getExtraInventory();
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (simplecontainer != null) {
         simplecontainer.removeListener(this);
         int i = Math.min(simplecontainer.getContainerSize(), this.getExtraInventory().getContainerSize());

         for(int j = 0; j < i; ++j) {
            ItemStack itemstack = simplecontainer.getItem(j);
            if (!itemstack.isEmpty()) {
               this.getExtraInventory().setItem(j, itemstack.copy());
            }
         }
      }

      this.getExtraInventory().addListener(this);
      this.updateContainerEquipment();
      this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.getExtraInventory()));
   }*/

   public boolean isWithinWorkArea(BlockPos pos) {
      return getWorkInfo().getPosWorkArea().contains(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
   }

   protected void updateContainerEquipment() {
      /*if (!this.level.isClientSide) {
         this.setFlag(4, !this.getExtraInventory().getItem(0).isEmpty());
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

   public boolean canAddItem(Inventory container, ItemStack p_19184_) {
      boolean flag = false;

      for(ItemStack itemstack : container.items) {
         if (itemstack.isEmpty() || ItemStack.isSameItemSameTags(itemstack, p_19184_) && itemstack.getCount() < itemstack.getMaxStackSize()) {
            flag = true;
            break;
         }
      }

      return flag;
   }

   public ItemStack addItem(Inventory inventory, ItemStack p_19174_) {
      ItemStack itemstack = p_19174_.copy();
      this.moveItemToOccupiedSlotsWithSameType(inventory, itemstack);
      if (itemstack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.moveItemToEmptySlots(inventory, itemstack);
         return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(Inventory inventory, ItemStack p_19192_) {
      for(int i = 0; i < inventory.getContainerSize(); ++i) {
         ItemStack itemstack = inventory.getItem(i);
         if (ItemStack.isSameItemSameTags(itemstack, p_19192_)) {
            this.moveItemsBetweenStacks(inventory, p_19192_, itemstack);
            if (p_19192_.isEmpty()) {
               return;
            }
         }
      }

   }

   private void moveItemsBetweenStacks(Inventory inventory, ItemStack p_19186_, ItemStack p_19187_) {
      int i = Math.min(inventory.getMaxStackSize(), p_19187_.getMaxStackSize());
      int j = Math.min(p_19186_.getCount(), i - p_19187_.getCount());
      if (j > 0) {
         p_19187_.grow(j);
         p_19186_.shrink(j);
         inventory.setChanged();
      }

   }

   private void moveItemToEmptySlots(Inventory inventory, ItemStack p_19190_) {
      for(int i = 0; i < inventory.getContainerSize(); ++i) {
         ItemStack itemstack = inventory.getItem(i);
         if (itemstack.isEmpty()) {
            inventory.setItem(i, p_19190_.copy());
            p_19190_.setCount(0);
            return;
         }
      }

   }

   @Override
   protected boolean canReplaceCurrentItem(ItemStack p_21428_, ItemStack p_21429_) {
      //might break armor equipping while working
      if (getMainHandItem().isEmpty() && getWorkInfo().isPerformingWork() && getWorkInfo().getItemNeededForWork() != ItemStack.EMPTY && itemstackMatches(getWorkInfo().getItemNeededForWork(), p_21428_)) {
         return false;
      }
      return super.canReplaceCurrentItem(p_21428_, p_21429_);
   }

   public boolean isShowWorkInfo() {
      return showWorkInfo;
   }

   public void setShowWorkInfo(boolean showWorkInfo) {
      this.showWorkInfo = showWorkInfo;
   }

   public void drawAABBAsParticles(AABB aabb) {
      double step = 0.5F;
      for (double x = aabb.minX; x <= aabb.maxX; x+=step) {
         particle(0, 0, 1, x, aabb.minY, aabb.minZ);
         particle(0, 0, 1, x, aabb.minY, aabb.maxZ);
         particle(0, 0, 1, x, aabb.maxY, aabb.minZ);
         particle(0, 0, 1, x, aabb.maxY, aabb.maxZ);
      }
      for (double z = aabb.minZ; z <= aabb.maxZ; z+=step) {
         particle(0, 0, 1, aabb.minX, aabb.minY, z);
         particle(0, 0, 1, aabb.maxX, aabb.minY, z);
         particle(0, 0, 1, aabb.minX, aabb.maxY, z);
         particle(0, 0, 1, aabb.maxX, aabb.maxY, z);
      }
      for (double y = aabb.minY; y <= aabb.maxY; y+=step) {
         particle(0, 0, 1, aabb.minX, y, aabb.minZ);
         particle(0, 0, 1, aabb.minX, y, aabb.maxZ);
         particle(0, 0, 1, aabb.maxX, y, aabb.minZ);
         particle(0, 0, 1, aabb.maxX, y, aabb.maxZ);
      }
   }

   public void drawCircleAsParticles(Vec3 pos, float radius) {
      for (float angleTicks = 0; angleTicks < 360; angleTicks+=5) {
         float x = -Mth.sin(angleTicks * Mth.DEG_TO_RAD) * radius;
         float z = Mth.cos(angleTicks * Mth.DEG_TO_RAD) * radius;
         particle(1, 1, 1, pos.x + x, pos.y, pos.z + z);
      }
   }

   public boolean shouldPickupExtraItems() {
      return getWorkInfo().isPerformingWork() || this.canPickupExtraItems;
   }

   public boolean isCanPickupExtraItems() {
      return canPickupExtraItems;
   }

   public void setCanPickupExtraItems(boolean val) {
      this.canPickupExtraItems = val;
   }

}
