package net.Gabou.identity2.platform;

public interface ModRegistryPlatform {
    ModRegistryPlatform NOOP = new ModRegistryPlatform() {
        @Override
        public void registerIdentityAbilityRegistry() {
        }
    };

    void registerIdentityAbilityRegistry();
}
