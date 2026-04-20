package ember.qualitycommands.util;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;

public record IdentityAbilityDefinition(RegistryEntry<Item> icon,String command,int cooldown,int useduration,Identifier bultinability,boolean override_attack) {
    
}
