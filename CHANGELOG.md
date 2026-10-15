# 2.2.0

## English

- Adds automatic Bedrock-style item groups with distinct group and member cells and lower-right expand/collapse markers. Grouping is enabled by default and can be disabled in `config/visual_creative_tab_editor/client.json`.
- Adds group creation, names, icons, membership editing and whole-group dragging through the existing long-press editor. Group definitions use layout saving, server synchronization and undo history.
- Adds a Groups control panel for creating, editing and deleting groups and adding or removing members. Deleting a group keeps its items.
- Refreshes only creative-tab resources when initializing the generated world packs.
- Keeps NeoForge's fixed system tabs out of ordinary tab pages.
- Preserves native empty display slots used by section banners and aligns Propulsion's banners with item grouping and scrolling.
- Closing a creative inventory without an active edit preview no longer rebuilds tab contents and the search collection.

## 中文

- 新增基岩版风格的同类物品自动分组，使用独立组头、成员底色及右下角展开/折叠标记。默认开启，可在 `config/visual_creative_tab_editor/client.json` 关闭。
- 在原有长按编辑器中新增分组创建、名称与图标修改、成员调整及整组拖动，沿用布局保存、服务端同步和撤销记录。
- 新增“物品组”面板，可新建、编辑、删除物品组及移入、移出成员。删除物品组时保留其中的物品。
- 初始化世界生成数据包时只刷新创造标签资源。
- NeoForge 的固定系统标签不再进入普通分页。
- 保留原生分区标题使用的空槽，并同步推进工程分区标题与物品分组、滚动的位置。
- 关闭创造物品栏时，没有活动编辑预览就不再重建标签内容与搜索集合。

# 2.1.1

## English

- Fixes the NeoForge 1.21.1 startup crash caused by the creative-tab title input's EditBox Mixin. The editor now uses NeoForge's native text-shadow setting and preserves shadow-free title editing.
- Migrates the project to KltytonTemple with shared editing logic and nine independent Fabric, Forge and NeoForge build targets for Minecraft 1.20.1, 1.21.1, 26.1.2 and 26.2.

## 中文

- 修复 NeoForge 1.21.1 中创造标签标题输入框的 EditBox Mixin 导致的启动崩溃。编辑器改用 NeoForge 原生文字阴影设置，保留无阴影标题编辑。
- 迁移至 KltytonTemple，共享编辑逻辑，并提供 Minecraft 1.20.1、1.21.1、26.1.2、26.2 的九个独立 Fabric、Forge、NeoForge 构建目标。
