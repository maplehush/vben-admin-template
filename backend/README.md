# Maple Hush Admin 后端

这是 Vben Admin 5 管理系统模板的模块化单体后端。Maven 根项目聚合 `admin-base-core` 与 `admin-module-system`；后者聚合预留的 `admin-system-api`、`admin-system-biz` 和唯一启动模块 `admin-system-start`。目前已完成工程装配、Flyway 迁移、首次管理员初始化、认证闭环，以及用户、角色、部门、菜单、独立接口权限、字典、在线会话和审计日志查询接口。字典类型与字典项管理列表由后端筛选、分页，返回 `PageResult`；字典选项接口仍返回完整的有效选项。字典项支持可空的十六进制展示颜色，并兼容原有语义色；选项接口一并返回 `itemColor`。

## 目录与持久化

```text
backend/
├── pom.xml                         # 根聚合与版本
├── schema/V*__*.sql               # Flyway 唯一迁移源
├── admin-base-core/               # common/api/vo 公共响应、分页、时间工具
└── admin-module-system/           # 系统模块聚合
    ├── admin-system-api/          # 预留的跨模块契约（当前为空）
    ├── admin-system-biz/          # 系统业务实现
    │   ├── src/main/java/com/maplehush/admin/modules/system/
    │   │   ├── auth/ user/ role/ dept/
    │   │   └── menu/ permission/ dict/ audit/ file/ common/
    │   └── src/main/resources/mapper/system/*.xml
    └── admin-system-start/        # 唯一启动类、配置与可执行 JAR
```

业务包按需要使用 `controller/service/impl/mapper/entity/dto/vo/config/constant`，其中 `service/IXxxService.java` 定义业务接口，`service/impl/XxxServiceImpl.java` 提供实现，Controller 依赖接口。查询入参使用 DTO，响应使用 VO，分页统一为 `PageResult<VO>`；实体不直接作为 HTTP 响应。单表 CRUD 使用 MyBatis-Plus `BaseMapper`；联表、树、权限范围及原子更新使用 Mapper XML。实体字段逐项注释，简单访问方法由 Lombok 生成。Service 负责校验、权限、事务和业务规则，不直接写 SQL。完整目录与依赖方向见[项目结构](../开发文档/07-详细项目目录结构.md)，命名与格式化规则见[后端代码规范](../开发文档/20-后端代码规范.md)。

业务异常使用 `ApiException` 与 `ApiErrorCode` 统一维护 HTTP 状态、业务码和提示文案，失败响应继续使用 `code/data/message`。用户、角色、部门、菜单、接口权限、字典及字典项写入，以及本人改密和会话撤销，通过 audit 包的 `IOperationAuditService` 记录操作日志，与业务更新共用事务；审计 SQL 集中在 `OperationAuditMapper.xml`。

新增、更新和删除接口统一以 `data: true` 表示成功；新增接口不再返回 ID 包装对象。资源 ID 在列表或详情查询中以十进制字符串返回。

## 官方工程基线

