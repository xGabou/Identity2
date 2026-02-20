package net.Gabou.identity2.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class FluidPredicateArgumentType implements ArgumentType<FluidPredicateArgumentType.FluidPredicate> {
	
	private static final Collection<String> EXAMPLES = Arrays.asList("water", "minecraft:water", "#water");
	private final HolderLookup<Block> registryWrapper;

	public FluidPredicateArgumentType(CommandBuildContext commandRegistryAccess) {
		this.registryWrapper = commandRegistryAccess.lookupOrThrow(Registries.BLOCK);
	}

	public static FluidPredicateArgumentType fluidPredicate(CommandBuildContext commandRegistryAccess) {
		return new FluidPredicateArgumentType(commandRegistryAccess);
	}

	public FluidPredicateArgumentType.FluidPredicate parse(StringReader stringReader) throws CommandSyntaxException {
		return parse(this.registryWrapper, stringReader);
	}

	public static FluidPredicateArgumentType.FluidPredicate parse(HolderLookup<Block> registryWrapper, StringReader reader) throws CommandSyntaxException {//finish
		return BlockStateParser.parseForTesting(registryWrapper, reader, true)
			.map(
				result -> new FluidPredicateArgumentType.StatePredicate(result.blockState().getFluidState(), result.properties().keySet(), result.nbt()),
				result -> new FluidPredicateArgumentType.TagPredicate(result.tag(), result.vagueProperties(), result.nbt())
			);
	}

	public static Predicate<BlockInWorld> getFluidPredicate(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		return context.getArgument(name, FluidPredicateArgumentType.FluidPredicate.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return BlockStateParser.fillSuggestions(this.registryWrapper, builder, true, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public interface FluidPredicate extends Predicate<BlockInWorld> {
		boolean hasNbt();
	}

	static class StatePredicate implements FluidPredicateArgumentType.FluidPredicate {
		private final FluidState state;
		private final Set<Property<?>> properties;
		@Nullable
		private final CompoundTag nbt;

		public StatePredicate(FluidState state, Set<Property<?>> properties, @Nullable CompoundTag nbt) {
			this.state = state;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(BlockInWorld cachedBlockPosition) {
			FluidState fluidState = cachedBlockPosition.getState().getFluidState();
			if (!fluidState.is(this.state.getType())) {
				return false;
			} else {
				/*for (Property<?> property : this.properties) {
					if (fluidState.get(property) != this.state.get(property)) {
						return false;
					}
				}*/

				if (this.nbt == null) {
					return true;
				} else {
					return true;
				}
			}
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}

	/*static class TagPredicate implements FluidPredicateArgumentType.FluidPredicate {
		private final RegistryEntryList<Fluid> tag;
		@Nullable
		private final NbtCompound nbt;
		private final Map<String, String> properties;

		TagPredicate(RegistryEntryList<Fluid> tag, Map<String, String> properties, @Nullable NbtCompound nbt) {
			this.tag = tag;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(CachedBlockPosition cachedBlockPosition) {
			FluidState fluidState = cachedBlockPosition.getBlockState().getFluidState();
			if (!fluidState.isIn(this.tag)) {
				return false;
			} else {
				for (Entry<String, String> entry : this.properties.entrySet()) {
					Property<?> property = fluidState.getFluid().getStateManager().getProperty((String)entry.getKey());
					if (property == null) {
						return false;
					}

					Comparable<?> comparable = (Comparable<?>)property.parse((String)entry.getValue()).orElse(null);
					if (comparable == null) {
						return false;
					}

					if (fluidState.get(property) != comparable) {
						return false;
					}
				}

				if (this.nbt == null) {
					return true;
				} else {
					return false;
				}
			}
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}*/
	static class TagPredicate implements FluidPredicateArgumentType.FluidPredicate {
		private final HolderSet<Block> tag;
		@Nullable
		private final CompoundTag nbt;
		private final Map<String, String> properties;

		TagPredicate(HolderSet<Block> tag, Map<String, String> properties, @Nullable CompoundTag nbt) {
			this.tag = tag;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(BlockInWorld cachedBlockPosition) {
			return false;
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}
}