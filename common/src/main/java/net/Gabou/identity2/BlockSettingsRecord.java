package net.Gabou.identity2;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.Gabou.identity2.Identity2;
import net.minecraft.registry.Registry;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import java.util.function.Function;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
public class BlockSettingsRecord{
    public boolean noCollision;
    public String id;
    public String namespace;
    public BlockSettingsRecord(boolean ncl,String id,String namespace){
        net.Gabou.identity2.Identity2.LOGGER.info("created new block settings record with id "+id);
        this.noCollision=ncl;
        this.id=id;
        this.namespace=namespace;
        //if(((SimpleRegistry)Registries.BLOCK).frozen==false)
        //net.Gabou.identity2.ModRegistries.convertBlockSettingsRecordToBlock(this,Registries.BLOCK);
        
    }
    public boolean noCollision(){return noCollision;}
    public String id(){return id;}
    public String namespace(){return namespace;}
}