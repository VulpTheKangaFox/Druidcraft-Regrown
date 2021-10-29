package com.vulp.druidcraftrg.blocks;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public class DoubleCropBlock extends CropsBlock {

    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
    private final VoxelShape[] SHAPE_ARRAY;

    public DoubleCropBlock(double radius, Properties properties) {
        super(properties);
        radius = MathHelper.clamp(radius, 0.0D, 8.0D);
        double a = 8.0D - radius;
        double b = 8.0D + radius;
        this.SHAPE_ARRAY = new VoxelShape[]{
                Block.box(a, 0.0D, a, b, 2.0D, b),
                Block.box(a, 0.0D, a, b, 4.0D, b),
                Block.box(a, 0.0D, a, b, 6.0D, b),
                Block.box(a, 0.0D, a, b, 8.0D, b),
                Block.box(a, 0.0D, a, b, 10.0D, b),
                Block.box(a, 0.0D, a, b, 12.0D, b),
                Block.box(a, 0.0D, a, b, 14.0D, b),
                Block.box(a, 0.0D, a, b, 16.0D, b)};
        this.registerDefaultState(this.defaultBlockState().setValue(BOTTOM, true));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return SHAPE_ARRAY[state.getValue(this.getAgeProperty())];
    }

    private boolean isBottom(BlockState state) {
        return state.getValue(BOTTOM);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        if (!this.isMaxAge(state) || this.canGrowUpward(world, state, pos)) {
            if (!world.isAreaLoaded(pos, 1)) return;
            if (world.getRawBrightness(pos, 0) >= 9) {
                float f = getGrowthSpeed(this, world, pos);
                if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(world, pos, state, rand.nextInt((int) (25.0F / f) + 1) == 0)) {
                    this.naturalGrowth(world, pos, state);
                    net.minecraftforge.common.ForgeHooks.onCropsGrowPost(world, pos, state);
                }
            }
        }

    }

    private boolean isAirAbove(IBlockReader reader, BlockPos pos) {
        return reader.getBlockState(pos.above()).getBlock() instanceof AirBlock;
    }

    private boolean isTopHalfAbove(IBlockReader reader, BlockPos pos) {
        BlockState state = reader.getBlockState(pos.above());
        return state.getBlock() == this && !state.getValue(BOTTOM);
    }

    @Override
    public void growCrops(World world, BlockPos pos, BlockState state) {
        int bonemeal = this.getBonemealAgeIncrease(world);
        if (this.isMaxAge(state) && this.isBottom(state)) {
            if (isAirAbove(world, pos)) {
                world.setBlock(pos.above(), this.getStateForAge(0).setValue(BOTTOM, false), 2);
            } else if (isTopHalfAbove(world, pos)) {
                BlockState aboveState = world.getBlockState(pos.above());
                int aboveAge = aboveState.getValue(AGE);
                if (!this.isMaxAge(aboveState)) {
                    world.setBlock(pos.above(), aboveState.setValue(AGE, MathHelper.clamp(aboveAge + bonemeal, 0, this.getMaxAge())), 2);
                }
            }
        } else if (!this.isMaxAge(state)) {
            world.setBlock(pos, this.getStateForAge(MathHelper.clamp(this.getAge(state) + bonemeal, 0, this.getMaxAge())).setValue(BOTTOM, state.getValue(BOTTOM)), 2);
        }
    }

    public void naturalGrowth(World world, BlockPos pos, BlockState state) {
        if (this.isMaxAge(state) && this.isBottom(state)) {
            if (isAirAbove(world, pos)) {
                world.setBlock(pos.above(), this.getStateForAge(0).setValue(BOTTOM, false), 2);
            }
        } else if (!this.isMaxAge(state)) {
            world.setBlock(pos, this.getStateForAge(this.getAge(state) + 1).setValue(BOTTOM, state.getValue(BOTTOM)), 2);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, IBlockReader reader, BlockPos pos) {
        return super.mayPlaceOn(state, reader, pos) || state.getBlock() == this && this.isMaxAge(state);
    }

    private boolean canGrowUpward(IBlockReader reader, BlockState state, BlockPos pos) {
        return state.getValue(BOTTOM) && isAirAbove(reader, pos);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockReader reader, BlockPos pos, BlockState state, boolean isRemote) {
        BlockState aboveState = reader.getBlockState(pos.above());
        return !this.isMaxAge(state) || this.canGrowUpward(reader, state, pos) || aboveState.getBlock() == this && !isMaxAge(aboveState);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> stateContainer) {
        stateContainer.add(AGE, BOTTOM);
    }
}