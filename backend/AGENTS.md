# 后端开发约定

适用于 `backend/`。本项目是 Java 21、Spring Boot 4、MyBatis-Plus 的模块化单体。使用 `./mvnw` 构建；开发阶段不创建 Git 提交。

## 工程边界

- 根 `pom.xml` 聚合 `admin-base-core` 和 `admin-module-system`。后者聚合 `admin-system-api`、`admin-system-biz` 与唯一可执行模块 `admin-system-start`。API 模块目前按项目约定保留为空模块，待真实跨模块契约出现时再加入接口；没有实际业务代码时不建 Demo 模块。依赖方向为 `start → biz → api → core`。
- `admin-base-core` 放跨模块公共契约和工具；公共响应模型 `ApiResult`、`PageResult` 位于 `common/api/vo`。只有真实跨模块复用的传输模型才加入 `common/api/dto`。系统业务代码位于 `admin-system-biz/src/main/java/com/maplehush/admin/modules/system/`，先按 `auth/user/role/dept/menu/permission/dict/audit/file` 划分，再按真实职责使用 `controller/service/impl/mapper/entity/dto/vo/config/constant`。请求和查询条件放功能包下的 DTO，HTTP 响应和查询结果放 VO；分页统一使用 core 的 `PageResult<VO>`。不为没有实际用途的类型创建空包。
- 系统资源的实体、Mapper 和主业务服务使用同一 `SysXxx` 前缀，如 `SysRole`、`SysRoleMapper`、`ISysRoleService`、`SysRoleServiceImpl`。独立查询服务使用 `ISysRoleQueryService`、`SysRoleQueryServiceImpl` 等名称。认证、授权检查、审计等能力型组件按职责命名。Controller 和跨功能服务依赖接口，Spring 只在实现类上注册服务；接口只暴露业务操作与必要的数据类型，不承载持久化细节。业务服务不继承暴露通用 CRUD 的 MyBatis-Plus `IService`，以免绕过权限、事务和状态约束。
- Controller 按资源命名（如 `SysUserController`、`SysDeptController`），同一资源的查询与管理接口放在一个 Controller；不使用 `ManagementController`、`QueryController` 等页面职责后缀。Controller 处理 HTTP 参数与响应，Service 处理权限、校验、事务和业务规则，Mapper 处理持久化。不从 Controller 访问其他功能的 Mapper。
- 常量包名使用 `constant`。只收纳稳定且确有复用价值的值；业务状态和权限码不可随意散落成魔法字符串。类及关键规则使用准确、简洁的中文注释，尤其说明并发与安全约束。字段注释作为简短说明时不加句末句号。
- API 异常使用系统模块 `common/exception` 下的 `ApiException` 和 `ApiErrorCode`，统一维护 HTTP 状态、业务错误码和对外文案；全局异常处理只负责映射为 `ApiResult`。新增错误先定义错误码，避免在业务代码和 Controller 中重复拼响应。

## 数据与安全

- 简单单表查询、计数和排序优先使用 MyBatis-Plus `BaseMapper` 与 `LambdaQueryWrapper`；联表、复杂筛选、需要行锁或原子条件更新的 SQL 放入 `admin-system-biz/src/main/resources/mapper/system/*.xml`。分页查询只有在配置并验证分页拦截器后才能改用 `selectPage`，不得以 `last` 拼接前端分页参数。SQL 参数必须绑定，不能由前端值拼接表名、列名或条件。Service 不注入 `JdbcTemplate`，不处理 `ResultSet`，不嵌入 SQL。
- Mapper 实体对应真实表结构；单表查询（包括树和加锁查询）优先映射实体，稳定的联表列表可直接映射响应 VO，内部鉴权等计算结果确有需要时使用独立类型。实体不要直接作为 HTTP 响应。修改角色授权、账号状态、会话和删除约束时，保持事务、行锁与受影响行数判断。
- 持久化实体的每个字段都写简洁、准确的中文注释；状态字段写明数值含义，敏感字段写明存储方式。普通访问方法用 Lombok `@Getter`、`@Setter` 生成，不手写重复的 getter/setter；避免对数据库实体使用会额外生成 `equals/hashCode` 的 `@Data`。
- 系统表实体统一命名为 `SysUser`、`SysRole`、`SysDept` 等 `SysXxx`；关联实体如有需要也按 `SysUserRole` 命名。使用 `@TableName` 和 `@TableId` 明确表与主键策略，接口展示字段放在独立响应对象。实体字段注释说明业务含义，不加句末句号。
- 通用启禁用状态使用 `admin-base-core` 的 `CommonStatus`：数据库 1/0，接口 `ENABLED`/`DISABLED`。`mustChangePassword` 等布尔标记不使用该枚举。业务可配置选项用 `sys_dict`、`sys_dict_item`；内置 `common_status` 字典不可修改或删除，禁用的类型/字典项不进入选项接口。
- MySQL 是权威数据源。Flyway 唯一迁移源是 `backend/schema/V*__*.sql`；已发布迁移不改写，只添加新版本。测试使用隔离库，不能向开发库写入测试数据。
- 数据库 `DATETIME` 按北京时间、秒精度存储；接口时间输出为 `yyyy-MM-dd HH:mm:ss`。需要当前时刻的 Service 注入启动类提供的 `Clock`，用 `admin-base-core` 的 `DateUtils` 做时区换算；响应 VO 使用 `LocalDateTime`，默认由启动模块 `JacksonConfiguration` 的 Java 时间模块统一序列化，仅特殊格式使用字段级 `@JsonFormat`。测试可注入固定时钟。JWT 的 `Instant` 仍表示绝对时间。
- 每个管理接口都在后端校验权限与数据范围。`super_admin` 仅用于初始化系统账号，不通过普通角色分配给员工；管理员不得禁用自己；普通员工默认不分配系统管理菜单与相关权限，若需要强制隔离，必须同时在后端授权写入和读取处校验。前端禁用按钮只是交互提示，不能代替服务端校验。
- 部门编号由服务端按 `app.dept.code-prefix` 和数据库 ID 生成（默认 `DEPT_0001`），不回收、不允许客户端指定。仅禁用且无子部门、用户归属或角色数据范围引用的部门可删除。
- OSS AccessKey 只由后端环境变量或本地忽略文件提供，不写入源码、日志、前端 `VITE_` 环境变量或文档。
- 管理操作在业务事务内显式调用 `IOperationAuditService.record`；审计写入与当前业务写入共同提交或回滚。目标类型由 `AuditTargetType` 定义，审计 SQL 只保留在 audit 包的 Mapper XML，不在各资源 Mapper 中重复定义。登录日志与查询范围按原有职责处理。

## 验证与文档

- 修改后运行 `./mvnw clean test`。变更 MySQL 方言、Flyway、事务或约束时，在独立 MySQL 和 Redis 环境执行 `TEST_DB_PASSWORD=... ./verify-mysql.sh`；脚本会运行完整单元测试与 `PermissionBoundaryMySqlIT`。H2 测试通过不等于 MySQL 行为已验证。
- 根 `.editorconfig` 约束编码、缩进与换行；Maven `spotless:check` 在 `mvn test` 时检查 Java 格式和 import。修改 Java 后运行 `./mvnw spotless:apply`，再运行构建；IDE 使用 EditorConfig，避免个人格式化配置覆盖项目规则。
- 构建产物位于 `admin-module-system/admin-system-start/target/`。更新模块、迁移或公开接口时，同步更新 `README.md`、`../开发文档/` 及 OpenAPI 契约。
