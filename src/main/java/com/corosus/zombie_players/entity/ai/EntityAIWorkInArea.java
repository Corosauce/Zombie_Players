package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CULog;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.EnumBlockBreakBehaviorType;
import com.corosus.zombie_players.entity.EnumTrainType;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.corosus.zombie_players.util.TreeCutter;
import com.corosus.zombie_players.util.UtilCrops;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class EntityAIWorkInArea extends Goal
{
    private final ZombiePlayer entityObj;

    private int walkingTimeoutMax = 20*10;

    private int walkingTimeout;
    private int repathPentalty = 0;

    private int lookUpdateTimer = 0;

    private float missingHealthToHeal = 5;

    //private BlockPos posCachedBestChest = null;

    private BlockPos posCurrentWorkTarget = BlockPos.ZERO;

    //private BlockState stateWork = null;

    //private BlockPos posWorkCenter = BlockPos.ZERO;

    private int scanCooldownAmount = 10;
    private int scanNextTickWork = 0;

    /**
     * need to likely target only specific states to match, so we need a basic mapping of state to lookup depending on the type of block, maybe tag to states
     *
     * crops
     * - block
     * - CropBlock.AGE and whatever their state was when player told zombie player to look for
     *
     * logs
     * - block
     * - veinmine
     *
     * cactus, sugar cane
     * - block
     * - dont mine bottom piece
     *
     * berry bush
     * - right click
     *
     * sheep
     * - entitytype / top level class
     * - uhhhhh i guess custom case of checking if they have wool?
     * - shear
     *
     * - maybe a timer where it will try to interact with entities every x mins
     *
     * animals:
     * - entitytype
     * - breed them
     *
     * cow:
     * - milk them
     *
     *
     * - a universal solution for entities right click it seeming like a delay
     * -- item to set delay
     * -- internally zombie player will map each entity instance to last time tried work on it, so it will do the work fast on each entity then wait
     *
     * - for killing animals, not sure what would be better than timer without adding more and more complexity
     *
     */

    //public static HashMap<Block, Property> lookupBlockToKeyProperty = new HashMap<>();
    public static HashMap<Class, BlockInfo> lookupBlockClassToBlockInfo = new HashMap<>();
    public static HashMap<String, BlockInfo> lookupBlockTagToBlockInfo = new HashMap<>();
    //public static HashMap<Block, Property> lookupBlockToKeyPropertyValue = new HashMap<>();

    //public static HashMap<Class, EnumBlockBreakBehaviorType> lookupBlockClassToBehaviorType = new HashMap<>();


    static class BlockInfo {
        public Property property;
        public EnumBlockBreakBehaviorType blockBreakBehaviorType;

        public BlockInfo(Property property, EnumBlockBreakBehaviorType blockBreakBehaviorType) {
            this.property = property;
            this.blockBreakBehaviorType = blockBreakBehaviorType;
        }
    }

    static {
        //lookupBlockToKeyProperty.put(Blocks.WHEAT, CropBlock.AGE);
        //lookupBlockClassToKeyProperty.put(CropBlock.class, CropBlock.AGE);
        //lookupBlockToKeyPropertyValue.put(Blocks.WHEAT, CropBlock.AGE);

        //TODO: data driven style like: "minecraft:wheat[age=7]", see quark SimpleHarvestModule

        add(CropBlock.class, CropBlock.AGE, EnumBlockBreakBehaviorType.HARVEST);
        add(SugarCaneBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(CactusBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(GrowingPlantBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(BambooBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(SweetBerryBushBlock.class, SweetBerryBushBlock.AGE, EnumBlockBreakBehaviorType.BREAK_NORMAL);

        add("logs", null, EnumBlockBreakBehaviorType.BREAK_VEINMINE_TREE);
    }

    public static void add(String tag, Property property, EnumBlockBreakBehaviorType behaviorType) {
        lookupBlockTagToBlockInfo.put(tag, new BlockInfo(property, behaviorType));
    }

    public static void add(Class clazz, Property property, EnumBlockBreakBehaviorType behaviorType) {
        lookupBlockClassToBlockInfo.put(clazz, new BlockInfo(property, behaviorType));
    }

    /*public static BlockInfo getInfo(BlockState state) {
        if (state.)
    }*/

    public EntityAIWorkInArea(ZombiePlayer entityObjIn)
    {
        this.entityObj = entityObjIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        //stateWork = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse()
    {
        //posCurrentWorkTarget = BlockPos.ZERO;
        if (!entityObj.isCalm() || entityObj.getWorkInfo().isInTrainingMode() || entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() == Blocks.AIR) return false;
        return canWorkOrFindWork();
    }

    public boolean canWorkOrFindWork() {
        if (scanNextTickWork < entityObj.getLevel().getGameTime()) {
            scanNextTickWork = (int) (entityObj.getLevel().getGameTime() + scanCooldownAmount);

            if (!entityObj.getWorkInfo().getPosWorkCenter().equals(BlockPos.ZERO) && posCurrentWorkTarget.equals(BlockPos.ZERO)) {
                return !findWorkBlockPos().equals(BlockPos.ZERO);
            }
            return false;
        } else {
            return false;
        }
    }

    public BlockPos findWorkBlockPos() {
        int range = ConfigZombiePlayersAdvanced.chestSearchRange;
        for (int x = -range; x <= range; x++) {
            for (int y = -range/2; y <= range/2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = this.entityObj.getWorkInfo().getPosWorkCenter().offset(x, y, z);
                    if (isValidWorkBlock(pos)) {
                        posCurrentWorkTarget = pos;
                        return posCurrentWorkTarget;
                    }
                }
            }
        }
        return BlockPos.ZERO;
    }

    public boolean isValidWorkBlock(BlockPos pos) {
        BlockState state = entityObj.level.getBlockState(pos);

        if (state.getBlock() == entityObj.getWorkInfo().getStateWorkLastObserved().getBlock()) {
            BlockInfo info = lookupBlockClassToBlockInfo.get(state.getBlock().getClass());
            if (info == null) {
                List list = state.getTags().toList();
                for (TagKey key : state.getTags().toList()) {
                    if (state.is(key)) {
                        String str = key.location().toString().replace("minecraft:", "");
                        info = lookupBlockTagToBlockInfo.get(str);
                        if (info != null) {
                            break;
                        }
                    }
                }
            }
            if (info != null) {
                if (info.property == null) {
                    return true;
                }
                if (state.hasProperty(info.property)) {
                    //cleanse each block to be only block + compared property
                    BlockState desiredState = entityObj.getWorkInfo().getStateWorkLastObserved();
                    if (state.getBlock().defaultBlockState().setValue(info.property, state.getValue(info.property)) ==
                            desiredState.getBlock().defaultBlockState().setValue(info.property, desiredState.getValue(info.property))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse()
    {
        if (!entityObj.isCalm() || entityObj.getWorkInfo().isInTrainingMode() || entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() == Blocks.AIR) return false;
        return !posCurrentWorkTarget.equals(BlockPos.ZERO);
    }

    @Override
    public void tick() {
        super.tick();

        /*if (hasFoodSource(entityObj.inventory)) {
            consumeOneStackSizeOfFood(entityObj.inventory);
            entityObj.heal(5);
            entityObj.world.playSound(null, entityObj.getPosition(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.NEUTRAL, 1F, 1F);
            return;
        }*/

        if (!posCurrentWorkTarget.equals(BlockPos.ZERO)) {
            boolean isClose = false;
            BlockPos blockposGoal = posCurrentWorkTarget;

            if (blockposGoal.equals(BlockPos.ZERO)) {
                stop();
                return;
            }

            //prevent walking into the fire
            double dist = entityObj.position().distanceTo(new Vec3(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 5D) {
                //entityObj.openChest(posCachedBestChest);
                /*for (int i = 0; i < 5 && entityObj.getHealth() < entityObj.getMaxHealth(); i++) {
                    consumeOneStackSizeOfFoodAtChest();
                    entityObj.ateCalmingItem(true);
                }*/
                if (operateOnTargetPosition(blockposGoal)) {
                    posCurrentWorkTarget = BlockPos.ZERO;
                }

                entityObj.getNavigation().stop();
                return;
            }

            if (!isClose) {
                if ((this.entityObj.getNavigation().isDone() || walkingTimeout <= 0) && repathPentalty <= 0) {

                    int i = blockposGoal.getX();
                    int j = blockposGoal.getY();
                    int k = blockposGoal.getZ();

                    boolean success = false;

                    if (this.entityObj.distanceToSqr(Vec3.atCenterOf(blockposGoal)) > 256.0D) {
                        Vec3 vec3d = DefaultRandomPos.getPosTowards(this.entityObj, 14, 3, new Vec3((double) i + 0.5D, (double) j, (double) k + 0.5D), (double)((float)Math.PI / 2F));

                        if (vec3d != null) {
                            success = this.entityObj.getNavigation().moveTo(vec3d.x, vec3d.y, vec3d.z, 1.3D);
                        }
                    } else {
                        success = this.entityObj.getNavigation().moveTo((double) i + 0.5D, (double) j, (double) k + 0.5D, 1.3D);
                    }

                    if (!success) {
                        repathPentalty = 40;
                    } else {
                        walkingTimeout = walkingTimeoutMax;
                    }
                } else {
                    if (walkingTimeout > 0) {
                        walkingTimeout--;
                    } else {

                    }
                }
            }

            if (repathPentalty > 0) {
                repathPentalty--;
            }

            if (lookUpdateTimer > 0) {
                lookUpdateTimer--;
            }
        }


    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start()
    {
        super.start();
        //this.insidePosX = -1;
        //reset any previous path so tick can start with a fresh path
        this.entityObj.getNavigation().stop();
    }

    /**
     * Resets the task
     */
    @Override
    public void stop()
    {
        super.stop();
        walkingTimeout = 0;
        posCurrentWorkTarget = BlockPos.ZERO;
    }

    public boolean operateOnTargetPosition(BlockPos pos) {
        BlockState state = entityObj.level.getBlockState(pos);
        if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_RIGHT_CLICK) {
            FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) entityObj.level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
            entityObj.level.getBlockState(pos).use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, true));
        } else {
            BlockInfo info = lookupBlockClassToBlockInfo.get(state.getBlock().getClass());
            if (info == null) {
                for (TagKey key : state.getTags().toList()) {
                    if (state.is(key)) {
                        String str = key.location().toString().replace("minecraft:", "");
                        info = lookupBlockTagToBlockInfo.get(str);
                        if (info != null) {
                            break;
                        }
                    }
                }
            }
            if (info != null) {
                if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_NORMAL) {
                    entityObj.level.destroyBlock(pos, true);
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.HARVEST) {
                    FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) entityObj.level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
                    if (!UtilCrops.harvestAndReplant(entityObj.level, pos, state, fakePlayer)) {
                        entityObj.level.destroyBlock(pos, true);
                    }
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_VEINMINE_TREE) {
                    //TODO: says pos needs to be air for findTree, tweak this when you test
                    TreeCutter.Tree tree = TreeCutter.findTree(entityObj.level, pos);
                    for (BlockPos posEntry : tree.getLogs()) {
                        entityObj.level.destroyBlock(posEntry, true);
                    }
                    for (BlockPos posEntry : tree.getLeaves()) {
                        entityObj.level.destroyBlock(posEntry, true);
                    }
                    entityObj.level.destroyBlock(pos, true);
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM) {
                    if (entityObj.level.getBlockState(pos.below()).getBlock() == entityObj.getWorkInfo().getStateWorkLastObserved().getBlock()) {
                        entityObj.level.destroyBlock(pos, true);
                    }
                }
            } else {
                CULog.dbg("error no info on blockstate: " + state);
            }
        }

        return true;
    }




}