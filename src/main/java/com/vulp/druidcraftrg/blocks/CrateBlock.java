package com.vulp.druidcraftrg.blocks;

import com.vulp.druidcraftrg.DruidcraftRegrown;
import com.vulp.druidcraftrg.init.BlockInit;
import com.vulp.druidcraftrg.state.properties.CrateType;
import net.minecraft.block.*;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

public class CrateBlock extends ContainerBlock {

    public static final EnumProperty<CrateType> TYPE = EnumProperty.create("type", CrateType.class);
    private static final List<CrateType> NORTH_CRATES = CrateType.typeListFromDirection(Direction.NORTH);
    private static final List<CrateType> SOUTH_CRATES = CrateType.typeListFromDirection(Direction.SOUTH);
    private static final List<CrateType> EAST_CRATES = CrateType.typeListFromDirection(Direction.EAST);
    private static final List<CrateType> WEST_CRATES = CrateType.typeListFromDirection(Direction.WEST);
    private static final List<CrateType> UP_CRATES = CrateType.typeListFromDirection(Direction.UP);
    private static final List<CrateType> DOWN_CRATES = CrateType.typeListFromDirection(Direction.DOWN);
    private static final int[] CRATE_TYPE_START_POINTS = new int[]{0, 8, 12, 16, 20, 22, 24, 26};

