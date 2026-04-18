# Identity2 FAQ

## Which Minecraft versions are supported here?

This bundle documents the current `1.21.1` branch only.

## Which loaders are officially supported here?

`Fabric` and `NeoForge`.

## Is Forge supported on this branch?

No official Forge metadata is present in the current branch.

## How do players open the morph menu?

Default keybind: `G`

## How do players use an identity ability?

Default keybind: `V`

## Do players need to unlock identities first?

By default, yes. The current code defaults `requireUnlockedIdentityForMorph` to `true`.

## Where is the config file?

`config/identity2.json`

## How do admins unlock identities?

Use `/identity unlock <identity_id> [target]` or `/identity unlock all [target]`.

## Does the mod support modded mobs?

Some modded entities can work through the generic morph and variant systems, but the repository does not publish an official per-mod compatibility list for this branch.

## Is there an addon API?

Yes. See `docs/public/ADDON_DEV_GUIDE.md`.

## Can config be edited in game?

Yes. Admins can use `/identity config ...`. Current code also saves those changes back to `config/identity2.json`.
