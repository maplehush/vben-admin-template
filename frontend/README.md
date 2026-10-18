# Maple Hush Admin 前端

基于 Vben Admin 5 的单应用前端模板，使用 Vue 3、TypeScript、Vite、pnpm 和 `antdv-next`。后端为本仓库 `../backend/` 的 Spring Boot 单体服务。

## 启动

```bash
pnpm install
pnpm dev
```

开发页面默认运行在 `http://localhost:5999`。Vite 把 `/api` 请求代理到 `http://localhost:8080`，请先启动后端。本地数据库和 Redis 配置见 [后端运行说明](../backend/README.md)。

## 检查与构建

```bash
pnpm -F @vben/web-antdv-next run typecheck
pnpm build:antdv-next
```

构建结果位于 `apps/web-antdv-next/dist/`。`pnpm build` 会同时构建工作区内需要构建的共享包。

## 目录

- `apps/web-antdv-next/`：唯一运行的应用，包含登录、工作台和系统管理页面。
- `packages/`：应用依赖的 Vben 共享功能与 UI 基础包。
- `playground/`：保留的 Vben 页面和组件源码示例，当前不参与工作区安装或构建。
- `internal/`：Vite、Tailwind、TypeScript 和代码检查配置。
- `scripts/`：构建、代码检查与 Docker 部署脚本。独立部署使用本目录 `docker-compose.yml`；完整本地环境使用仓库根目录 `docker-compose.yml`，见[Docker 部署说明](../开发文档/21-Docker部署.md)。

前端精简依据、配置位置及二次开发约定见 [前端精简与配置](../开发文档/18-前端精简与配置.md)。Vben 原始项目与完整组件文档见 [官方文档](https://doc.vben.pro/)。