    public CrateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(TYPE, CrateType.SMALL));
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader reader) {
        return null;
    }

    private boolean[] createCrateArray(World world, BlockPos pos) {
        boolean[] boolArray = new boolean[27];
        int ticker = 0;
        for (int y = -1; y < 2; y++) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    boolArray[ticker] = world.getBlockState(pos.offset(x, y, z)).getBlock() == this;
                    ticker++;
                }
            }
        }
        boolArray[13] = true;
        return boolArray;
    }

    // up/down = increments of 9, east/west = increments of 3.
    private CrateType detectCrateConfig(World world, BlockPos pos, boolean[] boolArray) {
        int x_increment = 3;
        int y_increment = 9;
        int centre = 13;
        int ticker = 0;

        for (int m = 0; m < 7; m++) {
            for (int j = 0; j < (m == 3 || (m > 3 && m != 6) ? 1 : 2); j++) {
                for (int i = 0; i < (m == 2 || (m > 3 && m != 5)  ? 1 : 2); i++) {
                    for (int k = 0; k < (m == 1 || (m > 3 && m != 4)  ? 1 : 2); k++) {
                        boolean latch = true;
                        // Large crate checker:
                        if (m == 0) {
                            for (int y = -j; y < 2 - j; y++) {
                                for (int x = -i; x < 2 - i; x++) {
                                    for (int z = -k; z < 2 - k; z++) {
                                        if (!boolArray[13 + (x * 3) + (y * 9) + z]) {
                                            latch = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else if (m < 4) {
                            // Wide crate checker:
                            for (int y = -j; y < (m == 3 ? 1 : 2 - j); y++) {
                                for (int x = -i; x < (m == 2 ? 1 : 2 - i); x++) {
                                    for (int z = -k; z < (m == 1 ? 1 : 2 - k); z++) {
                                        if (!boolArray[13 + (x * 3) + (y * 9) + z]) {
                                            latch = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int y = -j; y < (m != 6 ? 1 : 2 - j); y++) {
                                for (int x = -i; x < (m != 5 ? 1 : 2 - i); x++) {
                                    for (int z = -k; z < (m != 4 ? 1 : 2 - k); z++) {
                                        if (!boolArray[13 + (x * 3) + (y * 9) + z]) {
                                            latch = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (latch) {
                            CrateType placeType = CrateType.values()[ticker];
                            DruidcraftRegrown.LOGGER.debug("Crate attempted. [" + ticker + "]");
                            HashMap<BlockPos, CrateType> map = checkConfigValid(world, pos, placeType, ticker);
                            if (map != null) {
                                DruidcraftRegrown.LOGGER.debug("CRATE DETECTED! [" + ticker + "]");
                                map.forEach((blockPos, crateType) -> world.setBlock(blockPos, BlockInit.crate.defaultBlockState().setValue(TYPE, crateType), 2));
                                return placeType;
                            }
                        }
                        ticker++;
                    }
                }
            }
        }
        return CrateType.SMALL;
    }

    @Nullable
    private HashMap<BlockPos, CrateType> checkConfigValid(World world, BlockPos pos, CrateType type, int oldTicker) {
        HashMap<BlockPos, CrateType> map = new HashMap<>();
        List<Direction> directions = new LinkedList<>(Arrays.asList(type.getOpenDirections()));
        // Messy way of handling this, I know. Too lazy to mend it.
        List<Direction> oppositeCache = new ArrayList<>(Collections.emptyList());
        directions.removeIf(direction -> {
            Direction opposite = direction.getOpposite();
            oppositeCache.add(opposite);
            return directions.contains(opposite) || oppositeCache.contains(direction);
        });
        int[] integers = new int[]{0, 0, 0};
        for (Direction dir : directions) {
            integers[dir.getAxis().ordinal()] = dir.getStepX() + dir.getStepY() + dir.getStepZ();
        }
        // Below is an attempted fix to get the ticker to start at the start of its own crate type.
        for (int i = 0; i < 8; i++) {
            int finalOldTicker = oldTicker;
            if (Arrays.stream(CRATE_TYPE_START_POINTS).noneMatch(j -> j == finalOldTicker)) {
                oldTicker--;
            } else {
                break;
            }
        }
        int ticker = oldTicker;
        for (int y = 0; y < (integers[1] == 0 ? 1 : 2); y++) {
            for (int x = 0; x < (integers[0] == 0 ? 1 : 2); x++) {
                for (int z = 0; z < (integers[2] == 0 ? 1 : 2); z++) {
                    int xPos = pos.getX() + x - (integers[0] == -1 ? 1 : 0);
                    int yPos = pos.getY() + y - (integers[1] == -1 ? 1 : 0);
                    int zPos = pos.getZ() + z - (integers[2] == -1 ? 1 : 0);
                    BlockPos dynamicPos = new BlockPos(xPos, yPos, zPos);
                    if (dynamicPos.getX() != pos.getX() || dynamicPos.getY() != pos.getY() || dynamicPos.getZ() != pos.getZ()) {  // The last block placed will not turn here, as it is placed after the fact.
                        if (!world.isClientSide)
                            DruidcraftRegrown.LOGGER.debug(dynamicPos);
                        CrateType dynamicCrateType = CrateType.values()[ticker];
                        if (!world.isClientSide)
                            DruidcraftRegrown.LOGGER.debug(dynamicCrateType.getSerializedName());
                        for (Direction dir : dynamicCrateType.getOpenDirections()) {
                            BlockState state = world.getBlockState(dynamicPos);
                            if (state.getBlock() != this || Arrays.stream(state.getValue(TYPE).getOpenDirections()).noneMatch(openDir -> openDir == dir)) {
                                return null;
                            }
                        }
                        map.put(dynamicPos, CrateType.values()[ticker]);
                    }
                    ticker++;
                    // world.setBlock(new BlockPos(xPos, yPos, zPos), Blocks.GLOWSTONE.defaultBlockState(), 2);
                }
            }
        }
        return map;
    }

    private void debugArrayCreation(boolean[] boolArray) {
        for (int i = 0; i < 3; i++) {
            DruidcraftRegrown.LOGGER.debug("----");
            for (int j = 0; j < 3; j++) {
                int k = (i*9)+(j*3);
                DruidcraftRegrown.LOGGER.debug((boolArray[k] ? "O" : "/") + (boolArray[k+1] ? "O" : "/") + (boolArray[k+2] ? "O" : "/"));
            }
        }
        DruidcraftRegrown.LOGGER.debug("----");
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        CrateType type = CrateType.SMALL;
        if (context.canPlace()) {
            boolean[] boolArray = createCrateArray(world, pos);
            /*if (!world.isClientSide) {
                debugArrayCreation(boolArray);
            }*/
            type = detectCrateConfig(world, pos, boolArray);
        }
        BlockState state = super.getStateForPlacement(context);
        return state != null ? state.setValue(TYPE, type) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
