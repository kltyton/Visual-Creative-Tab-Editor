# Creative Tab data pack format

Visual Creative Tab Editor reads server-data resources from:

```text
data/<namespace>/creative_tabs/<path>.json
```

The resource path is also the tab identifier. For example,
`data/example/creative_tabs/building/favorites.json` defines
`example:building/favorites`.

For Minecraft 1.20.1, a hand-written pack uses data-pack format 15:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Creative tab overrides"
  }
}
```

## Layering

Fields are merged independently in the real selected-pack order, from lowest to
highest priority. A higher pack can therefore change only an icon or title while
retaining the lower layer's items.

Each world contains two reserved directory packs:

- `visual_creative_tab_editor_generated_default` is regenerated from the final
  native and modded tabs and pinned below every other pack.
- `visual_creative_tab_editor_player_overrides` stores visual-editor differences
  and is pinned above every other pack.

The player pack stores field-level differences from the non-player result. New
tabs added by a later mod therefore continue to appear.

## Schema version 1

All fields other than `format` are optional in an overriding resource. A new tab
should provide a title, icon, and item list.

Setting `hidden` also removes that tab's `search_items` contribution from the
rebuilt global Search tab while preserving the tab identity.

```json
{
  "format": 1,
  "title": { "text": "Favorites" },
  "icon": { "id": "minecraft:diamond", "count": 1 },
  "items": [
    { "id": "minecraft:diamond", "count": 1 },
    {
      "id": "minecraft:diamond_sword",
      "count": 1,
      "tag": "{Damage:7}"
    }
  ],
  "search_items": [
    { "id": "minecraft:diamond", "count": 1 },
    {
      "id": "minecraft:diamond_sword",
      "count": 1,
      "tag": "{Damage:7}"
    }
  ],
  "hidden": false,
  "order": 20,
  "type": "category",
  "can_scroll": true,
  "show_title": true,
  "aligned_right": false,
  "background": "minecraft:textures/gui/container/creative_inventory/tab_items.png"
}
```

`title` uses Minecraft 1.20.1 component JSON and is read with
`Component.Serializer`. Each `icon`, `items`, or `search_items` entry is an
explicit object with these fields:

- `id`: required registered item ID.
- `count`: optional integer that defaults to `1`; resolved creative-tab data
  requires exactly `1`.
- `tag`: optional SNBT string containing the 1.20.1 ItemStack `CompoundTag`.

The `tag` value is a JSON string, not a nested JSON object. It preserves legacy
1.20.1 NBT such as enchantments, custom names, damage, and other item tags. For
category tabs, `items` is the visible parent-tab list and `search_items` is its
independent contribution to global search; this preserves Minecraft's
parent-only and search-only entries.

Layers merge field by field from low to high priority. A missing field inherits
the lower value; a present field replaces it as a whole. Arrays are not appended,
an empty array clears a list, and explicit `null` values are invalid.

Supported `type` values are:

- `category`
- `search`
- `hotbar`
- `inventory`

Only `category` may be introduced as a new runtime tab. Existing special tabs
retain their native behavior. In particular, `minecraft:hotbar`,
`minecraft:search`, and `minecraft:inventory` must retain their respective
`hotbar`, `search`, and `inventory` types. A search-tab `items` list is interpreted as a
preferred ordering: items not listed there, including items from newly installed
mods, are appended automatically.

## Limits and failure behavior

- 512 tabs per resolved catalog.
- 16384 stacks in `items` and 16384 stacks in `search_items` per tab, with 65536
  configured stacks total across the catalog.
- Unknown fields, invalid identifiers, empty stacks, invalid Component JSON,
  invalid SNBT, and unsupported schema versions fail the resource reload.
- Stale entries in the two reserved, Mod-owned world packs are skipped instead
  of preventing world startup (for example after a Mod is removed). The default
  pack is regenerated at server start; third-party packs remain strictly
  validated.
- A resolved catalog must retain at least one visible category tab.
- The combined base-and-resolved network snapshot must fit within 16 MiB
  expanded and 4 MiB compressed; an oversized resource reload is rejected.

On reload failure Minecraft retains the previous valid resource state. The
visual editor additionally uses a revision number so two administrators cannot
silently overwrite one another.
