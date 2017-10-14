package com.mcmoddev.orespawn.api;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.data.ReplacementsRegistry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public abstract class FeatureBase {
	protected final int MAX_CACHE_SIZE = 1024;
	/** overflow cache so that ores that spawn at edge of chunk can 
	 * appear in the neighboring chunk without triggering a chunk-load */
	protected final Map<Vec3i,Map<BlockPos,IBlockState>> overflowCache = new HashMap<>(MAX_CACHE_SIZE);
	protected final Deque<Vec3i> cacheOrder = new LinkedList<>();
	protected Random random;
	
	public FeatureBase( Random rand ) {
		this.random = rand;
	}
	
	protected void runCache(int chunkX, int chunkZ, World world, IBlockState blockReplace) {
		Vec3i chunkCoord = new Vec3i(chunkX, chunkZ, world.provider.getDimension());
		Map<BlockPos,IBlockState> cache = retrieveCache(chunkCoord);
		
		if( !cache.isEmpty() ) { // if there is something in the cache, try to spawn it
			for(Entry<BlockPos,IBlockState> ent : cache.entrySet()){
				spawn(cache.get(ent.getKey()),world,ent.getKey(),world.provider.getDimension(),false,blockReplace);
			}
		}
	}
	
	protected void spawn(IBlockState oreBlock, World world, BlockPos coord, int dimension, boolean cacheOverflow,
			IBlockState blockReplace) {
		if( oreBlock == null ) {
			OreSpawn.LOGGER.fatal("FeatureBase.spawn() called with a null ore!");
			return;
		}
		
		IBlockState blockToReplace = blockReplace;
		if(blockToReplace == null) {
			blockToReplace = ReplacementsRegistry.getDimensionDefault(world.provider.getDimension());
		}
		if(blockToReplace == null) {
			OreSpawn.LOGGER.fatal("called to spawn %s, replaceBlock is null and the registry says there is no default", oreBlock);
			return;
		}
		if(coord.getY() < 0 || coord.getY() >= world.getHeight()) return;
		if(world.isBlockLoaded(coord)){
			IBlockState targetBlock = world.getBlockState(coord);
			if(canReplace(targetBlock,blockToReplace)) {
				world.setBlockState(coord, oreBlock, 2);
			}
		} else if(cacheOverflow){
			cacheOverflowBlock(oreBlock,coord,dimension);
		}
	}

	protected void cacheOverflowBlock(IBlockState bs, BlockPos coord, int dimension){
		Vec3i chunkCoord = new Vec3i(coord.getX() >> 4, coord.getY() >> 4, dimension);
		if(overflowCache.containsKey(chunkCoord)){
			cacheOrder.addLast(chunkCoord);
			if(cacheOrder.size() > MAX_CACHE_SIZE){
				Vec3i drop = cacheOrder.removeFirst();
				overflowCache.get(drop).clear();
				overflowCache.remove(drop);
			}
			overflowCache.put(chunkCoord, new HashMap<BlockPos,IBlockState>());
		}
		Map<BlockPos,IBlockState> cache = overflowCache.get(chunkCoord);
		cache.put(coord, bs);
	}

	protected Map<BlockPos,IBlockState> retrieveCache(Vec3i chunkCoord ){
		if(overflowCache.containsKey(chunkCoord)){
			Map<BlockPos,IBlockState> cache =overflowCache.get(chunkCoord);
			cacheOrder.remove(chunkCoord);
			overflowCache.remove(chunkCoord);
			return cache;
		} else {
			return Collections.<BlockPos,IBlockState>emptyMap();
		}
	}
	
	protected void scramble(int[] target, Random prng) {
		for(int i = target.length - 1; i > 0; i--){
			int n = prng.nextInt(i);
			int temp = target[i];
			target[i] = target[n];
			target[n] = temp;
		}
	}

	protected boolean canReplace(IBlockState target, IBlockState toReplace) {
		if( target.getBlock().equals(Blocks.AIR) ) {
			return false;
		} else if( toReplace.equals(target) ) {
			return true;
		}
		return false;
	}

	protected static final Vec3i[] offsets_small = {
			new Vec3i( 0, 0, 0),new Vec3i( 1, 0, 0),
			new Vec3i( 0, 1, 0),new Vec3i( 1, 1, 0),

			new Vec3i( 0, 0, 1),new Vec3i( 1, 0, 1),
			new Vec3i( 0, 1, 1),new Vec3i( 1, 1, 1)
	};
	
	protected static final Vec3i[] offsets = {
			new Vec3i(-1,-1,-1),new Vec3i( 0,-1,-1),new Vec3i( 1,-1,-1),
			new Vec3i(-1, 0,-1),new Vec3i( 0, 0,-1),new Vec3i( 1, 0,-1),
			new Vec3i(-1, 1,-1),new Vec3i( 0, 1,-1),new Vec3i( 1, 1,-1),

			new Vec3i(-1,-1, 0),new Vec3i( 0,-1, 0),new Vec3i( 1,-1, 0),
			new Vec3i(-1, 0, 0),new Vec3i( 0, 0, 0),new Vec3i( 1, 0, 0),
			new Vec3i(-1, 1, 0),new Vec3i( 0, 1, 0),new Vec3i( 1, 1, 0),

			new Vec3i(-1,-1, 1),new Vec3i( 0,-1, 1),new Vec3i( 1,-1, 1),
			new Vec3i(-1, 0, 1),new Vec3i( 0, 0, 1),new Vec3i( 1, 0, 1),
			new Vec3i(-1, 1, 1),new Vec3i( 0, 1, 1),new Vec3i( 1, 1, 1)
	};
	
	protected static final int[] offsetIndexRef = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26};
	protected static final int[] offsetIndexRef_small = {0,1,2,3,4,5,6,7};
}
