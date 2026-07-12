# Visual Creative Tab Editor / 可视化创造标签页编辑器

中文名：**可视化创造标签页编辑器**。这是一个用于可视化、数据驱动编辑 Minecraft 创造标签页的 Fabric 与 Forge 多加载器 Mod。

A Fabric and Forge mod for visual, data-driven editing of Minecraft creative mode tabs.

## 当前状态 / Current status

当前版本已经实现：

- 原版与 Mod 创造标签页的数据驱动目录。
- 最低优先级自动默认包与最高优先级玩家差异包。
- 统一的标签/物品可视化编辑模式。
- 多选拖动、删除、修改、新建、跨标签移动和自动翻页。
- 标签标题与图标编辑。
- 按语义类别、本地化名称、ID 和 Mod ID 整理。
- 服务端权限校验、修订冲突检测和保存回滚。

The current version includes the data-driven catalog, world-local priority packs,
the unified visual editor, semantic sorting, server validation, conflict handling,
and loader-native Fabric/Forge pagination integration.

## 支持版本 / Supported versions

| 项目 / Component | 版本 / Version |
| --- | --- |
| Mod | `2.0.1-hotfix1` |
| Minecraft | `1.20.1` |
| Java | `17+` |
| Fabric | Loader `0.16.9+` + Fabric API `0.92.1+1.20.1` |
| Forge | `47.2.30+` |

多人游戏需要客户端和服务端同时安装。Fabric 环境需要 Fabric API。

Install the mod on both client and server for multiplayer. Fabric also requires Fabric API.

## 1.20.1 实现说明 / Implementation notes

- ItemStack JSON 使用固定的 `id`、`count` 和可选 `tag` 字段；`tag` 是
  Minecraft 1.20.1 SNBT 字符串。
- 公共网络模型使用 `FriendlyByteBuf`，Fabric 与 Forge 分别绑定自己的网络频道。
- Fabric 使用 `visual_creative_tab_editor.accesswidener`；Forge 使用包含
  SRG 成员名的 `META-INF/accesstransformer.cfg`。
- 共享 Gradle 约定由 `buildSrc/` 提供。

Item stacks use explicit `id`, `count`, and optional SNBT `tag` fields. Common
payloads use `FriendlyByteBuf`, while each loader owns channel registration and
thread dispatch.

## Wiki

完整安装、编辑器、数据包和开发文档：

Full installation, editor, data-pack, and development documentation:

[GitHub Wiki](https://github.com/kltyton/Visual-Creative-Tab-Editor/wiki)

Wiki 源文件同时保存在 [`wiki/`](wiki/Home.md)。

## 构建 / Build

```powershell
.\gradlew.bat :forge:runData
.\gradlew.bat build
.\gradlew.bat :fabric:runClient
.\gradlew.bat :forge:runClient
```

构建产物：

```text
fabric/build/libs/visual_creative_tab_editor-fabric-1.20.1-2.0.1-hotfix1.jar
forge/build/libs/visual_creative_tab_editor-forge-1.20.1-2.0.1-hotfix1.jar
```

Minecraft 1.20.1 分支使用 Java 17。项目的共享 Gradle 约定位于 `buildSrc/`。

## License

CC0-1.0
