package ember.qualitycommands;

import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import ember.qualitycommands.QualityCommands;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.component.ComponentType;
import com.mojang.serialization.Codec;
import java.util.function.UnaryOperator;
import net.minecraft.util.Unit;
public class ModComponents{
	public static void initialize(){
	}
	public static final ComponentType<String> USE_COMMAND_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "use_command"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<String> ON_ITEM_DESTROYED_COMMAND_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "item_entity_destroyed_command"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<String> INVENTORY_TICK_COMMAND_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "inv_tick_command"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<String> ON_CRAFT_ANY_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "on_craft"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<String> ON_CRAFT_CRAFTER_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "on_craft_crafter"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<String> ON_CRAFT_PLAYER_COMPONENT = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(QualityCommands.MOD_ID, "on_craft_player"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);





	public static final ComponentType<Unit> SOULBOUND = registerEnchantmentComponent("keep_on_death", builder -> builder.codec(Unit.CODEC));
	private static <T> ComponentType<T> registerEnchantmentComponent(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
		return Registry.register(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Identifier.ofVanilla(id), ((ComponentType.Builder)builderOperator.apply(ComponentType.builder())).build());
	}
}