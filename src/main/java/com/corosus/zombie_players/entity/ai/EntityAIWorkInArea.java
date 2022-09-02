package com.corosus.zombie_players.entity.ai;

import com.corosus.coroutil.util.CU;
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
import com.mojang.math.Vector3f;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.*;

public class EntityAIWorkInArea extends Goal
{
    private final ZombiePlayer entityObj;

    private int walkingTimeoutMax = 20*10;

    private int walkingTimeout;
    private int repathPentalty = 0;

    private int lookUpdateTimer = 0;

    private BlockPos posCurrentWorkTarget = BlockPos.ZERO;
    private BlockPos posNextWorkTarget = BlockPos.ZERO;
    private BlockPos posLastBadPath = BlockPos.ZERO;
    private long lastBadPathTime = 0;
    private long lastBadPathCooldown = 20*10;

    private long scanCooldownAmount = 5;
    private long scanNextTickWork = 0;

    private long jobCompleteCooldown = 0;
    private long jobCompleteCooldownValue = 100;

    private int curScanRange = 3;
    private int curAngle = 0;

    public static HashMap<Class, BlockInfo> lookupBlockClassToBlockInfo = new HashMap<>();
    public static HashMap<String, BlockInfo> lookupBlockTagToBlockInfo = new HashMap<>();

    static class BlockInfo {
        public Property property;
        public EnumBlockBreakBehaviorType blockBreakBehaviorType;

        public BlockInfo(Property property, EnumBlockBreakBehaviorType blockBreakBehaviorType) {
            this.property = property;
            this.blockBreakBehaviorType = blockBreakBehaviorType;
        }
    }

