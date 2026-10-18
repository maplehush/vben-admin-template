# Docker 部署

根目录和 `frontend/`、`backend/` 各有一份 `docker-compose.yml`。在相应目录执行 `docker compose up -d --build` 即可使用该目录的配置；无需传 `-f` 或 `--env-file`。

| 执行目录 | 启动内容 | 外部依赖 |
| --- | --- | --- |
| 仓库根目录 | 前端、后端、MySQL、Redis | 无 |
| `frontend/` | 前端 Nginx | 已部署的后端 |
| `backend/` | 后端 Spring Boot | 已部署的 MySQL、Redis |

## 首次配置

每个目录的 Compose 默认读取**该目录**的 `.env`。在首次使用的目录复制 `.env.example` 为 `.env`，按环境填写值；`.env` 被 Git 忽略。根目录至少需要设置 `MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD` 和 `JWT_SECRET_BASE64`。后者可由 `openssl rand -base64 32` 生成。后端独立部署须填写数据库、Redis、JWT 和浏览器 Origin；前端独立部署须按实际地址设置 `API_UPSTREAM`。

根目录的示例将前端绑定在 `127.0.0.1:8081`，浏览器访问 `http://localhost:8081`。更改地址时同步调整 `APP_AUTH_ALLOWED_ORIGIN`。前端生产包调用同源 `/api`；Nginx 将请求转发至 `API_UPSTREAM`，该值必须是**前端容器内可访问**且不带末尾斜杠的后端地址。

## 根目录同时启动

在仓库根目录执行：

```bash
docker compose up -d --build
```

此命令启动前端、后端、MySQL 和 Redis。后端等待数据库和 Redis 就绪，执行 Flyway 迁移，并在 `sys_user` 为空时自动创建 `admin` 管理员，初始密码为 `12345678`，首次登录须修改。有用户时跳过初始化，不覆盖账号或密码；因此无需再运行一次性初始化命令。管理员用户名可通过 `.env` 的 `ADMIN_USERNAME` 修改。

仅启动根目录 Compose 中的后端（包括其 MySQL、Redis 依赖），或仅启动前端：

```bash
docker compose up -d --build backend
docker compose up -d --build frontend
```

根目录前端默认转发到 `http://backend:8080`。若只启动前端，需将根目录 `.env` 中的 `API_UPSTREAM` 改为能从前端容器访问的已部署后端地址。停止时在根目录执行 `docker compose down`；MySQL 数据保留在命名卷中。

## 子目录单独启动

前端使用已部署后端，先在 `frontend/.env` 配置 `API_UPSTREAM`，然后执行：

```bash
cd frontend
docker compose up -d --build
```

后端使用已部署的 MySQL 和 Redis，先在 `backend/.env` 配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`SPRING_DATA_REDIS_HOST`、`JWT_SECRET_BASE64` 和 `APP_AUTH_ALLOWED_ORIGIN`，然后执行：

```bash
cd backend
docker compose up -d --build
```

独立后端同样会在空数据库中自动创建管理员；如需关闭，可设置 `APP_BOOTSTRAP_ADMIN_AUTO_ENABLED=false`。已有用户不会被修改。独立前端和后端互不启动。后端默认仅绑定 `127.0.0.1:8080`；跨主机访问时需按实际网络调整 `BACKEND_BIND_HOST`。公网 HTTPS 环境应保持 `APP_AUTH_COOKIE_SECURE=true`，根目录本地 HTTP 环境设为 `false`。

## 生产环境

根目录配置用于本地集成运行。生产环境还需配置数据库备份、TLS、密钥注入和 Flyway 迁移账号。OSS AccessKey、数据库密码等仅写入运行环境的 `.env` 或密钥系统，不要放入 Dockerfile、前端 `VITE_` 变量或仓库。
