package com.corosus.zombie_players.entity;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import com.corosus.coroutil.util.CULog;
import com.corosus.coroutil.util.CoroUtilEntity;
import com.corosus.zombie_players.EntityRegistry;
import com.corosus.zombie_players.Zombie_Players;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.Validate;

public class ZombiePlayerNew extends Monster implements IEntityAdditionalSpawnData, OwnableEntity {
   private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = (p_34284_) -> {
      return p_34284_ == Difficulty.HARD;
   };
   private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE);
   private boolean canBreakDoors;
   private int inWaterTime;
   private int conversionTime;



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

   /*public ZombiePlayerNew(EntityType<? extends ZombiePlayerNew> p_34271_, Level p_34272_) {
      super(p_34271_, p_34272_);
   }*/

   public ZombiePlayerNew(Level p_34274_) {
      this(EntityRegistry.zombie_player, p_34274_);
   }

   public ZombiePlayerNew(EntityType<ZombiePlayerNew> entityEntityType, Level level) {
      super(entityEntityType, level);
   }

   protected void registerGoals() {
      //this.goalSelector.addGoal(4, new ZombiePlayerNew.ZombieAttackTurtleEggGoal(this, 1.0D, 3));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      //this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
      //this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0D, true, 4, this::canBreakDoors));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
      //this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      //this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      //this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      //this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));

      /*this.goalSelector.addGoal(taskID++, new EntityAISwimming(this));
      this.goalSelector.addGoal(taskID++, new EntityAIAvoidEntityOnLowHealth(this, EntityLivingBase.class, ENEMY_PREDICATE,
              16.0F, 1.4D, 1.4D, 10F));
      this.goalSelector.addGoal(taskID++, new EntityAIEatToHeal(this));
      this.goalSelector.addGoal(taskID++, new EntityAIZombieAttackWeaponReach(this, 1.0D, false));
      this.goalSelector.addGoal(taskID++, new EntityAIMoveToWantedNearbyItems(this, 1.15D));
      this.goalSelector.addGoal(taskID++, new EntityAIOpenDoor(this, false));
      this.goalSelector.addGoal(taskID++, new EntityAIFollowOwnerZombie(this, 1.2D, 10.0F, 2.0F));
      this.goalSelector.addGoal(taskID++, new EntityAIMoveTowardsRestrictionZombie(this, 1.0D) {});
      this.goalSelector.addGoal(taskID++, new EntityAITemptZombie(this, 1.2D, false));

      //will this mess with calm state?
      this.goalSelector.addGoal(taskID++, new EntityAIInteractChest(this, 1.0D, 20));
      this.goalSelector.addGoal(taskID++, new EntityAIPlayZombiePlayer(this, 1.15D));
      this.goalSelector.addGoal(taskID++, new EntityAIWanderAvoidWater(this, 1.0D));

      this.goalSelector.addGoal(taskID, new EntityAIWatchClosest(this, Player.class, 8.0F));
      this.goalSelector.addGoal(taskID, new EntityAIWatchClosest(this, EntityZombiePlayer.class, 8.0F));
      this.goalSelector.addGoal(taskID, new EntityAILookIdle(this));

      this.targetSelector.addGoal(1, new EntityAIHurtByTarget(this, false, new Class[] {EntityPigZombie.class}));
      this.targetSelector.addGoal(2, new EntityAINearestAttackableTargetIfCalm(this, Player.class, true, true));
      this.targetSelector.addGoal(3, new EntityAINearestAttackableTargetIfCalm(this, EntityVillager.class, false, true));
      this.targetSelector.addGoal(3, new EntityAINearestAttackableTargetIfCalm(this, EntityIronGolem.class, true, true));

      this.targetSelector.addGoal(3, new EntityAINearestAttackableTargetIfCalm(this, EntityLiving.class, 0, true, false, ENEMY_PREDICATE, false));*/
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

   protected boolean convertsInWater() {
      return true;
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

            particle = ParticleTypes.NOTE;
         } else if (isCalm() && itemstack.getItem() == Items.GLASS_BOTTLE) {
            itemUsed = true;
            this.setBaby(!this.isBaby());

            particle = ParticleTypes.WITCH;
         } else if (isCalm() && itemstack.getItem() == Item.BY_BLOCK.get(Blocks.CHEST)) {
            //itemUsed = true;
            this.setCanEatFromChests(!this.canEatFromChests);

            particle = this.canEatFromChests ? ParticleTypes.HEART : ParticleTypes.WITCH;
         } else if (isCalm() && itemstack.getItem() == Items.ROTTEN_FLESH) {
            itemUsed = true;

            //TODO: 1.18, pretty sure this is the correct replacement here, verify this works and drops what theyre holding
            this.dropCustomDeathLoot(DamageSource.IN_WALL, 0, true);
            //this.dropEquipment(true, 0);
            this.clearInventory();

            particle = ParticleTypes.WITCH;
         } else if (itemstack.getItem() instanceof BedItem) {
            if (isCalm()) {
               particle = null;
               if (this.isLeashed()) {
                  for (int i = 0; i < particleCount; i++) this.level.addParticle(DustParticleOptions.REDSTONE, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(),
                          0.3D, 0D, 0.3D);
                  player.sendMessage(new TextComponent("Can't set home while leashed"), uuid);
               } else {
                  if (this.hasRestriction()) {
                     if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range1) {
                        for (int i = 0; i < particleCount; i++) this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 1.0D, this.getZ(),
                                0.3D, 0D, 0.3D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range2, true);
                        player.sendMessage(new TextComponent("Home set with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range2), uuid);
                     } else if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range2) {
                        for (int i = 0; i < particleCount; i++) this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 1.5D, this.getZ(), 0.3D, 0D, 0.3D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range3, true);
                        player.sendMessage(new TextComponent("Home set with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range3), uuid);
                     } else if (this.getRestrictRadius() == ConfigZombiePlayersAdvanced.stayNearHome_range3) {
                        for (int i = 0; i < particleCount; i++) this.level.addParticle(DustParticleOptions.REDSTONE, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0.3D, 0D, 0.3D);
                        this.setHomePosAndDistance(BlockPos.ZERO, -1, true);
                        player.sendMessage(new TextComponent("Home removed"), uuid);
                     } else {
                        for (int i = 0; i < particleCount; i++) this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(),
                                0.3D, 0D, 0.3D);
                        this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1, true);
                        player.sendMessage(new TextComponent("Home set with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range1), uuid);
                     }

                  } else {
                     for (int i = 0; i < particleCount; i++) this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(),
                             0.3D, 0D, 0.3D);
                     this.setHomePosAndDistance(blockPosition(), (int) ConfigZombiePlayersAdvanced.stayNearHome_range1, true);
                     player.sendMessage(new TextComponent("Home set with max wander distance of " + ConfigZombiePlayersAdvanced.stayNearHome_range1), uuid);
                  }
               }

            }


         } else if (isRawMeat(itemstack)) {
            itemUsed = true;

            if (!hasOwner()) {
               setOwnerName(player.getName().getString());
            }
            ateCalmingItem();

            particle = null;
         } else if (isCalm() && itemstack.isEmpty()) {
            shouldFollowOwner = !shouldFollowOwner;

            particle = this.shouldFollowOwner ? ParticleTypes.HEART : ParticleTypes.WITCH;

            if (shouldFollowOwner) {
               this.restrictTo(null, -1);
            } else {
               this.restrictTo(homePositionBackup, homeDistBackup);
            }
         }

         if (particle != null) {
            for (int i = 0; i < particleCount; i++) this.level.addParticle(particle, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(),
                    0.3D, 0D, 0.3D);
            level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
         }

         if (itemUsed) {

            this.consumeItemFromStack(player, itemstack);
            return InteractionResult.CONSUME;
         }
      }

      return super.mobInteract(player, hand);
   }

   public void tick() {
      if (!this.level.isClientSide && this.isAlive() && !this.isNoAi()) {

      }

      if (!level.isClientSide()) {
         if (calmTime > 0) {

            if (chestUseTime > 0) {
               chestUseTime--;
               if (chestUseTime == 0) {
                  if (posChestUsing != null) {
                     closeChest(posChestUsing);
                  }
               }
            }

            if (calmTime <= 100 && calmTime % 10 == 0) {
               this.playSound(SoundEvents.ZOMBIE_INFECT, getSoundVolume(), getVoicePitch());
               this.level.addParticle(ParticleTypes.ANGRY_VILLAGER, true, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0.3D, 0D, 0.3D);
            } else if (calmTime <= 20 * 90 && calmTime % (20 * 5) == 0) {
               this.playSound(SoundEvents.ZOMBIE_VILLAGER_AMBIENT, getSoundVolume(), getVoicePitch());
               this.level.addParticle(ParticleTypes.ANGRY_VILLAGER, true, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0.3D, 0D, 0.3D);
            }

            if (isHealthLow()) {
               this.level.addParticle(DustParticleOptions.REDSTONE, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0, 0, 0);
            }
            if (isCalmTimeLow()) {
               this.level.addParticle(ParticleTypes.WITCH, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0, 0, 0);
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

         if (level.getGameTime() % 20 == 0 && isItemWeWant(getMainHandItem())) {
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
               //level.addParticle(ParticleTypes.BLOCK_DUST, getX() + x1, getY(), getZ() + z1, x2, y2, z2, Block.getId(state));
               ((ServerLevel)this.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX() + x1, getY(), getZ() + z1, 10, (double)(this.getBbWidth() / 4.0F), (double)(this.getBbHeight() / 4.0F), (double)(this.getBbWidth() / 4.0F), 0.05D);
            }
         }
      } else {
         this.setNoAi(false);
      }

      super.tick();
   }

   public void aiStep() {
      if (this.isAlive()) {
         boolean flag = this.isSunSensitive() && this.isSunBurnTick();
         if (flag) {
            ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemstack.isEmpty()) {
               if (itemstack.isDamageableItem()) {
                  itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
                  if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                     this.broadcastBreakEvent(EquipmentSlot.HEAD);
                     this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                  }
               }

               flag = false;
            }

            if (flag) {
               this.setSecondsOnFire(8);
            }
         }
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

         return true;
      }
   }

   public boolean doHurtTarget(Entity entityIn) {
      boolean result = super.doHurtTarget(entityIn);

      if (!level.isClientSide() && entityIn instanceof LivingEntity) {
         if (((LivingEntity) entityIn).getHealth() <= 0 && isCalm()) {
            heal((float)ConfigZombiePlayersAdvanced.healPerKill);
            this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(),
                    0.3D, 0D, 0.3D);
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

   public void addAdditionalSaveData(CompoundTag p_34319_) {
      super.addAdditionalSaveData(p_34319_);
      p_34319_.putBoolean("IsBaby", this.isBaby());
      p_34319_.putBoolean("CanBreakDoors", this.canBreakDoors());
      p_34319_.putInt("InWaterTime", this.isInWater() ? this.inWaterTime : -1);
   }

   public void readAdditionalSaveData(CompoundTag p_34305_) {
      super.readAdditionalSaveData(p_34305_);
      this.setBaby(p_34305_.getBoolean("IsBaby"));
      this.setCanBreakDoors(p_34305_.getBoolean("CanBreakDoors"));
      this.inWaterTime = p_34305_.getInt("InWaterTime");

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

   public static boolean getSpawnAsBabyOdds(Random p_34303_) {
      return p_34303_.nextFloat() < net.minecraftforge.common.ForgeConfig.SERVER.zombieBabyChance.get();
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
         return new TextComponent("Zombie " + gameProfile.getName());
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
         this.level.addParticle(ParticleTypes.HEART, false, this.getX(), this.getY() + this.getEyeHeight() + 0.5D, this.getZ(), 0.3D, 0D, 0.3D);
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

   public boolean isValidChest(BlockPos pos, boolean sightCheck) {
      return isWithinRestriction(pos) && isMeatyChest(pos) && (!sightCheck || CoroUtilEntity.canSee(this, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())));
   }

   public void tickScanForChests() {
      if ((level.getGameTime()+this.getId()) % (20*30) != 0) return;

      //CULog.dbg("scanning for chests");

      Iterator<BlockPos> it = listPosChests.iterator();
      while (it.hasNext()) {
         BlockPos pos = it.next();
         if (!isValidChest(pos, false)) {
            it.remove();
         }
      }

      if (listPosChests.size() >= MAX_CHESTS) {
         return;
      }

      List<ZombiePlayerNew> listEnts = level.getEntitiesOfClass(ZombiePlayerNew.class, new AABB(this.blockPosition()).inflate(20, 20, 20));
      Collections.shuffle(listEnts);
      for (ZombiePlayerNew ent : listEnts) {
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

            if (!hasChestAlready(pos) && isValidChest(pos, true)) {
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
               if (isValidChest(pos, true)) {
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
      CULog.dbg("open chest");
      hasOpenedChest = true;
      //keep higher than EntityAIInteractChest.ticksChestOpenMax to avoid bugging it out until i merge code better
      chestUseTime = 15;
      posChestUsing = pos;
      /*BlockEntity tEnt = level.getBlockEntity(pos);
      if (tEnt instanceof ChestBlockEntity) {
         ChestBlockEntity chest = (ChestBlockEntity) tEnt;
         chest.numPlayersUsing++;
         level.addBlockEvent(pos, chest.getBlockType(), 1, chest.numPlayersUsing);
         level.notifyNeighborsOfStateChange(pos, chest.getBlockType(), false);
      }*/
   }

   public void closeChest(BlockPos pos) {
      CULog.dbg("close chest");
      hasOpenedChest = false;
      BlockEntity tEnt = level.getBlockEntity(pos);
      if (tEnt instanceof ChestBlockEntity) {
         /*ChestBlockEntity chest = (ChestBlockEntity) tEnt;
         chest.numPlayersUsing--;
         if (chest.numPlayersUsing < 0) {
            chest.numPlayersUsing = 0;
         }
         level.addBlockEvent(pos, chest.getBlockType(), 1, chest.numPlayersUsing);
         level.notifyNeighborsOfStateChange(pos, chest.getBlockType(), false);*/
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
   public void writeSpawnData(FriendlyByteBuf buffer) {
      buffer.writeInt(risingTime);
      if (gameProfile != null) {
         ByteBufUtil.writeUtf8(buffer, gameProfile.getName());
         ByteBufUtil.writeUtf8(buffer, gameProfile.getId() != null ? gameProfile.getId().toString() : "");
      }
   }

   @Override
   public void readSpawnData(FriendlyByteBuf additionalData) {
      try {
         risingTime = additionalData.readInt();
         String playerName = readUTF8String(additionalData);
         String playerUUID = readUTF8String(additionalData);
         gameProfile = new GameProfile(!playerUUID.equals("") ? UUIDTypeAdapter.fromString(playerUUID) : null, playerName);
      } catch (Exception ex) {
         //just log simple message and debug if needed
         CULog.dbg("exception for EntityZombiePlayer.readSpawnData: " + ex.toString());
         //ex.printStackTrace();
      }
   }

   public static String readUTF8String(ByteBuf from)
   {
      int len = readVarInt(from,2);
      String str = from.toString(from.readerIndex(), len, StandardCharsets.UTF_8);
      from.readerIndex(from.readerIndex() + len);
      return str;
   }

   public static int readVarInt(ByteBuf buf, int maxSize)
   {
      Validate.isTrue(maxSize < 6 && maxSize > 0, "Varint length is between 1 and 5, not %d", maxSize);
      int i = 0;
      int j = 0;
      byte b0;

      do
      {
         b0 = buf.readByte();
         i |= (b0 & 127) << j++ * 7;

         if (j > maxSize)
         {
            throw new RuntimeException("VarInt too big");
         }
      }
      while ((b0 & 128) == 128);

      return i;
   }

   @Nullable
   public Player getPlayerEntityByName(String name)
   {
      for (int j2 = 0; j2 < this.level.players().size(); ++j2)
      {
         Player entityplayer = this.level.players().get(j2);

         if (name.equals(entityplayer.getName()))
         {
            return entityplayer;
         }
      }

      return null;
   }
}
