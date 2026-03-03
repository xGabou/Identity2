package draylar.identity.forge.ability;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import draylar.identity.ability.AbilityRegistry;
import draylar.identity.forge.IdentityForge;
import draylar.identity.forge.ability.impl.*;
//import net.minecraft.entity.EntityType;


public class AlexsMobsAbilityRegistry {



    private AlexsMobsAbilityRegistry() {}

    public static void init() {
        if (!IdentityForge.isAlexsMobsLoaded) {
            return;
        }

        // Call .get() at the point of calling   AbilityRegistry.register
          AbilityRegistry.register(AMEntityRegistry.ANACONDA.get(), new AnacondaAbility());
          AbilityRegistry.register(AMEntityRegistry.BALD_EAGLE.get(), new BaldEagleAbility());
          AbilityRegistry.register(AMEntityRegistry.BONE_SERPENT.get(), new BoneSerpentAbility());
          AbilityRegistry.register(AMEntityRegistry.COCKROACH.get(),new CockRoachAbility());
          AbilityRegistry.register(AMEntityRegistry.CRIMSON_MOSQUITO.get(), new CrimsonMosquitoAbility());
          AbilityRegistry.register(AMEntityRegistry.CROCODILE.get(), new CrocodileAbility());
          AbilityRegistry.register(AMEntityRegistry.CROW.get(), new CrowAbility());
          AbilityRegistry.register(AMEntityRegistry.DROPBEAR.get(), new DropBearAbility());
          AbilityRegistry.register(AMEntityRegistry.ELEPHANT.get(), new ElephantAbility());
          AbilityRegistry.register(AMEntityRegistry.EMU.get(), new EmuAbility());
          AbilityRegistry.register(AMEntityRegistry.ENDERIOPHAGE.get(), new EnderiophageAbility());
          AbilityRegistry.register(AMEntityRegistry.FLY.get(), new FlyAbility());
          AbilityRegistry.register(AMEntityRegistry.GIANT_SQUID.get(), new GiantSquidAbility());
          AbilityRegistry.register(AMEntityRegistry.GORILLA.get(), new GorillaAbility());
          AbilityRegistry.register(AMEntityRegistry.GRIZZLY_BEAR.get(), new GrizzlyBearAbility());
          AbilityRegistry.register(AMEntityRegistry.GUSTER.get(), new GusterAbility());
          AbilityRegistry.register(AMEntityRegistry.HUMMINGBIRD.get(), new HummingbirdAbility());
          AbilityRegistry.register(AMEntityRegistry.KANGAROO.get(), new KangarooAbility());
          AbilityRegistry.register(AMEntityRegistry.KOMODO_DRAGON.get(), new KomodoDragonAbility());
          AbilityRegistry.register(AMEntityRegistry.MIMICUBE.get(), new MimicubeAbility());
          AbilityRegistry.register(AMEntityRegistry.MOOSE.get(), new MooseAbility());
          AbilityRegistry.register(AMEntityRegistry.ORCA.get(), new OrcaAbility());
          AbilityRegistry.register(AMEntityRegistry.RACCOON.get(), new RaccoonAbility());
          AbilityRegistry.register(AMEntityRegistry.RATTLESNAKE.get(), new RattlesnakeAbility());
          AbilityRegistry.register(AMEntityRegistry.ROADRUNNER.get(), new RoadrunnerAbility());
          AbilityRegistry.register(AMEntityRegistry.SKUNK.get(), new SkunkAbility());
          AbilityRegistry.register(AMEntityRegistry.SNOW_LEOPARD.get(), new SnowLeopardAbility());
          AbilityRegistry.register(AMEntityRegistry.SOUL_VULTURE.get(), new SoulVultureAbility());
          AbilityRegistry.register(AMEntityRegistry.SPECTRE.get(), new SpectreAbility());
          AbilityRegistry.register(AMEntityRegistry.SUNBIRD.get(), new SunbirdAbility());
          AbilityRegistry.register(AMEntityRegistry.TARANTULA_HAWK.get(), new TarantulaHawkAbility());
          AbilityRegistry.register(AMEntityRegistry.TASMANIAN_DEVIL.get(), new TasmanianDevilAbility());
          AbilityRegistry.register(AMEntityRegistry.TIGER.get(), new TigerAbility());
          AbilityRegistry.register(AMEntityRegistry.VOID_WORM.get(), new VoidWormAbility());
          AbilityRegistry.register(AMEntityRegistry.WARPED_MOSCO.get(), new WarpedMoscoAbility());
    }


}
