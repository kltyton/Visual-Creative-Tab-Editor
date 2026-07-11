# Architecture

The built-in creative-tab registry remains frozen. Registered vanilla and modded
tab objects keep their identity, while a reloadable immutable catalog supplies
their title, icon, visibility, order, layout, and contents. JSON-only category
tabs are runtime objects outside the registry and are projected into Fabric,
NeoForge, and Forge native page implementations.

## Server flow

1. A new world starts with native creative tabs as a safe fallback.
2. At `SERVER_STARTED`, native contents are rebuilt while this mod's projection
   is bypassed. This captures Fabric/NeoForge/Forge tab events and other mods' final
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

## Client flow

The creative screen holds a draft catalog during editing. Preview state is
thread-local to the render thread, which avoids leaking an integrated-server
preview into the logical server. Saving submits the draft; canceling discards it.
Fabric, NeoForge, and Forge keep their own native pagination, with thin
loader-specific ordering adapters.

## Access policy

Members that only need wider Java access use loader-native transformation:

- Fabric Loader 0.16 / Loom 1.9 reads
  `visual_creative_tab_editor.accesswidener`;
- NeoForge reads the common `META-INF/accesstransformer.cfg` plus the
  NeoForge-only `META-INF/neoforge-accesstransformer.cfg` for its patched page
  list;
- Forge reads its loader-specific `META-INF/accesstransformer.cfg`, whose
  Minecraft members use Forge-required SRG names and which also exposes the
  Forge-patched page list.

Mixin accessors and invokers are not used. Mixins remain only where behavior
must change, such as data-driven method results, input interception, creative
cache rebuild hooks, and the virtual item-tail row calculation.

## Rename compatibility

World packs using the previous directory names are moved to the
`visual_creative_tab_editor_*` names on first start. If a directory move cannot
be completed, the pinned pack source can still read the legacy location. Custom
tab identifiers in the previous `one_enough_creative_tab` namespace remain
editable so an existing world does not lose its visual-editor layout.
If both directory names already exist, the current directory wins, the legacy
directory is preserved, and a warning is logged instead of silently merging
potentially conflicting player edits.
