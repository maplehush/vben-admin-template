# 系统模块重构实施计划

**目标：** 将后端整理为 JeecgBoot 风格的 Maven 多模块工程，按业务功能分包，并将普通持久化迁到 MyBatis-Plus；保持现有接口、权限和数据库迁移行为。

**依据：** `../specs/2026-10-03-jeecg-inspired-backend-layout-design.md`。用户明确不需要 Git 提交。

## 约束

- Java 21、Spring Boot 4.1.1、MySQL 8.4、MyBatis-Plus 3.5.17。
- 只有 `admin-system-start` 可启动；Flyway 已发布迁移不修改内容。
- 角色、菜单、部门、用户、会话和日志的权限及数据范围规则保持不变。
- 业务时间按北京时间和秒精度处理。

## 步骤

1. 保存源码快照并运行当前测试，记录基线；验证 MySQL 集成测试可使用隔离库。
2. 调整 Maven 层级：`admin-base-core`、`admin-module-system` 聚合；系统下设 `admin-system-biz` 和 `admin-system-start`。移除无实际代码的 API 和 Demo 模块；将迁移源归入 `backend/schema`。
3. 调整公共包和系统业务包：`common`、`config`，系统功能按 `auth/user/role/dept/menu/permission/audit/file`，其下按真实职责建立 `controller/service/mapper/entity/dto/vo/constant`。完成后先编译和运行单元测试。
4. 为业务表增加实体与 MyBatis-Plus `BaseMapper`；普通单表 CRUD 使用 MyBatis-Plus，联表、权限范围和条件更新集中到 Mapper，Service 保留规则和事务。逐功能迁移并运行对应测试。
5. 更新 AGENTS、README 和开发文档，检查包扫描、Flyway 装配与启动产物；运行全量单元测试、MySQL 集成测试、打包，并在隔离库核验权限与时间。

## 重点复核

- 角色授权、员工不可获超级管理员角色、部门层级与数据范围边界。
- 会话撤销和账号禁用的原子条件更新。
- Flyway V1–V11 校验和及新库初始化。
- 数据库时间与 API 时间一致，且为秒精度。
- 旧 URL、HTTP 响应及权限码保持兼容。
