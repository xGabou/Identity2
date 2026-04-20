package ember.qualitycommands.blocks;

import ember.qualitycommands.ModBlocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.util.math.Direction;

public class MagicBarrierBlock extends BarrierBlock {
	public MagicBarrierBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if(ModBlocks.MAGIC_BARRIER_BLOCK==null){
			return (false||context.isAbove(VoxelShapes.fullCube(),pos.add(0,99999,0),true)) ? VoxelShapes.fullCube() : VoxelShapes.empty();
		}
		else{
			return (context.isHolding(ModBlocks.MAGIC_BARRIER_BLOCK.asItem())||context.isAbove(VoxelShapes.fullCube(),pos.add(0,99999,0),true)) ? VoxelShapes.fullCube() : VoxelShapes.empty();
		}
	}
	public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
		return true;
	}
    @Override
	public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}
	@Override
	public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 1.0F;
	}
    @Override
	public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return false;//stateFrom.isOf(this) ? true : state.isSideInvisible(stateFrom, direction);
	}
}