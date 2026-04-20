package ember.qualitycommands.commands.argument;

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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.BlockArgumentParser;

public class FluidPredicateArgumentType implements ArgumentType<FluidPredicateArgumentType.FluidPredicate> {
	
	private static final Collection<String> EXAMPLES = Arrays.asList("water", "minecraft:water", "#water");
	private final RegistryWrapper<Block> registryWrapper;

	public FluidPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) {
		this.registryWrapper = commandRegistryAccess.getOrThrow(RegistryKeys.BLOCK);
	}

	public static FluidPredicateArgumentType fluidPredicate(CommandRegistryAccess commandRegistryAccess) {
		return new FluidPredicateArgumentType(commandRegistryAccess);
	}

	public FluidPredicateArgumentType.FluidPredicate parse(StringReader stringReader) throws CommandSyntaxException {
		return parse(this.registryWrapper, stringReader);
	}

	public static FluidPredicateArgumentType.FluidPredicate parse(RegistryWrapper<Block> registryWrapper, StringReader reader) throws CommandSyntaxException {//finish
		return BlockArgumentParser.blockOrTag(registryWrapper, reader, true)
			.map(
				result -> new FluidPredicateArgumentType.StatePredicate(result.blockState().getFluidState(), result.properties().keySet(), result.nbt()),
				result -> new FluidPredicateArgumentType.TagPredicate(result.tag(), result.vagueProperties(), result.nbt())
			);
	}

	public static Predicate<CachedBlockPosition> getFluidPredicate(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
		return context.getArgument(name, FluidPredicateArgumentType.FluidPredicate.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return BlockArgumentParser.getSuggestions(this.registryWrapper, builder, true, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public interface FluidPredicate extends Predicate<CachedBlockPosition> {
		boolean hasNbt();
	}

	static class StatePredicate implements FluidPredicateArgumentType.FluidPredicate {
		private final FluidState state;
		private final Set<Property<?>> properties;
		@Nullable
		private final NbtCompound nbt;

		public StatePredicate(FluidState state, Set<Property<?>> properties, @Nullable NbtCompound nbt) {
			this.state = state;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(CachedBlockPosition cachedBlockPosition) {
			FluidState fluidState = cachedBlockPosition.getBlockState().getFluidState();
			if (!fluidState.isOf(this.state.getFluid())) {
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
		private final RegistryEntryList<Block> tag;
		@Nullable
		private final NbtCompound nbt;
		private final Map<String, String> properties;

		TagPredicate(RegistryEntryList<Block> tag, Map<String, String> properties, @Nullable NbtCompound nbt) {
			this.tag = tag;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(CachedBlockPosition cachedBlockPosition) {
			return false;
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}
}