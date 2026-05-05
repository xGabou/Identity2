package ember.qualitycommands.util;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;

public record IdentityAbilityDefinition(RegistryEntry<Item> icon,String command,int cooldown,int useduration,ResourceLocation bultinability,boolean override_attack) {
    
}