当前 `backend/` 先由 [Spring Initializr](https://start.spring.io/) 生成空白 Maven 工程，再迁入原手工工程的模块、代码和配置；迁移核对完成后，临时保留的 `backend-bak/` 已删除。生成参数为 Spring Boot 4.1.1、Java 21、Maven、`com.maplehush`、`admin-system-start`，依赖选项为 Spring Web、Security、OAuth2 Resource Server、JDBC、Flyway、MySQL Driver、Spring Data Redis、Validation、Actuator。当前启动类 `AdminSystemStartApplication`、Maven Wrapper、`.gitattributes` 和基础 `.gitignore` 来自生成工程；旧启动类 `AdminApplication` 与它功能相同，未重复迁入。

Spring Initializr 生成的是单个应用模块；本工程按 [Spring 官方多模块指南](https://spring.io/guides/gs/multi-module/)将它改为 `admin-system-start` 子模块，建立根聚合 POM，并迁入系统业务模块和公共模块。官方生成的 Starter 按职责分配到启动模块与系统模块；MyBatis-Plus 和 springdoc 是模板需要的附加依赖。只在 `admin-system-start` 使用 Spring Boot 打包插件，库模块产出普通 JAR。官方生成的空 `contextLoads` 测试需要外部数据库和 Redis，因此没有保留；对应的集成测试应在业务实现批次使用隔离环境编写。

## 本地准备

- Java 21、MySQL 8.4、Redis、Maven Wrapper。
- 创建空库 `maple_hush_admin`，字符集 `utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。不手工导入表结构或种子数据。
- `local` Profile 的数据库和 Redis 地址在 `application-local.yml`。首次在本机运行时，把 `.env.local.example` 复制为 Git 忽略的 `.env.local`，填写 `DB_PASSWORD`、`SPRING_DATA_REDIS_PASSWORD` 和 `JWT_SECRET_BASE64`；在仓库根目录运行时也会读取 `backend/.env.local`。
- `dev` 留给共享开发环境，仅开启接口文档；数据库地址、账号和密码由 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量提供，Redis 密码由 `SPRING_DATA_REDIS_PASSWORD` 提供。数据库 URL 建议带 `serverTimezone=Asia/Shanghai`；其他部署环境同样使用各自的连接配置。

业务 `DATETIME` 统一按北京时间和秒精度读写，接口时间格式为 `yyyy-MM-dd HH:mm:ss`。连接会话和 Flyway 会话设为 `+08:00`，Jackson 设为 `GMT+8`；启动模块的 `JacksonConfiguration` 为 `LocalDateTime` 等 Java 时间类型注册全局 JSON 格式，只有特殊字段使用 `@JsonFormat`。启动类提供统一 `Clock` Bean，业务服务通过注入使用，测试可替换为固定时钟。V11 将旧的 `DATETIME(3)` 改为秒精度并向下截断历史毫秒。已有按 UTC 存储的库须先停服务、备份，再一次性执行 [历史数据转换脚本](maintenance/2026-10-03-convert-utc-datetime-to-beijing.sql)；新建库和已转换的库不能执行该脚本。

## 构建与运行

```bash
cd backend
./mvnw clean package
java -jar admin-module-system/admin-system-start/target/admin-system-start-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Java 代码采用根 `.editorconfig` 和 Maven Spotless 统一格式。提交本地修改前先运行 `./mvnw spotless:apply`，再运行 `./mvnw clean test`；`test` 阶段会执行格式检查。

涉及 Flyway、MySQL 方言、事务或约束时，在可访问测试 MySQL 和 Redis 的环境运行 `TEST_DB_PASSWORD=... ./verify-mysql.sh`。可用 `TEST_DB_USERNAME`、`TEST_MYSQL_HOST`、`TEST_MYSQL_PORT` 指定测试 MySQL；脚本先运行完整单元测试，再显式执行 `PermissionBoundaryMySqlIT`。集成测试创建随机数据库并在结束时删除，所用 MySQL 账号须有创建和删除数据库的权限。不要指向开发库或共享数据库服务。

在 IDEA 的启动配置中将 **Active profiles** 设为 `local`；若没有该选项，在 **Program arguments** 中填 `--spring.profiles.active=local`。不设置 Profile 时不会自动使用本机账号。

## 首次管理员初始化

空库完成 Flyway 迁移后，在终端运行一次：

```bash
java -jar admin-module-system/admin-system-start/target/admin-system-start-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --spring.main.web-application-type=none \
  --app.bootstrap-admin.enabled=true \
  --app.bootstrap-admin.username=admin
```

初始密码固定为 `12345678`，服务端仅存密码哈希，首次登录必须修改密码。用户名要求 3–64 位，只能使用字母、数字、下划线、点和连字符。此手动命令仅适用于 `sys_user` 为空的数据库；重复执行会失败，不会重置已有账号。Docker Compose 启动时另有自动初始化流程：只在空数据库创建账号并绑定预置的 `super_admin` 角色，已有用户时跳过。两种流程都设置 `must_change_password=1`。手动命令不启动 Web 服务；普通 Web 启动不要设置 `app.bootstrap-admin.enabled`。

启动模块从 `backend/schema/` **复制**版本脚本到 JAR 的 `db/migration/`；该目录是唯一 SQL 维护源。Flyway 默认在本地启动时执行尚未执行的迁移。`GET /actuator/health` 需要 MySQL 和 Redis 均可用才返回健康状态。开发环境的 OpenAPI 地址是 `/v3/api-docs`；默认环境关闭该端点。Apifox 继续使用[唯一的设计契约文件](../开发文档/openapi/核心接口.openapi.json)，不必为每次实现生成新副本。

## OSS 图片上传

后端通过 `POST /upload` 接收 multipart 的 `file` 字段。需要登录并完成首次改密；仅接受 PNG、JPEG、GIF、WebP 图片，最大 5 MB。对象路径为 `vben-admin-img/{用户ID}/{UTC日期}/{随机UUID}.{扩展名}`，响应包含 `key` 和 `url`。前端头像地址上传按钮和富文本图片上传复用该接口。URL 可直接访问的前提是 Bucket 为公共读，或配置了适合图片访问的 CDN/访问策略；上传接口不会修改 Bucket 或对象 ACL。

启动后端前设置 `OSS_ENABLED=true`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`。本机 `local` Profile 也会读取后端根目录下被 Git 忽略的 `.env.oss.local` 属性文件（`app.oss.enabled`、`app.oss.access-key-id`、`app.oss.access-key-secret`）；该文件不得提交。`application.yml` 已配置北京地域、`maple-hush` Bucket、域名和 `vben-admin-img` 前缀；如部署到其它环境，可通过 `OSS_REGION`、`OSS_ENDPOINT`、`OSS_BUCKET`、`OSS_DOMAIN`、`OSS_PREFIX` 覆盖。AccessKey 仅供后端读取，不配置在前端 `VITE_` 变量或源码中。未启用 OSS 时，上传接口返回 503，其它功能仍可启动。

## 当前认证接口

后端直连路径为 `GET /auth/captcha`、`POST /auth/login`、`POST /auth/refresh`、`POST /auth/logout`。浏览器经同源代理访问 `/api/auth/*`，代理去掉 `/api` 前缀。登录请求包含 `username`、`password`、`captchaId`、`captchaCode`；验证码答案仅在 Redis 中保存 120 秒且只可使用一次。登录成功的 JSON 只返回 Access Token，Refresh Token 通过 `HttpOnly` Cookie 传递。刷新成功返回纯文本 Access Token，并轮换 Cookie；刷新和退出请求必须带配置允许的 `Origin`。退出会撤销会话，重复退出仍成功。

`local` Profile 的允许来源为 `http://localhost:5999`、`http://127.0.0.1:5999`，以及后端直连调试使用的 `http://localhost:18080`、`http://127.0.0.1:18080`。本地 Cookie 关闭 `Secure` 以支持 HTTP；非本地配置默认要求 `Secure`，并需提供 `JWT_SECRET_BASE64`、`APP_AUTH_ALLOWED_ORIGIN`。Access Token 有效期 15 分钟，Refresh Token 有效期 7 天。会话与登录日志存 MySQL，验证码和限流计数存 Redis。

受保护接口为 `GET /user/info`、`GET /auth/codes`、`GET /menu/all` 和 `PUT /user/password`。每次请求验签 Access Token，核对 MySQL 中的会话有效期、撤销状态和用户状态；匿名或失效令牌返回 `40100`。初始化管理员的 `must_change_password=1` 会由后端阻止权限码、菜单及其他业务请求，只允许读取本人信息和修改本人密码。改密需提交原密码和 8–128 位新密码；成功后清除首次改密标记并撤销该用户全部会话，随后重新登录。尚未登记到安全规则的业务路径默认拒绝。

菜单接口仅返回当前前端已经打包的工作台 `/dashboard/workspace/index`、用户 `/system/user/list`、角色 `/system/role/list`、部门 `/system/dept/list`、菜单 `/system/menu/list`、接口权限 `/system/permission/list`、字典 `/system/dict/list`、在线会话 `/system/session/list`、登录日志 `/system/log/login/list`、操作日志 `/system/log/operation/list` 页面及必要父目录。权限码按当前有效角色的按钮与独立接口权限授权查询；`super_admin` 在应用层获得全部有效权限码。管理 API 在服务端执行权限校验，用户管理还执行数据范围校验，不能只依赖按钮隐藏。

在线会话接口为 `GET /auth/sessions`、`DELETE /auth/sessions/{sessionId}`、`GET /system/sessions` 和 `DELETE /system/sessions/{sessionId}`。仅返回未撤销且未过期的会话，不返回令牌哈希；管理员查询和强制下线分别需要 `system:session:list`、`system:session:revoke`。非超级管理员不能下线超级管理员。Flyway V3 增加菜单与权限种子数据，前端提供在线会话管理页。

`GET /system/login-logs` 和 `GET /system/operation-logs` 分别要求 `system:login-log:list`、`system:operation-log:list`。两者支持分页、账号、结果或操作类型、时间范围筛选，并按授权数据范围限制记录；未知账号的失败登录仅对 `ALL` 数据范围可见。V9 提供菜单与独立接口权限种子；V10 增加“系统管理 → 日志管理”目录并将两类日志作为其子菜单，保留原权限码。`GET /user/info` 返回本人邮箱和手机号；`PUT /user/profile` 保存本人资料。个人中心可查询并撤销本人会话，响应中的 `current` 标记当前设备。

生产部署应由受控步骤使用迁移凭据执行 Flyway，再使用不具备 DDL 权限的应用账号启动；这套独立部署流程尚未实现，不能直接把本地自动迁移方式当作生产部署方案。

Docker 镜像由本目录 `Dockerfile` 构建。单独连接已有 MySQL 与 Redis 时使用本目录 `docker-compose.yml`；同时启动前后端及本地依赖时使用仓库根目录 `docker-compose.yml`。环境变量、首次管理员初始化和限制见[Docker 部署说明](../开发文档/21-Docker部署.md)。

## 当前验证

2026-10-15 在独立 MySQL 8.4 与 Redis 测试实例上执行 `./verify-mysql.sh`，包含完整后端单元测试及 `PermissionBoundaryMySqlIT`；迁移 V9→V14、权限边界和字典审计断言通过。本人改密与会话撤销的审计和回滚测试已补齐；最新的完整回归结果应以当次构建输出为准。前端类型检查、ESLint、单元测试与生产构建需在前端目录单独执行。

设计说明与后续接口契约见[开发文档](../开发文档/README.md)。
