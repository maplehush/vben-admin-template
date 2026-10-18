# 后端结构与持久化重设计

日期：2026-10-03

## 目标与边界

本项目继续采用 Java 21、Spring Boot 4 的模块化单体，一个启动入口和一个可执行 JAR。参考 JeecgBoot 的模块边界和常规分层，但不引入代码生成、微服务或空泛的 DDD 抽象。现有 HTTP 契约、权限规则、数据库结构及已发布 Flyway 迁移保持兼容。用户已明确开发阶段不提交 Git。

本设计同时解决三个问题：包和模块命名不统一；虽然引入 MyBatis-Plus，Service 仍直接执行大量 SQL；`AppTime` 混合了业务时钟、参数解析、JDBC 读取和输出格式化。仅移动目录不能解决后两个问题。

## 核对依据

- [JeecgBoot 官方目录说明](https://help.jeecg.com/java/projectDirectoryStructure/)采用 `base-core`、`module-system/{system-api,system-biz,system-start}`。[SysUserMapper](https://github.com/jeecgboot/JeecgBoot/blob/main/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/mapper/SysUserMapper.java) 继承 `BaseMapper<SysUser>`，[SysUserServiceImpl](https://github.com/jeecgboot/JeecgBoot/blob/main/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/service/impl/SysUserServiceImpl.java) 继承 `ServiceImpl`，复杂查询仍有自定义 Mapper 方法。
- JeecgBoot 的 `base-core` 源码分为 `org.jeecg.common`、`org.jeecg.config`、`org.jeecg.modules`；[airag/app](https://github.com/jeecgboot/JeecgBoot/tree/main/jeecg-boot/jeecg-boot-module/jeecg-boot-module-airag/src/main/java/org/jeecg/modules/airag/app) 则按 `consts / controller / entity / enums / mapper / service / vo` 分包。用户提供的 `AiAppConsts`、`AiragChatController` 和 `ShopController` 也体现了这种“先模块，再业务功能，再具体职责”的定位方式。
- JeecgBoot 的 [application-dev.yml](https://github.com/jeecgboot/JeecgBoot/blob/main/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml) 配置 Jackson 日期格式和 `GMT+8` 时区；部分[实体字段](https://github.com/jeecgboot/JeecgBoot/blob/main/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/entity/SysUser.java)还有 `@JsonFormat` 和 `@DateTimeFormat`。本项目采用相同的北京时间口径，但不能只靠 YAML 统一全部 Java Time 类型。
- 当前 `admin-module-system/pom.xml` 已依赖 MyBatis-Plus Spring Boot 4 starter 和 JSQLParser，但系统模块有 **17 个 Java 文件**引用 `JdbcTemplate` 或 `NamedParameterJdbcTemplate`。现有 `SessionManagementService` 将联表分页、条件更新、权限保护和 `ResultSet` 映射放在一个 Service。
- 现有 `admin-base-core/.../time/AppTime.java` 负责 `now`、`after`、ISO 解析和 `ResultSet` 时间格式化，职责混杂。现有 `application.yml` 把 MySQL 会话时区设为 UTC，并设置 `spring.jackson.date-format`；这并不能保证所有 `LocalDateTime` 输出格式一致。

## 方案选择

| 方案 | 评价 |
| --- | --- |
| 只调整 Maven 目录 | SQL 与时间设计问题仍在，不采用。 |
| JeecgBoot 式模块边界 + 按功能和常规层次分包 + 分批迁到 MyBatis-Plus | 与现有技术栈相符，迁移可回归，**采用**。 |
| 完整 DDD 或强制消灭所有手写 SQL | 增加不必要的抽象，复杂联表查询仍需明确 SQL，不采用。 |

## 模块与包命名

```text
backend/
├── pom.xml                              # 父 POM：聚合和版本
├── AGENTS.md  README.md
├── schema/V*__*.sql                     # Flyway 唯一迁移源
├── admin-base-core/                     # 跨模块公共契约和配置
│   └── src/main/java/com/maplehush/admin/
│       ├── common/
│       │   ├── response/                # ApiResponse、统一异常响应
│       │   ├── page/                    # PageResult
│       │   └── constant/                # 真正跨模块使用的常量
│       └── config/                      # Jackson 时间格式等基础配置
├── admin-module-system/                 # packaging=pom，仅聚合
│   ├── admin-system-api/                # 确有调用方的跨模块接口和 DTO
│   ├── admin-system-biz/                # 认证与系统管理实现
│   │   ├── src/main/java/com/maplehush/admin/modules/system/
│   │   │   ├── auth/
│   │   │   ├── user/
│   │   │   ├── role/
│   │   │   ├── dept/
│   │   │   ├── menu/
│   │   │   ├── permission/
│   │   │   ├── audit/
│   │   │   └── file/
│   │   └── src/main/resources/mapper/system/  # 复杂查询需要时使用
│   └── admin-system-start/              # 唯一 main、运行配置、Flyway 装配
└── admin-module-demo/                   # 现有示例，实施时核实是否仍需启动
```

`biz` 是 `business` 的缩写，沿用 JeecgBoot 的系统业务实现模块命名，并非 DDD 专有术语。它只区分 Maven 模块职责，不决定 Java 包内的分层。跨模块 API 没有真实调用方时不创建空接口或空包。JeecgBoot 另有一个容纳 airag、demo 的 `jeecg-boot-module` 聚合目录；本项目目前没有第二个实际业务模块，因此不先建一个空聚合层。以后出现真实业务模块时，再按 `admin-module-业务名` 增加，与 system 模块同级。

取消上版 `core/web,pagination,time` 与 `auth/web,application,infrastructure` 混用。基础模块按 `common` 与 `config` 分类；只有确有基础业务实现时才增加 `modules/base`，不为了模仿目录而造空包。系统模块使用 `modules/system/业务功能/职责`，以 `auth` 为例：

```text
modules/system/auth/
├── controller/       # HTTP 输入、输出和参数转换
├── service/          # 登录、会话等业务规则和事务
├── mapper/           # MyBatis-Plus Mapper、自定义查询
├── entity/           # 持久化实体
├── dto/              # 请求参数、服务间传输对象
├── vo/               # 面向前端的响应和列表投影
├── constant/         # 本功能常量；有限状态优先使用枚举
└── config/           # 本功能专属安全配置与过滤器
```

`user`、`role`、`dept`、`menu`、`permission`、`audit`、`file` 遵循同样约定，但只创建有真实文件的子包。后续按开发约定统一采用 `service/IXxxService` 与 `service/impl/XxxServiceImpl`；数据库实体不直接作为 HTTP 响应。依赖保持单向：`biz → core`，`start → biz`；`core` 不包含系统业务或 Mapper。

### 注释与代码风格

参考用户提供的 JeecgBoot 示例，业务类、公开方法和有特殊含义的常量写简洁的中文 Javadoc：说明职责、参数约束、返回结果及规则的缘由。对权限保护、状态流转、时间单位、并发条件等容易误用的地方，写明为什么这样处理。团队若需记录作者和创建日期，可在类文档保留 `@author`、`@since`；这些信息不代替业务说明，也不要求每个简单方法重复填写。普通赋值和一眼可读的代码不逐行翻译；修改规则时同步修改注释，避免过时说明。

`constant` 只放不可变、确有复用价值的键名、前缀或限制值；类似启用/禁用的有限业务状态优先用枚举，避免把各种不相干的字符串塞进一个大常量类。示例 Controller 中直接保存模拟商品数据、处理购买规则，适用于 Demo；本项目正式业务的 Controller 只做 HTTP 适配，规则仍在 Service。继续使用构造器注入，避免为贴近示例而引入字段注入。

## 持久化规则

1. 为用户、角色、部门、菜单、权限、会话、日志及关系表建立与实际 schema 对应的实体和 `BaseMapper<T>`。主键策略、字段映射、自动填充只按真实表定义，不能为了框架修改历史库。
2. 单表 CRUD、条件列表及普通分页使用 `BaseMapper`、`LambdaQueryWrapper`、`LambdaUpdateWrapper` 和 `Page`。筛选字段与排序只能来自服务端白名单，前端值作为绑定参数传入。
3. 联表投影、树、权限范围、统计和原子条件更新允许手写 SQL，但 SQL 位于相应 Mapper XML 或必要的 Mapper 注解中，使用参数绑定并映射到专门的查询投影，再由 Service 转换为响应 VO。参考 [MyBatis-Plus 持久层接口说明](https://baomidou.com/guides/data-interface/)；使用框架不意味着绝对没有 SQL。
4. Service 保留授权、规则、事务和 Mapper 编排，不再注入 `JdbcTemplate`、处理 `ResultSet` 或拼接 SQL。禁用账号、撤销会话、修改授权等操作必须保持数据库条件更新的原子性和受影响行数检查；不能简单改成先 `selectById` 再 `updateById`。
5. `ServiceImpl` 只在其通用方法确实减少重复代码时使用，不机械继承。Mapper 不写权限决策，Service 不写持久化细节。

截图中的会话功能目标为 `SysSessionMapper extends BaseMapper<SysSession>`。普通写入及简单条件更新使用 MyBatis-Plus；带用户名的分页查询和受保护账号判断使用 Mapper 自定义查询。权限判断和事务由 `SessionManagementService` 负责；管理员保护在同一事务和一致的数据库条件下验证，避免并发绕过。

## 时间规则

用户决定按 JeecgBoot、若依 RuoYi-Vue 的北京时间口径实现：接口时间格式为 `yyyy-MM-dd HH:mm:ss`，默认时区为 `Asia/Shanghai`（UTC+8）。JeecgBoot 和若依均在 Jackson 配置中使用 `GMT+8`，部分 `Date` 字段也标注格式。本项目使用 `Asia/Shanghai` 作为明确的地区时区标识，并保持相同的北京时间展示结果。

时间策略切换前，数据库连接、`AppTime` 和部分测试采用 UTC 约定。MySQL `DATETIME` 本身不带时区；切换时已停服务、备份，并用一次性脚本转换了开发库现有数据。新库直接采用北京时间会话，不执行历史数据转换脚本。

- **取时与计算**：提供可注入的 `Clock` Bean，时区为 `Asia/Shanghai`。Service 使用它获取当前时刻；到期时间先用 `Instant` 和 `Duration` 计算，再转换为北京时间的 `LocalDateTime` 写入 `DATETIME`。测试注入固定时钟。
- **数据库映射**：实体时间字段使用 `LocalDateTime`，由 MyBatis/MyBatis-Plus 类型处理器读取，不再通过 `ResultSet` 转成字符串。
- **接口输出**：响应 VO 使用 Java 时间类型。针对实际 Spring Boot 4/Jackson 版本，在一处 Jackson 配置中明确序列化 `LocalDateTime` 为 `yyyy-MM-dd HH:mm:ss`；`spring.jackson.date-format` 可覆盖传统 `Date`，不能未经验证就假定适用于全部 Java Time 类型。例外字段才使用字段级注解。
- **接口输入**：ISO 8601 时间范围在 Controller/参数绑定层解析为 `Instant` 或 `OffsetDateTime`，转换到 `Asia/Shanghai` 后进入 Service；查询 Service 不再反复解析字符串。
- **时区边界**：数据库连接会话与 Jackson 配置统一到 `Asia/Shanghai`/`+08:00`，验证 JVM、Jackson、MyBatis 和数据库行为。`LocalDateTime` 本身没有时区，不能指望设置 Jackson `time-zone` 自动转换数据库中已有的 `DATETIME`。

已删除混合 JDBC 映射职责的 `AppTime`，目前使用范围更小的 `DateUtils` 统一取时、ISO 参数换算和字符串格式；它不读取 `ResultSet`。后续迁移到实体和响应 VO 时，数据库映射及输出格式应分别落在 MyBatis 与 Jackson 边界。登录日志和操作日志须与其他列表呈现相同的时间格式，不带 `T`、`Z` 和毫秒。

## 实施顺序与验证

1. 记录现有接口、权限、时间和 SQL 用法，保存可恢复快照；后端当前不是 Git 仓库，且用户明确不需要 Git 提交。
2. 核对现有数据库的历史 `DATETIME` 数据和时间写入路径，确定是否需要一次性转换；随后完成 Jackson 时间配置、北京时间 `Clock` 和一个读写样例，验证时间契约后再批量迁移。
3. 建立 MyBatis-Plus 实体、Mapper 扫描与分页配置。以会话功能试迁移，验证列表、撤销、管理员保护、并发条件和时间输出。
4. 分批迁移用户与角色、部门与菜单、权限与审计、认证其他流程；每批移除对应 Service 的原生 SQL 和 JDBC 行映射，回归权限及事务。
5. 持久化稳定后再调整 Maven 模块位置与 Java 包，逐批编译；核对组件扫描、Mapper XML、Flyway 打包路径。已发布迁移文件名与内容不变。
6. 更新 `backend/AGENTS.md`、README、开发文档及受影响的 OpenAPI 说明。运行 `./mvnw test`、打包，并用 MySQL 验证方言、事务、权限和时间；H2 不替代 MySQL 关键回归。

完成标准：一个可执行服务；普通 CRUD 确实走 MyBatis-Plus；复杂 SQL 集中在 Mapper；系统 Service 没有 SQL/JDBC 行映射；`AppTime` 被职责明确的机制替代；接口、权限、历史迁移与时间输出保持兼容；目录和开发约定与实际代码一致。

## 实施结果（2026-10-03）

最终目录以[详细项目目录结构](../../../../开发文档/07-详细项目目录结构.md)及后端 `README.md` 为准。当前没有跨模块调用方或 Demo 代码，因此未创建 `admin-system-api` 与 `admin-module-demo`。用户、角色等功能已按业务拆分，并按后续要求统一采用 Service 接口与 `impl` 实现；单表 CRUD 由 MyBatis-Plus 承担，联表和条件 SQL 放在 Mapper XML，生产 Service 不再使用 `JdbcTemplate` 或 `ResultSet`。

统一 `Clock` 由启动模块提供，业务服务注入；`DateUtils` 负责北京时间换算，不再读取 JDBC 结果。2026-10-03 后续调整已将响应时间字段改为 `LocalDateTime`，在实际响应记录字段上使用 `@JsonFormat` 控制输出文本；HTTP JSON 仍为 `yyyy-MM-dd HH:mm:ss`。数据库实体不直接用作 HTTP 响应，时间注解放在响应边界才会生效。
