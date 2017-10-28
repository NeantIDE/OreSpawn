package com.mcmoddev.orespawn.impl.os3;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.api.BiomeLocation;
import com.mcmoddev.orespawn.api.os3.SpawnBuilder;
import com.mcmoddev.orespawn.util.BinaryTree;

import net.minecraft.block.state.IBlockState;

import com.mcmoddev.orespawn.api.os3.FeatureBuilder;
import com.mcmoddev.orespawn.api.os3.BiomeBuilder;
import com.mcmoddev.orespawn.api.os3.OreBuilder;

public class SpawnBuilderImpl implements SpawnBuilder {
	private BiomeLocation biomeLocs;
	private FeatureBuilder featureGen;
	private List<IBlockState> replacementBlocks;
	private List<OreBuilder> myOres;
	private BinaryTree oreSpawns;
	private boolean enabled = true;
	private boolean retrogen = false;
	
	public SpawnBuilderImpl() {
		this.biomeLocs = null;
		this.featureGen = null;
		this.replacementBlocks = new ArrayList<>();
		this.myOres = new ArrayList<>();
	}
	
	@Override
	public FeatureBuilder newFeatureBuilder(@Nullable String featureName) {
		String featName;
		this.featureGen = new FeatureBuilderImpl();
		if( OreSpawn.FEATURES.getFeature(featureName) == null || featureName == null) {
			featName = "default";
		} else {
			featName = featureName;
		}
		
		this.featureGen.setGenerator(featName);
		return this.featureGen;
	}

	@Override
	public BiomeBuilder newBiomeBuilder() {
		return new BiomeBuilderImpl();
	}

	@Override
	public OreBuilder newOreBuilder() {
		return new OreBuilderImpl();
	}
	
	@Override
	public SpawnBuilder create(BiomeBuilder biomes, FeatureBuilder feature,
			List<IBlockState> replacements, OreBuilder... ores) {
		this.biomeLocs = biomes.getBiomes();
		this.featureGen = feature;
		this.replacementBlocks.addAll(replacements);
		
		if( ores.length > 1 ) {
			for(int i = 0; i < ores.length; i++) {
				this.myOres.add(ores[i]);
			}
		} else {
			this.myOres.add(ores[0]);
		}
		return this;
	}

	@Override
	public BiomeLocation getBiomes() {
		return this.biomeLocs;
	}

	@Override
	public ImmutableList<OreBuilder> getOres() {
		return ImmutableList.<OreBuilder>copyOf(this.myOres);
	}

	@Override
	public ImmutableList<IBlockState> getReplacementBlocks() {
		return ImmutableList.<IBlockState>copyOf(this.replacementBlocks);
	}

	@Override
	public FeatureBuilder getFeatureGen() {
		return this.featureGen;
	}

	@Override
	public BinaryTree getOreSpawns() {
		if( this.oreSpawns == null ) {
			this.buildOreSpawnTree();
		}
		
		return this.oreSpawns;
	}

	private void buildOreSpawnTree() {
		if( this.myOres.size() > 1 ) {
			int maxVal = 0;
			for( OreBuilder os : this.myOres ) {
				maxVal += os.getChance();
			}

			int median = maxVal / 2;
			int count = 0;
			this.oreSpawns = new BinaryTree(median);
			for( OreBuilder os : this.myOres ) {
				count += os.getChance();
				this.oreSpawns.addNode(os, count);
			}
		} else {
			this.oreSpawns = new BinaryTree(50);
			this.oreSpawns.makeRoot(this.myOres.get(0));
		}
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean retrogen() {
		return this.retrogen;
	}

	@Override
	public void retrogen(boolean enabled) {
		this.retrogen = enabled;
	}
}
