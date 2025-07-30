# VelocityRememberServer

一个 [Velocity](https://github.com/PaperMC/Velocity) 插件，用于记住玩家上次登录的服务器

本项目基于 [https://github.com/TISUnion/VelocityRememberServer](https://github.com/TISUnion/VelocityRememberServer) 修改而来

## 功能特性

- 记住玩家上次连接的服务器，下次登录时自动连接到该服务器
- 如果玩家进入服务器被踢出将会清空记录，防止一直卡在该服务器中

## 配置文件

插件配置文件位于 `plugins/velocityrememberserver/config.yml`：

```yaml
# 是否启用插件
enabled: true
# 断开连接超时时间（秒），在此时间内断开连接会清除记录
disconnect_timeout_seconds: 30
```

玩家位置记录存储在 `plugins/velocityrememberserver/locations.yml`

## 兼容性

已在 Velocity `3.2.0` 版本上测试
