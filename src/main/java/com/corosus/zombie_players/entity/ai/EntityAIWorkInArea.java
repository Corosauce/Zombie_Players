package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CULog;
import com.corosus.coroutil.util.CoroUtilEntity;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.EnumBlockBreakBehaviorType;
import com.corosus.zombie_players.entity.EnumTrainType;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.corosus.zombie_players.util.TreeCutter;
import com.corosus.zombie_players.util.UtilCrops;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
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
    //private long stuckTimeout = 0;
    //private long stuckTimeoutAmount = 100;

    private int lookUpdateTimer = 0;

    private float missingHealthToHeal = 5;

    //private BlockPos posCachedBestChest = null;

    private BlockPos posCurrentWorkTarget = BlockPos.ZERO;

    //private BlockState stateWork = null;

    //private BlockPos posWorkCenter = BlockPos.ZERO;

    private long scanCooldownAmount = 10;
    private long scanNextTickWork = 0;

    private long jobCompleteCooldown = 0;
    private long jobCompleteCooldownValue = 100;

    private int curScanRange = 3;

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
        //add(CaveVinesBlock.class, CaveVinesBlock.BERRIES, EnumBlockBreakBehaviorType.BREAK_NORMAL);
        //add(CaveVinesPlantBlock.class, CaveVinesBlock.BERRIES, EnumBlockBreakBehaviorType.BREAK_NORMAL);

        add("logs", null, EnumBlockBreakBehaviorType.BREAK_VEINMINE_TREE);
        add("cave_vines", CaveVinesBlock.BERRIES, EnumBlockBreakBehaviorType.BREAK_NORMAL);


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
        if (scanNextTickWork < entityObj.getLevel().getGameTime() && jobCompleteCooldown < entityObj.getLevel().getGameTime()) {
            scanNextTickWork = entityObj.getLevel().getGameTime() + scanCooldownAmount;

            if (!entityObj.getWorkInfo().getPosWorkCenter().equals(BlockPos.ZERO) && posCurrentWorkTarget.equals(BlockPos.ZERO)) {
                return !findWorkBlockPosRadial().equals(BlockPos.ZERO);
            }
            return false;
        } else {
            return false;
        }
    }

    public int getWorkDistance() {
        return 10;
    }

    public BlockPos findWorkBlockPosRadial() {
        int range = curScanRange;
        //CULog.dbg("scan range: " + range);
        int lastX = 0;
        int lastZ = 0;
        for (int angle = 0; angle < 360; angle+=1) {
            int x = (int) (-Mth.sin(angle * Mth.DEG_TO_RAD) * range);
            int z = (int) (Mth.cos(angle * Mth.DEG_TO_RAD) * range);

            if (lastX == x && lastZ == z) continue;

            lastX = x;
            lastZ = z;

            //CULog.dbg("scan " + x + " - " + z);

            for (int y = -4; y <= getWorkDistance()/2; y++) {
            //for (int y = -getWorkDistance()/2; y <= getWorkDistance()/2; y++) {

                //BlockPos pos = this.entityObj.getWorkInfo().getPosWorkCenter().offset(x, y, z);
                BlockPos pos = this.entityObj.blockPosition().offset(x, y, z);
                //((ServerLevel)entityObj.level).sendParticles(DustParticleOptions.REDSTONE, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, 0.3D, 0D, 0.3D, 1D);
                //CULog.dbg("probe: " + pos);
                if (isValidWorkBlock(pos)) {
                    if (entityObj.level.getBlockState(pos).is(BlockTags.LOGS)) {
                        if (!entityObj.level.getBlockState(pos.below()).is(BlockTags.LOGS) && !entityObj.level.getBlockState(pos.below()).isAir()) {
                            posCurrentWorkTarget = pos;
                            return posCurrentWorkTarget;
                        }
                    } else {
                        posCurrentWorkTarget = pos;
                        return posCurrentWorkTarget;
                    }
                }
            }
        }
        curScanRange++;
        if (curScanRange > getWorkDistance()) {
            curScanRange = 2;
        }
        return BlockPos.ZERO;
    }

    public BlockPos findWorkBlockPos() {
        int range = getWorkDistance();
        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= range/2; y++) {
            //for (int y = -range/2; y <= range/2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = this.entityObj.getWorkInfo().getPosWorkCenter().offset(x, y, z);
                    if (isValidWorkBlock(pos)) {
                        if (entityObj.level.getBlockState(pos).is(BlockTags.LOGS)) {
                            if (!entityObj.level.getBlockState(pos.below()).is(BlockTags.LOGS) && !entityObj.level.getBlockState(pos.below()).isAir()) {
                                posCurrentWorkTarget = pos;
                                return posCurrentWorkTarget;
                            }
                        } else {
                            posCurrentWorkTarget = pos;
                            return posCurrentWorkTarget;
                        }
                    }
                }
            }
        }
        return BlockPos.ZERO;
    }

    public boolean isValidWorkBlock(BlockPos pos) {
        BlockState state = entityObj.level.getBlockState(pos);


        //TODO: problem, this line fails for cave vines cause theres 2 cave vines blocks, how to fix by matching getBlockInfo without breaking othing stuff?
        //if our info match is tag based, just do if state.is(tag) ?
        //would help with various tree matches too
        BlockInfo infoDesired = getBlockInfo(entityObj.getWorkInfo().getStateWorkLastObserved());
        BlockInfo info = getBlockInfo(state);
        if (state.getBlock() == entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() || (infoDesired != null && info == infoDesired)) {
            boolean foundRuleForBlock = false;
            boolean successfullMatchPhase1 = false;

            if (info != null) {
                foundRuleForBlock = true;
            }

            //at this point, fallback to basic break or right click for target block
            //TODO: exclude things in lists that match block but dont match state
            if (!foundRuleForBlock) {
                successfullMatchPhase1 = true;
            }

            if (foundRuleForBlock) {
                if (info.property == null) {
                    successfullMatchPhase1 = true;
                }
                if (state.hasProperty(info.property)) {
                    //cleanse target and desired block to be only block + compared property
                    //- this is still bad cause if were depending on tags, the blocks could mismatch and we need to allow that
                    BlockState desiredBlockWithOnlySpecificComparedState = entityObj.getWorkInfo().getStateWorkLastObserved().getBlock().defaultBlockState()
                            .setValue(info.property, entityObj.getWorkInfo().getStateWorkLastObserved().getValue(info.property));
                    BlockState blockWithOnlySpecificComparedState = state.getBlock().defaultBlockState().setValue(info.property, state.getValue(info.property));

                    //CULog.dbg("blockWithOnlySpecificComparedState: " + blockWithOnlySpecificComparedState + " vs " + desiredBlockWithOnlySpecificComparedState + " - " + pos);

                    //compare targeted properties, since the block/tag matches from this point
                    if (entityObj.getWorkInfo().getStateWorkLastObserved().hasProperty(info.property) &&
                            entityObj.getWorkInfo().getStateWorkLastObserved().getValue(info.property) ==
                    state.getValue(info.property)) {
                        successfullMatchPhase1 = true;
                    }



                    /*if (blockWithOnlySpecificComparedState == desiredBlockWithOnlySpecificComparedState) {
                        successfullMatchPhase1 = true;
                    }*/
                }
            }

            if (successfullMatchPhase1) {
                //CULog.dbg("successfullMatchPhase1!: " + entityObj.level.getBlockState(pos));
            }

            if (successfullMatchPhase1 && isWithinRestrictions(pos)) {

                //filter out some bad conditions here

                if (entityObj.level.getBlockState(pos).is(BlockTags.LOGS)) {
                    if (entityObj.level.getBlockState(pos.below()).is(BlockTags.LOGS) || entityObj.level.getBlockState(pos.below()).isAir()) {
                        return false;
                    }
                }

                //if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_RIGHT_CLICK) {
                //lets try requiring air next to spot ONLY for blocks that have specificly set break operation rules
                if (!foundRuleForBlock) {

                    //check if there is air on the side player clicked
                    BlockPos posCheckForAir = pos.relative(entityObj.getWorkInfo().getWorkClickDirectionLastObserved());
                    //CULog.dbg("first pos: " + pos);
                    //CULog.dbg("posCheckForAir: " + posCheckForAir);

                    if (!entityObj.level.getBlockState(posCheckForAir).isAir()) {
                        return false;
                    }

                    /*if (entityObj.getWorkInfo().getWorkClickDirectionLastObserved() == Direction.UP) {
                        if (!entityObj.level.getBlockState(pos.above()).isAir()) {
                            return false;
                        }
                    }*/
                }

                CULog.dbg("found valid work block!: " + entityObj.level.getBlockState(pos));
                return true;
            }
        }
        return false;
    }

    public boolean isWithinRestrictions(BlockPos pos) {
        return entityObj.isWithinRestriction(pos) && this.entityObj.getWorkInfo().getPosWorkCenter().distSqr(pos) < getWorkDistance() * getWorkDistance();
    }

    public boolean hasLineOfSight(LivingEntity source, BlockPos p_147185_) {
        Vec3 vec3 = new Vec3(source.getX(), source.getEyeY(), source.getZ());
        Vec3 vec31 = new Vec3(p_147185_.getX() + 0.5, p_147185_.getY() + 0.5, p_147185_.getZ() + 0.5);
        if (vec31.distanceTo(vec3) > 128.0D) {
            return false;
        } else {
            /*BlockHitResult result = source.level.clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
            System.out.println(result..);*/
            return CoroUtilEntity.canSee(entityObj, p_147185_);
            //return result.getBlockPos() == p_147185_;//.getType() == HitResult.Type.MISS;
        }
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

        ((ServerLevel)entityObj.level).sendParticles(DustParticleOptions.REDSTONE, posCurrentWorkTarget.getX() + 0.5, posCurrentWorkTarget.getY() + 0.5, posCurrentWorkTarget.getZ() + 0.5, 1, 0.3D, 0D, 0.3D, 1D);

        if (!posCurrentWorkTarget.equals(BlockPos.ZERO)) {
            boolean isClose = false;
            BlockPos blockposGoal = posCurrentWorkTarget;

            if (blockposGoal.equals(BlockPos.ZERO)) {
                stop();
                return;
            }

            //prevent walking into the fire
            double dist = entityObj.position().distanceTo(new Vec3(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 3.8D) {
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
                        //success = this.entityObj.getNavigation().moveTo((double) i + 0.5D, (double) j, (double) k + 0.5D, 1.0D);
                        success = tryMoveToOpenSpotNextTo(new BlockPos(i, j, k));
                    }

                    if (!success) {
                        repathPentalty = 40;
                    } else {
                        //stuckTimeout = entityObj.level.getGameTime();
                        walkingTimeout = walkingTimeoutMax;
                    }
                } else {
                    if (walkingTimeout > 0) {
                        walkingTimeout--;

                        entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(entityObj.getNavigation().getTargetPos().getX() + 0.5, entityObj.getNavigation().getTargetPos().getY() + 0.5, entityObj.getNavigation().getTargetPos().getZ() + 0.5));

                        if (walkingTimeout < walkingTimeoutMax / 4 * 3) {
                            if (walkingTimeout % 4 == 0) {
                                for (int i = 0; i < 2; i++) {
                                    for (int x = -1; x < 2; x++) {
                                        trimLeaves(entityObj.blockPosition().offset(x, i, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, i, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, i, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, i, 0));

                                        trimLeaves(entityObj.blockPosition().offset(0, i, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, i, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, i, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, i, x));
                                    }

                                }
                            }
                        }
                    } else {
                        stop();
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

    public boolean tryMoveToOpenSpotNextTo(BlockPos pos) {
        if (entityObj.level.getBlockState(pos.offset(1, 0, 0)).isAir())
            return this.entityObj.getNavigation().moveTo((double) pos.getX() + 1 + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, 1.0D);
        if (entityObj.level.getBlockState(pos.offset(-1, 0, 0)).isAir())
            return this.entityObj.getNavigation().moveTo((double) pos.getX() - 1 + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, 1.0D);
        if (entityObj.level.getBlockState(pos.offset(0, 0, 1)).isAir())
            return this.entityObj.getNavigation().moveTo((double) pos.getX() + 0 + 0.5D, (double) pos.getY(), (double) pos.getZ() + 1 + 0.5D, 1.0D);
        if (entityObj.level.getBlockState(pos.offset(0, 0, -1)).isAir())
            return this.entityObj.getNavigation().moveTo((double) pos.getX() + 0 + 0.5D, (double) pos.getY(), (double) pos.getZ() - 1 + 0.5D, 1.0D);
        return this.entityObj.getNavigation().moveTo((double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, 1.0D);
    }

    public void trimLeaves(BlockPos pos) {
        if (entityObj.level.getBlockState(pos).is(BlockTags.LEAVES) || entityObj.level.getBlockState(pos).is(BlockTags.LOGS)) {
            entityObj.level.destroyBlock(pos, true);
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
        ((ServerLevel)entityObj.level).sendParticles(DustParticleOptions.REDSTONE, entityObj.getX(), entityObj.getY() + 1.5, entityObj.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
        walkingTimeout = walkingTimeoutMax;
        posCurrentWorkTarget = BlockPos.ZERO;
        curScanRange = 2;
        //stuckTimeout = 0;
    }

    public boolean operateOnTargetPosition(BlockPos pos) {
        BlockState state = entityObj.level.getBlockState(pos);
        if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_RIGHT_CLICK) {
            FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) entityObj.level, new GameProfile(UUID.randomUUID(), "Zombie_Player"));
            /*entityObj.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND,
                    BlockHitResult.miss(new Vec3(pos.getX(), pos.getY(), pos.getZ()), entityObj.getWorkInfo().getWorkClickDirectionLastObserved(), pos)));*/

            entityObj.getItemInHand(InteractionHand.MAIN_HAND).getItem().useOn(new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND,
                    BlockHitResult.miss(new Vec3(pos.getX(), pos.getY(), pos.getZ()), entityObj.getWorkInfo().getWorkClickDirectionLastObserved(), pos)));

            //entityObj.level.getBlockState(pos).use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), entityObj.getWorkInfo().getWorkClickDirectionLastObserved(), pos, true));
        } else if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_LEFT_CLICK) {
            BlockInfo info = getBlockInfo(state);
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
                //CULog.dbg("error no info on blockstate: " + state);

                //entityObj.level.destroyBlock(pos, true);

            }
        }

        entityObj.swing(InteractionHand.MAIN_HAND);
        entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));

        jobCompleteCooldown = entityObj.level.getGameTime() + jobCompleteCooldownValue;

        return true;
    }

    public BlockInfo getBlockInfo(BlockState state) {
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
        return info;
    }


}