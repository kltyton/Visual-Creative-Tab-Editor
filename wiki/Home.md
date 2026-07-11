# One Enough Creative Tab

One Enough Creative Tab 是一个面向 Minecraft 26.2 的创造标签页编辑 Mod，同时支持 Fabric 与 NeoForge。

它把原版及已注册 Mod 的创造标签页投影为可重载的数据目录，并提供类似手机桌面的可视化编辑器。标签页的标题、图标、顺序、可见性和物品列表都可以由世界数据包控制。

## 当前状态

当前版本已经实现完整的服务器权威数据流和可视化编辑流程，不再是项目骨架。

| 项目 | 当前支持 |
| --- | --- |
| Mod 版本 | `26.2.0.0` |
| Minecraft | `26.2` |
| Fabric | Loader `0.19.3+`，需要 Fabric API；当前使用 `0.152.1+26.2` 构建和验证 |
| NeoForge | `26.2.0.1-beta+` |
| Java | `25+` |
| 安装位置 | 单人游戏安装在客户端；多人游戏需要客户端和服务端同时安装 |
| 配置文件 | 当前没有独立配置文件 |

## 已实现功能

- 原版和已注册 Mod 创造标签页的数据驱动投影。
- 每个世界自动生成最低优先级的原始标签数据包。
- 普通数据包可以逐字段覆盖标题、图标、内容、顺序和布局。
- 可视化编辑结果保存为世界级、最高优先级的差异数据包。
- 标签与物品共用同一个编辑会话，支持复选框、多选、拖动让位和批量删除。
- 支持修改标签标题和图标、替换物品、新建标签和新增物品。
- 拖动物品悬停其他分类标签后自动移动并打开目标标签。
- 拖动到左右边缘后自动翻页，原生翻页按钮仍然可用。
- 按语义类别、ID、本地化名称或 Mod ID 整理物品。
- 服务端权限复核、修订冲突检测、上传限制和失败回滚。

## 快速导航

- [安装与快速开始](https://github.com/kltyton/OneEnoughCreativeTab/wiki/安装与快速开始)
- [可视化编辑器](https://github.com/kltyton/OneEnoughCreativeTab/wiki/可视化编辑器)
- [整理与排序](https://github.com/kltyton/OneEnoughCreativeTab/wiki/整理与排序)
- [数据包格式](https://github.com/kltyton/OneEnoughCreativeTab/wiki/数据包格式)
- [数据存储与优先级](https://github.com/kltyton/OneEnoughCreativeTab/wiki/数据存储与优先级)
- [故障排查](https://github.com/kltyton/OneEnoughCreativeTab/wiki/故障排查)
- [架构设计](https://github.com/kltyton/OneEnoughCreativeTab/wiki/架构设计)
- [开发与构建](https://github.com/kltyton/OneEnoughCreativeTab/wiki/开发与构建)

## 兼容性边界

项目会保留已注册原版和 Mod 标签对象的身份，因此依赖这些对象的常规扩展可以继续工作。数据包中新建的纯 JSON 分类标签是注册表之外的运行时对象；直接枚举内建注册表、强依赖固定标签坐标或修改相同界面方法的 Mod 仍可能需要单独适配。

“玩家覆盖”是整个世界的设置，不是每位玩家各自保存的一份本地布局。
