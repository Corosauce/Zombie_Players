package com.corosus.zombie_players.util;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.material.Material;

import java.util.function.BiPredicate;

public class UtilScanner {

    public static BlockPos findBlock(LivingEntity entity, int scanRange, int yVariance, int tries, BiPredicate<Level, BlockPos> predicate) {

        int scanSize = scanRange;
        //int scanSizeY = scanRange / 2;
        int adjustRangeY = 10;

        int tryX;
        int tryY = Mth.floor(entity.posY);
        int tryZ;

        for (int ii = 0; ii <= tries; ii++) {
            //try close to entity first few times
            if (ii <= 3) {
                scanSize = 20;
                //scanSizeY = 10 / 2;
            } else {
                scanSize = scanRange;
                //scanSizeY = scanRange / 2;
            }
            tryX = Mth.floor(entity.getX()) + (entity.getLevel().random.nextInt(scanSize)-scanSize/2);
            int i = tryY + (yVariance > 0 ? (entity.getLevel().random.nextInt(yVariance)-entity.getLevel().random.nextInt(yVariance)) : 0);
            tryZ = Mth.floor(entity.getZ()) + (entity.getLevel().random.nextInt(scanSize)-scanSize/2);
            BlockPos posTry = new BlockPos(tryX, tryY, tryZ);

            boolean foundBlock = false;
            int newY = i;

            if (!entity.getLevel().isEmptyBlock(posTry)) {
                //scan up
                int tryMax = adjustRangeY;
                while (!entity.getLevel().isEmptyBlock(posTry) && tryMax-- > 0) {
                    newY++;
                    posTry = new BlockPos(tryX, newY, tryZ);
                }

                //if found air and water below it
                /*if (entity.getLevel().isEmptyBlock(posTry) && entity.getLevel().getBlockState(posTry.add(0, -1, 0)).getMaterial().isLiquid()) {
                    foundWater = true;
                }*/

                if (entity.getLevel().isEmptyBlock(posTry) && predicate.test(entity.getLevel(), posTry.offset(0, -1, 0))) {
                    foundBlock = true;
                    posTry = posTry.offset(0, -1, 0);
                }
            } else {
                //scan down
                int tryMax = adjustRangeY;
                while (entity.getLevel().isEmptyBlock(posTry) && tryMax-- > 0) {
                    newY--;
                    posTry = new BlockPos(tryX, newY, tryZ);
                }
                /*if (!entity.getLevel().isEmptyBlock(posTry) && entity.getLevel().getBlockState(posTry.add(0, 1, 0)).getMaterial().isLiquid()) {
                    foundWater = true;
                }*/
                if (entity.getLevel().isEmptyBlock(posTry.offset(0, 1, 0)) && predicate.test(entity.getLevel(), posTry)) {
                    foundBlock = true;
                }
            }

            if (foundBlock) {
                return posTry;
            }
        }

        return null;
    }

    public static boolean isWater(Level world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial().isLiquid();
    }

    public static boolean isDeepWater(Level world, BlockPos pos) {
        boolean clearAbove = world.isEmptyBlock(pos.above(1)) && world.isEmptyBlock(pos.above(2)) && world.isEmptyBlock(pos.above(3));
        boolean deep = world.getBlockState(pos).getMaterial().isLiquid() && world.getBlockState(pos.below()).getMaterial().isLiquid();
        boolean notUnderground = false;
        if (deep) {
            int height = world.getPrecipitationHeight(pos).getY() - 1;
            notUnderground = height == pos.getY();
        }

        return deep && notUnderground && clearAbove;
    }

    public static boolean isLand(Level world, BlockPos pos) {
        return world.getBlockState(pos).isSideSolid(world, pos, FaceInfo.UP);
    }

    public static boolean isFire(Level world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.FIRE;
    }

    public static boolean isChest(Level world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof ChestBlock;
    }
}
