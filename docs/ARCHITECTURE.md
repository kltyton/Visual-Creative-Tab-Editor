# Architecture

## Version baseline

This branch targets Minecraft 1.20.1 on Java 17. Its loader modules are Fabric
0.16.9 with Fabric API 0.92.1+1.20.1, and Forge 47.2.30. Shared Gradle
conventions are implemented in `buildSrc/`.

The built-in creative-tab registry remains frozen. Registered vanilla and modded
tab objects keep their identity, while a reloadable immutable catalog supplies
their title, icon, visibility, order, layout, and contents. JSON-only category
tabs are runtime objects outside the registry and are projected into the Fabric
and Forge creative-page implementations.

## Server flow

1. A new world starts with native creative tabs as a safe fallback.
2. At `SERVER_STARTED`, native contents are rebuilt while this mod's projection
   is bypassed. This captures Fabric/Forge tab events and other mods' final
   contributions.
3. The resulting definitions are written to the pinned lowest pack with
   per-file atomic replacement, and obsolete JSON files are removed.
4. The pack repository is refreshed and resources are reloaded once if content
   changed.
5. Reloaded resources are merged by pack stack and synchronized to clients.

The snapshot contains both the non-player base and the effective result. The
editor sends a bounded, compressed target catalog. The server checks operator
permission, revision, identifiers, limits, tab types, and item stacks before
writing a minimal highest-layer difference pack.

## Network limits

Both directions use 24 KiB chunks and GZIP. Logical payloads are limited to 171
chunks, 4 MiB compressed, and 16 MiB expanded. A resource reload is rejected if
the combined base-and-resolved snapshot cannot fit those wire limits.
Client-to-server data never contains a filesystem path or arbitrary output
filename.

The common payload records expose a channel ID and encode/decode through
`FriendlyByteBuf`. Fabric binds those records to Fabric networking channels;
Forge binds them to its `SimpleChannel`. Registration, packet direction, and
game-thread scheduling remain loader-specific.

## Client flow

The creative screen holds a draft catalog during editing. Preview state is
thread-local to the render thread, which avoids leaking an integrated-server
preview into the logical server. Saving submits the draft; canceling discards it.
Fabric and Forge keep their own pagination state, with thin loader-specific
ordering adapters.

## Access policy

Members that only need wider Java access use loader-native transformation:

- Fabric Loader 0.16.9 / Loom 1.9 reads
  `visual_creative_tab_editor.accesswidener` in the `named` mapping namespace.
- The common module and Forge artifact both declare the required SRG-named
  rules in their respective `META-INF/accesstransformer.cfg` files. The common
  copy is applied by legacy ModDev before source remapping.

Mixin accessors and invokers are not used. Mixins remain only where behavior
must change, such as data-driven method results, input interception, creative
cache rebuild hooks, and the virtual item-tail row calculation.

Shared Gradle conventions live in `buildSrc/`. The loader modules consume the
same common Java and resources without introducing a runtime abstraction
library.

## Rename compatibility

World packs using the previous directory names are moved to the
`visual_creative_tab_editor_*` names on first start. If a directory move cannot
be completed, the pinned pack source can still read the legacy location. Custom
tab identifiers in the previous `one_enough_creative_tab` namespace remain
editable so an existing world does not lose its visual-editor layout.
If both directory names already exist, the current directory wins, the legacy
directory is preserved, and a warning is logged instead of silently merging
potentially conflicting player edits.