    static {
        //TODO: data driven style like: "minecraft:wheat[age=7]", see quark SimpleHarvestModule

        add(CropBlock.class, CropBlock.AGE, EnumBlockBreakBehaviorType.HARVEST);
        add(PotatoBlock.class, PotatoBlock.AGE, EnumBlockBreakBehaviorType.HARVEST);
        add(BeetrootBlock.class, BeetrootBlock.AGE, EnumBlockBreakBehaviorType.HARVEST);
        add(SugarCaneBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(CactusBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(GrowingPlantBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(BambooBlock.class, null, EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM);
        add(SweetBerryBushBlock.class, SweetBerryBushBlock.AGE, EnumBlockBreakBehaviorType.BREAK_NORMAL);

        add("logs", null, EnumBlockBreakBehaviorType.BREAK_VEINMINE_TREE);
        add("cave_vines", CaveVinesBlock.BERRIES, EnumBlockBreakBehaviorType.BREAK_NORMAL);
    }

    public static void add(String tag, Property property, EnumBlockBreakBehaviorType behaviorType) {
        lookupBlockTagToBlockInfo.put(tag, new BlockInfo(property, behaviorType));
    }

    public static void add(Class clazz, Property property, EnumBlockBreakBehaviorType behaviorType) {
        lookupBlockClassToBlockInfo.put(clazz, new BlockInfo(property, behaviorType));
    }

    public EntityAIWorkInArea(ZombiePlayer entityObjIn)
    {
        this.entityObj = entityObjIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse()
    {
        if (entityObj.isDepositingInChest()) return false;

        if (entityObj.needsMoreWorkItem()) return false;

        if (!entityObj.isCalm() || entityObj.getWorkInfo().isInTrainingMode() || entityObj.getWorkInfo().isInAreaSetMode() || jobCompleteCooldown >= entityObj.getLevel().getGameTime() || entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() == Blocks.AIR) return false;
        if (posNextWorkTarget != BlockPos.ZERO) {
            //CULog.dbg("using quickly found next work target");
            posCurrentWorkTarget = posNextWorkTarget;
            return true;
        }
        return canWorkOrFindWork();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse()
    {
        if (!entityObj.isCalm() || entityObj.getWorkInfo().isInTrainingMode() || jobCompleteCooldown >= entityObj.getLevel().getGameTime() || entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() == Blocks.AIR) return false;
        if (entityObj.needsMoreWorkItem()) return false;
        return !posCurrentWorkTarget.equals(BlockPos.ZERO);
    }

    public boolean canWorkOrFindWork() {
        if (scanNextTickWork < entityObj.getLevel().getGameTime()) {
            scanNextTickWork = entityObj.getLevel().getGameTime() + scanCooldownAmount;

            if (entityObj.getWorkInfo().isWorkAreaSet() && posCurrentWorkTarget.equals(BlockPos.ZERO)) {
                return !findWorkBlockPosRadial().equals(BlockPos.ZERO);
            }
            return false;
        } else {
            return false;
        }
    }

    public int getWorkDistance() {
        return entityObj.getWorkDistance();
    }

    public BlockPos findWorkBlockPosRadial() {
        //CULog.dbg("scan range: " + range);
        int lastX = 0;
        int lastZ = 0;

        if (curAngle >= 360) {
            curAngle = 0;

            curScanRange++;
            if (curScanRange > getWorkDistance()) {
                curScanRange = 1;
            }
        }

        int range = curScanRange;

        boolean looked = false;
        for (int angleTicks = 0; angleTicks < 90; angleTicks+=1) {
            curAngle += 1;
            int x = Mth.floor(-Mth.sin(curAngle * Mth.DEG_TO_RAD) * range);
            int z = Mth.floor(Mth.cos(curAngle * Mth.DEG_TO_RAD) * range);

            if (lastX == x && lastZ == z) continue;

            lastX = x;
            lastZ = z;

            //CULog.dbg("scan " + x + " - " + z);
            boolean dbgScan = entityObj.isShowWorkInfo();
            if (dbgScan) {
                BlockPos pos = this.entityObj.blockPosition().offset(x, 0, z);
                ((ServerLevel)entityObj.level).sendParticles(new DustParticleOptions(new Vector3f(0, 1, 0), 1f), pos.getX() + 0.5, pos.getY() - 4, pos.getZ() + 0.5, 1, 0.3D, 0D, 0.3D, 1D);
                ((ServerLevel)entityObj.level).sendParticles(new DustParticleOptions(new Vector3f(0, 1, 0), 1f), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, 0.3D, 0D, 0.3D, 1D);
                ((ServerLevel)entityObj.level).sendParticles(new DustParticleOptions(new Vector3f(0, 1, 0), 1f), pos.getX() + 0.5, pos.getY() + (getWorkDistance()/2), pos.getZ() + 0.5, 1, 0.3D, 0D, 0.3D, 1D);
            }

            for (int y = -4; y <= getWorkDistance()/2; y++) {

                BlockPos pos = this.entityObj.blockPosition().offset(x, y, z);

                if (!looked) {
                    looked = true;
                    if (CU.random.nextInt(5) == 0) {
                        entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.getX() + 0.5, this.entityObj.blockPosition().getY(), pos.getZ() + 0.5));
                    }
                }


                //CULog.dbg("probe: " + pos);
                if (isValidWorkBlock(pos)) {
                    if (entityObj.level.getBlockState(pos).is(BlockTags.LOGS)) {
                        if (!entityObj.level.getBlockState(pos.below()).is(BlockTags.LOGS) && !entityObj.level.getBlockState(pos.below()).is(BlockTags.LEAVES) && !entityObj.level.getBlockState(pos.below()).isAir()) {
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
        return BlockPos.ZERO;
    }

    public boolean isValidWorkBlock(BlockPos pos) {

        if (shouldSkipPos(pos)) return false;

        if (!entityObj.getWorkInfo().getPosWorkArea().contains(pos.getX(), pos.getY(), pos.getZ())) return false;

        BlockState state = entityObj.level.getBlockState(pos);

        /*CULog.dbg("dbg: " + pos);
        CULog.dbg("dbgstate: " + state);*/

        //TODO: problem, this line fails for cave vines cause theres 2 cave vines blocks, how to fix by matching getBlockInfo without breaking othing stuff?
        //if our info match is tag based, just do if state.is(tag) ?
        //would help with various tree matches too
        BlockInfo infoDesired = getBlockInfo(entityObj.getWorkInfo().getStateWorkLastObserved());
        BlockInfo info = getBlockInfo(state);
        /*if (state.getBlock() == Blocks.FARMLAND) {
            CULog.dbg("sdfsdfsdf");
        }*/
        if (state.getBlock() == entityObj.getWorkInfo().getStateWorkLastObserved().getBlock() || (infoDesired != null && info == infoDesired)) {
            boolean foundRuleForBlock = false;
            boolean successfullMatchPhase1 = false;

            //need to have special rules only when no special tool being used, otherwise things like trying to harvest with bonemeal in hands will happen
            if (info != null && entityObj.getWorkInfo().getItemNeededForWork().isEmpty()) {
                foundRuleForBlock = true;
            }

            if (!foundRuleForBlock) {
                successfullMatchPhase1 = true;
            }

            if (foundRuleForBlock) {
                if (info.property == null) {
                    successfullMatchPhase1 = true;
                }
                if (state.hasProperty(info.property)) {

                    //compare only targeted properties, since the block/tag matches from this point
                    if (entityObj.getWorkInfo().getStateWorkLastObserved().hasProperty(info.property) &&
                            entityObj.getWorkInfo().getStateWorkLastObserved().getValue(info.property) ==
                    state.getValue(info.property)) {
                        successfullMatchPhase1 = true;
                    }
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

                if (info != null) {
                    if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM) {
                        if (entityObj.level.getBlockState(pos.below()).getBlock() != entityObj.getWorkInfo().getStateWorkLastObserved().getBlock()) {
                            return false;
                        }
                    }
                }

                if (!foundRuleForBlock) {

                    if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_RIGHT_CLICK) {
                        //check if there is air on the side player clicked, allowing for an open area to click into
                        BlockPos posCheckForAir = pos.relative(entityObj.getWorkInfo().getWorkClickDirectionLastObserved());

                        if (!entityObj.level.getBlockState(posCheckForAir).isAir()) {
                            return false;
                        }
                    }

                    //allow left click breaking to just work, so no check here for that
                }

                CULog.dbg("found valid work block!: " + entityObj.level.getBlockState(pos));
                return true;
            }
        }
        return false;
    }

    public boolean isWithinRestrictions(BlockPos pos) {
        return entityObj.isWithinRestriction(pos) && this.entityObj.getWorkInfo().getPosWorkArea().contains(pos.getX(), pos.getY(), pos.getZ());
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

    @Override
    public void tick() {
        super.tick();

        if (entityObj.isShowWorkInfo()) {
            ((ServerLevel) entityObj.level).sendParticles(DustParticleOptions.REDSTONE, posCurrentWorkTarget.getX() + 0.5, posCurrentWorkTarget.getY() + 0.5, posCurrentWorkTarget.getZ() + 0.5, 2, 0.5D, 0.5D, 0.5D, 1D);
        }

        if (!posCurrentWorkTarget.equals(BlockPos.ZERO)) {
            boolean isClose = false;
            BlockPos blockposGoal = posCurrentWorkTarget;

            if (blockposGoal.equals(BlockPos.ZERO)) {
                stop();
                return;
            }

            double dist = entityObj.position().distanceTo(new Vec3(blockposGoal.getX(), blockposGoal.getY(), blockposGoal.getZ()));
            if (dist <= 3.8D) {
                if (operateOnTargetPosition(blockposGoal)) {
                    posNextWorkTarget = quickFindNeighborWorkBlock(posCurrentWorkTarget);

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
                        success = tryMoveToOpenSpotNextTo(new BlockPos(i, j, k));
                    }

                    if (!success) {
                        repathPentalty = 40;
                    } else {
                        walkingTimeout = walkingTimeoutMax;
                    }
                } else {
                    if (walkingTimeout > 0) {
                        walkingTimeout--;

                        entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(entityObj.getNavigation().getTargetPos().getX() + 0.5, entityObj.getNavigation().getTargetPos().getY() + 0.5, entityObj.getNavigation().getTargetPos().getZ() + 0.5));

                        if (walkingTimeout < walkingTimeoutMax / 4 * 3) {
                            if (walkingTimeout % 4 == 0) {
                                for (int y = 0; y <= 2; y++) {
                                    for (int x = -1; x < 2; x++) {
                                        trimLeaves(entityObj.blockPosition().offset(x, y, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, y, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, y, 0));
                                        trimLeaves(entityObj.blockPosition().offset(x, y, 0));

                                        trimLeaves(entityObj.blockPosition().offset(0, y, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, y, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, y, x));
                                        trimLeaves(entityObj.blockPosition().offset(0, y, x));
                                    }

                                }
                            }
                        }
                    } else {

                        lastBadPathTime = entityObj.level.getGameTime() + lastBadPathCooldown;
                        posLastBadPath = entityObj.getNavigation().getTargetPos();
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
        ((ServerLevel)entityObj.level).sendParticles(new DustParticleOptions(new Vector3f(0f, 0f, 1f), 1f), entityObj.getX(), entityObj.getY() + 1.5, entityObj.getZ(), 1, 0.3D, 0D, 0.3D, 1D);
        walkingTimeout = walkingTimeoutMax;
        posCurrentWorkTarget = BlockPos.ZERO;
        //posNextWorkTarget = BlockPos.ZERO;
        curScanRange = 1;
    }

    public boolean operateOnTargetPosition(BlockPos pos) {
        boolean performedAction = false;
        BlockState state = entityObj.level.getBlockState(pos);
        if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_RIGHT_CLICK) {
            FakePlayer fakePlayer = entityObj.getFakePlayer();

            if (!entityObj.getMainHandItem().isEmpty()) {
                //these 3 calls are based on the player interaction logic order that starts in Minecraft.startUseItem
                InteractionResult result = InteractionResult.PASS;
                if (entityObj.getWorkInfo().getBlockHitResult() != null) {
                    result = state.use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, entityObj.getWorkInfo().getBlockHitResult());
                    performedAction = true;
                }
                if (!result.consumesAction()) {
                    result = entityObj.getMainHandItem().useOn(new UseOnContext(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, entityObj.getMainHandItem(),
                            BlockHitResult.miss(new Vec3(pos.getX(), pos.getY(), pos.getZ()), entityObj.getWorkInfo().getWorkClickDirectionLastObserved(), pos)));
                    performedAction = true;
                }
                if (!result.consumesAction()) {
                    InteractionResultHolder resultHolder = entityObj.getMainHandItem().use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND);
                    if (resultHolder.getResult().consumesAction()) {
                        performedAction = true;
                    }
                }
            } else {
                if (entityObj.getWorkInfo().getBlockHitResult() != null) {
                    state.use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, entityObj.getWorkInfo().getBlockHitResult());
                    performedAction = true;
                }
            }

            //entityObj.level.getBlockState(pos).use(entityObj.level, fakePlayer, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), entityObj.getWorkInfo().getWorkClickDirectionLastObserved(), pos, true));
        } else if (entityObj.getWorkInfo().getWorkClickLastObserved() == EnumTrainType.BLOCK_LEFT_CLICK) {
            BlockInfo info = getBlockInfo(state);
            if (info != null) {
                if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_NORMAL) {
                    if (entityObj.level.getBlockState(pos).getDestroySpeed(entityObj.level, pos) >= 0) {
                        entityObj.level.destroyBlock(pos, true);
                        performedAction = true;
                    }
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.HARVEST) {
                    FakePlayer fakePlayer = entityObj.getFakePlayer();
                    if (!UtilCrops.harvestAndReplant(entityObj.level, pos, state, fakePlayer, entityObj)) {
                        entityObj.level.destroyBlock(pos, true);
                        performedAction = true;
                    }
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_VEINMINE_TREE) {
                    //TODO: says pos needs to be air for findTree, tweak this when you test
                    TreeCutter.Tree tree = TreeCutter.findTree(entityObj.level, pos);
                    for (BlockPos posEntry : tree.getLogs()) {
                        entityObj.level.destroyBlock(posEntry, true);
                        performedAction = true;
                    }
                    for (BlockPos posEntry : tree.getLeaves()) {
                        entityObj.level.destroyBlock(posEntry, true);
                        performedAction = true;
                    }
                    entityObj.level.destroyBlock(pos, true);
                } else if (info.blockBreakBehaviorType == EnumBlockBreakBehaviorType.BREAK_ALL_BUT_BOTTOM) {
                    //if (entityObj.level.getBlockState(pos.below()).getBlock() == entityObj.getWorkInfo().getStateWorkLastObserved().getBlock()) {
                        entityObj.level.destroyBlock(pos, true);
                        performedAction = true;
                    //}
                }
            } else {
                //CULog.dbg("error no info on blockstate: " + state);

                if (entityObj.level.getBlockState(pos).getDestroySpeed(entityObj.level, pos) >= 0) {
                    entityObj.level.destroyBlock(pos, true);
                    performedAction = true;
                }

            }
        }

        if (performedAction) {
            entityObj.swing(InteractionHand.MAIN_HAND);
            entityObj.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            ((ServerLevel)entityObj.level).sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3D, 0D, 0.3D, 1D);
        }


        jobCompleteCooldown = entityObj.level.getGameTime() + jobCompleteCooldownValue;
        jobCompleteCooldown = entityObj.level.getGameTime() + 30;
        posLastBadPath = BlockPos.ZERO;

        curScanRange = 1;


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

    public BlockPos quickFindNeighborWorkBlock(BlockPos completedJobPos) {
        List<Direction> list = new ArrayList<>();
        Arrays.stream(Direction.values()).forEach(p -> list.add(p));
        Collections.shuffle(list);
        for (Direction dir : list) {
            //if (dir != Direction.UP && dir != Direction.DOWN) {
                BlockPos pos = completedJobPos.relative(dir);
                if (isValidWorkBlock(pos)) {
                    //CULog.dbg("setup next work pos: " + pos);
                    return pos;
                }
            //}
        }
        //for repetitive tasks
        if (isValidWorkBlock(completedJobPos)) {
            return completedJobPos;
        }
        return BlockPos.ZERO;
    }

    public boolean shouldSkipPos(BlockPos pos) {
        if (entityObj.level.getGameTime() < lastBadPathTime) {
            if (pos == posLastBadPath) return true;
        }
        return false;
    }
}