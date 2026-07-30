# Oversized Identity Unlock Payload Root Cause and Port Plan

## Root Cause

Unlocked identity variants were stored and synced as broad entity NBT instead of stable variant-only data. Noisy runtime fields such as attributes, combat memory, anger state, equipment, effects, and positions produced many duplicate variant tokens and made the legacy full-NBT unlock sync payload exceed packet limits.

## Long-Term Fix

- Store variant unlocks as canonical tokens only.
- Sanitize existing saved tokens by decoding, allowlisting stable variant keys, re-encoding, and deduplicating.
- Sync unlock state with chunked `IdentityUnlockSyncS2CPacketPayload` packets instead of the legacy full-NBT `UnlockedIdentitySyncS2CPacketPayload`.
- Treat oversized single variant tokens as corrupt/unsupported, skip only that token, and keep syncing the rest.

## Test Branch

This fix was ported to `identity2-1.21.11` first for live testing after proving the approach on `identity2-branch-1.21.1`.

## Port Matrix

- Port forward/back across supported branches after 1.21.11 testing passes.
- Do not touch `1.21.9R`, `1.21.1-ApolloMod`, or `1.21.1-Cobblemon` unless explicitly requested.

## Verification

- Join with a player that has noisy historical variant tokens and verify the data is repaired on sync.
- Confirm the morph menu still shows unlocked identities and variants after reconnect.
- Confirm large unlock lists split into multiple packets and reconstruct correctly client-side.
- Confirm no `Blocked oversized unlocked identity payload` log appears.
- Run the branch build after each port.
