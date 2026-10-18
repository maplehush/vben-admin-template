# Maple Hush Admin

基于 Vben Admin 5 和 Spring Boot 的后台管理模板。前端和后端位于同一个项目仓库，分别构建；后端是只部署一个 JAR 的模块化单体。

## 目录

| 目录 | 内容 |
| --- | --- |
| [`frontend/`](frontend/README.md) | Vben 前端工作区，当前应用为 `apps/web-antdv-next` |
| [`backend/`](backend/README.md) | Java 后端、Flyway 迁移与运行说明 |
| [`开发文档/`](开发文档/README.md) | 架构、接口约定、数据库设计与设计素材 |
| `开发文档/design/` | 品牌图片、SVG 素材与生成脚本 |
| `backend/docs/superpowers/` | 历史设计与实施计划 |

前端开发和构建命令见[前端说明](frontend/README.md)，后端启动、测试和本地配置见[后端说明](backend/README.md)。整体结构与模块边界见[项目结构](开发文档/02-项目结构.md)。

## Docker 部署命令

需要 Docker 和 Docker Compose V2。首次使用时，在准备执行命令的目录复制对应的 `.env.example` 为 `.env` 并填写配置。此工作区的三个 `.env` 已在本地生成，均被 Git 忽略；换机器后需要重新配置。

```bash
cd /Volumes/Macdisk/playground/vben-admin-template
test -f .env || cp .env.example .env
# 编辑 .env，设置数据库密码和 JWT 密钥
```

同时启动前端、后端、MySQL 和 Redis（本地集成环境）：

```bash
docker compose up -d --build
```

只启动根目录 Compose 中的后端及其 MySQL、Redis，或只启动前端：

```bash
docker compose up -d --build backend
docker compose up -d --build frontend
```

也可以进入子目录，使用各自的 Compose 文件单独部署。独立前端的 `API_UPSTREAM` 应指向容器可访问的后端；独立后端需要已有 MySQL 和 Redis：

```bash
cd frontend
test -f .env || cp .env.example .env # 首次配置时执行
docker compose up -d --build

cd ../backend
test -f .env || cp .env.example .env # 首次配置时执行
docker compose up -d --build
```

根目录启动后，空数据库会自动创建 `admin` 管理员（初始密码 `12345678`，首次登录须修改）；已有用户时跳过，不重置密码。停止时在相同目录执行 `docker compose down`。完整环境默认访问 `http://localhost:8081`。配置细节见[Docker 部署说明](开发文档/21-Docker部署.md)。

## 版本管理

项目根目录是唯一的 Git 仓库。`frontend/` 与 `backend/` 不再各自维护 Git 元数据；本地依赖、构建产物和密钥文件由根目录及各子目录的 `.gitignore` 排除。上游 Vben 仓库历史和原始源码压缩包已在项目外保留备份；该备份不参与项目版本管理。
