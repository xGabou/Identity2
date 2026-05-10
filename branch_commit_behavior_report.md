# Branch Commit/Behavior Presence Report

Generated: 2026-05-09

Updated after local port: `1.21.11`, `1.21.10`, `1.21.9`, and `1.21.8` now have the missing local `f45` behavior markers. Remote rows still reflect the last fetched remote refs until these branch changes are pushed.

Checked commits:

- `f45ab8500537c29aa17d39af5cb9f15d8135f185` - morph abilities and attack rendering, including Iron Golem / Ravager-style attack animation support.
- `872dafe6bea16cc996fe4e0b3d2ae85cc1253a50` - warden burrow and keepInventory safety.

`contains` means the exact commit is an ancestor of the branch/ref. `status` is based on behavior markers, so cherry-picks/backports can be `complete` even when `contains` is `no`.

## 1.21.4 Push Check

- Local `1.21.4`: `55f5bc2`
- Remote `origin/1.21.4`: `55f5bc2`
- Ahead/behind for `1.21.4...origin/1.21.4`: `0 0`
- Result: local `1.21.4` already has the pushed remote changes.

## Local Branches

| Branch | f45 contains | f45 status | Attack helper | Renderer hook | Golem render sync | Attack key/trigger | Ram mappings/tags | 872 contains | 872 status | Warden manager/toggle | Warden lifecycle exits | keepInventory helper/mixins | Notes |
|---|---:|---|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---|
| `1.20.1` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `1.20.4` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `1.20.5` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `1.20.6` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `1.21.1` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `1.21.1-ApolloMod` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `1.21.1-Cobblemon` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `1.21.10` | yes | complete | yes | yes | yes | yes/yes | yes | yes | complete | yes/yes | yes | yes/yes |  |
| `1.21.11` | no | complete | yes | yes | yes | yes/yes | yes | no | complete | yes/yes | yes | yes/yes | local adapted port; exact f45 commit is not an ancestor |
| `1.21.4` | no | complete | yes | yes | yes | yes/yes | yes | no | complete | yes/yes | yes | yes/yes |  |
| `1.21.8` | no | complete | yes | yes | yes | yes/yes | yes | no | complete | yes/yes | yes | yes/yes |  |
| `1.21.9` | no | complete | yes | yes | yes | yes/yes | yes | no | complete | yes/yes | yes | yes/yes |  |
| `1.21.9R` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `26.1.2` | yes | complete | yes | yes | yes | yes/yes | yes | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `master` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.1-from-1.20.6` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `temp/port-1.20.4` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.4-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.5` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.5-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.6` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.20.6-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `temp/port-1.21.1-from-1.21.10` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `temp/verify-1.21.1` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |

## Remote Branches

| Branch | f45 contains | f45 status | Attack helper | Renderer hook | Golem render sync | Attack key/trigger | Ram mappings/tags | 872 contains | 872 status | Warden manager/toggle | Warden lifecycle exits | keepInventory helper/mixins | Notes |
|---|---:|---|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---|
| `origin/1.20.1` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.20.4` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.20.5` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.20.6` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.21.1` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `origin/1.21.1-ApolloMod` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/1.21.1-Cobblemon` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/1.21.1-cobblemon` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/1.21.10` | yes | partial | yes | no | no | yes/yes | yes | yes | complete | yes/yes | yes | yes/yes | f45: attack render keywords present; ram attack keywords present; render helper reference present |
| `origin/1.21.11` | no | missing | no | no | no | no/no | no | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.21.4` | no | complete | yes | yes | yes | yes/yes | yes | no | complete | yes/yes | yes | yes/yes |  |
| `origin/1.21.8` | no | partial | yes | no | no | yes/yes | yes | no | complete | yes/yes | yes | yes/yes | f45: attack render keywords present; ram attack keywords present; render helper reference present |
| `origin/1.21.9` | no | partial | yes | no | no | yes/yes | yes | no | complete | yes/yes | yes | yes/yes | f45: attack render keywords present; ram attack keywords present; render helper reference present |
| `origin/1.21.9R` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/26.1.2` | yes | complete | yes | yes | yes | yes/yes | yes | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/OLD-1.21.9` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/master` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.1-from-1.20.6` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `origin/temp/port-1.20.4` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.4-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.5` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.5-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.6` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.20.6-clean2` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: noPhysics handling present |
| `origin/temp/port-1.21.1-from-1.21.10` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |
| `origin/temp/verify-1.21.1` | no | missing | no | no | no | no/no | no | no | alternate/keywords only | no/no | no | no/no | 872: keepInventory keywords present; noPhysics handling present |

## Interpretation

- `f45 complete` requires the shared attack render helper, the renderer hook that applies it, Iron Golem `attackAnimationTick` render sync, the synced attack animation key/trigger, and ram mappings/tags for goat/hoglin/ravager.
- Branches with f45 ability data but no renderer hook or no Iron Golem render sync are marked `partial`; those likely have ram/attack behavior but not the full visual attack rendering path.
- `872 complete` requires the warden burrow manager, secondary warden toggle, server tick/stop/respawn/damage/attack exits, and keepInventory-aware inventory/equipment drop protection.
- `alternate/keywords only` means related keywords exist, but the inspected code did not match enough behavior markers to call it an implementation.
