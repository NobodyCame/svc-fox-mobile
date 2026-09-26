# Module architecture target

The preferred project direction is not "same mod id, replace the Simple Voice Call JAR".
The better long-term shape is a new controlling module that can coexist with the
Simple Voice Call mod and use Simple Voice Call compatibility only through a legacy bridge.

## Why not just replace the original id?

Fabric treats a mod id as the identity of a mod. Only one version of a mod id can
be loaded at a time. Keeping `simple-voice-call` as the final id is useful for a
drop-in replacement build, but it does not allow the original and the project to be
loaded side by side.

Fabric metadata dependency keys are also not a silent suppression mechanism:

- `depends` requires another mod.
- `suggests` is metadata only.
- `conflicts` logs a warning.
- `breaks` aborts loading when the other mod is present.

Therefore "suppress the original but keep it installed for legacy" must be done
with code, not only with `fabric.mod.json`.

## Target package

The project now uses a new mod id and package namespace:

- Mod id: `svc-fox-mobile`
- Java package: `dev.yukiinotenshi.simplephonepromax`
- Legacy/Simple Voice Call package: referenced only as an external compatibility target,
  not used as the project's main namespace.

Do not ship classes from the Simple Voice Call package in a side-by-side module. That
risks classpath collisions with the Simple Voice Call JAR.

## Runtime modes

### Standalone mode

Simple Voice Call mod absent.

The project runs its own UI, call manager and Simple Voice Chat integration. Legacy
compatibility is implemented by preserving the Simple Voice Call group protocol:

- call group: `C<encoded target UUID>`
- decline group: `D<encoded caller UUID>`
- optional password: group name

This is the safest default mode.

### Legacy bridge mode

Simple Voice Call mod present.

The project still owns the user experience and active call state. The Simple Voice Call mod
is treated as a legacy compatibility dependency only.

The bridge should:

1. Detect that `simple-voice-call` is loaded.
2. Apply optional mixins only when the original is present.
3. Suppress original entry points that open original screens or mutate original
   call state.
4. Keep the original protocol and config import paths available.
5. Avoid calling original internal code unless a fallback absolutely needs it.

## Suppressing the original

The original opens its GUI through a mixin into
`ClientPlayerInteractionManager#interactItem`.

The bridge should add its own higher-priority mixin into the same method:

1. Detect the original phone item, including custom-named iron ingot.
2. Open the project's screen.
3. Return success and cancel the interaction before the Simple Voice Call UI takes over.

Additional optional mixins should target original classes only in legacy bridge
mode:

- the original call manager's `tick` method
- original incoming-call screen creation paths
- original outgoing call creation paths

Those mixins should be guarded by a mixin plugin or by Fabric Loader checks, so
the module does not crash when the original is absent.

## What "legacy mode" should mean

Legacy mode should mean:

- preserve original phone item detection;
- import original JSON files;
- speak the original Simple Voice Chat group protocol;
- optionally interoperate with Simple Voice Call clients on a server.

Legacy mode should not mean:

- using the Simple Voice Call mod as the primary UI;
- letting original state machines fight with the project;
- depending on original internal Simple Voice Chat wrappers for normal calls.

## Recommended project direction

Keep this project as the real standalone project for now. A future bridge release
may intentionally make the Simple Voice Call JAR a required dependency, but only after
Simple Voice Call UI/state suppression is implemented.

If the codebase is split later, use this shape:

```text
simple-phone-pro-max-edition/
  legacy-baseline/          # current remapped decompile, for reference
  pro-max-module/            # real standalone module
  legacy-protocol/          # shared protocol tests/helpers
```

For the standalone module:

- `fabric.mod.json` uses `svc-fox-mobile`.
- Current state: `simple-voice-call` is `suggests`, not `depends`.
- Future bridge plan: move `simple-voice-call` to `depends` after the project can
  suppress Simple Voice Call GUI/state conflicts and use the Simple Voice Call mod as the legacy
  baseline.
- `voicechat` can stay a dependency if the module cannot function without Simple
  Voice Chat.
- compile against the Simple Voice Call JAR as `compileOnly` only for optional mixin
  target signatures.

See `docs/FUTURE_ORACLE_NUMBERS_AND_ORIGINAL_DEPENDENCY.md` for the planned
Oracle number registry, 3-digit short numbers, admin number assignment and
future hard dependency on the Simple Voice Call mod.

See `docs/ORIGINAL_1_0_0_1_21_11_REVIEW.md` for notes from the new original
1.0.0-1.21.11 patch: short deterministic numbers, 5-second call anti-spam,
blocked-player signals and bridge compatibility risks.

## First implementation steps

1. Keep the current package/resource rename in place.
2. Isolate Simple Voice Chat internals behind a stable adapter.
3. Add optional legacy config import from the original filenames.
4. Implement the higher-priority phone item mixin.
5. Add a guarded original-suppression mixin set if side-by-side original support
   becomes necessary.
6. Add tests for group-name encoding and original config import.
7. Implement the new UI/call manager against the project's own state.
