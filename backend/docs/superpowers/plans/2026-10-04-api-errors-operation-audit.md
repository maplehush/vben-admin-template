# 统一错误与操作审计实施计划

**目标：** 将跨业务使用的认证异常改为统一 API 异常，集中错误码；将五处重复操作日志写入收敛到 audit 功能包，保持 HTTP 契约与原事务语义。

**设计依据：** 用户接受对 JeecgBoot base-core 的分析并要求开始。采用适合当前项目的公共约定，不复制 JeecgBoot 的通用 Controller、动态查询或自动记录请求参数的切面。

**约束：** Java 21、Spring Boot 4、MyBatis-Plus；`code/data/message`、HTTP 状态和原错误文案保持兼容；操作日志只在成功写入的事务中记录；开发阶段不创建 Git 提交。

## 步骤

- [x] 增加统一错误码目录 `ApiErrorCode` 与 `ApiException`，`ApiResult` 增加失败工厂；替换业务代码中的 `AuthException`，保留各错误的 HTTP 状态、业务码与中文文案。
- [x] 增加异常与响应契约测试，覆盖验证码、失效会话、业务冲突、上传异常及兜底响应。
- [x] 在 audit 包增加操作日志类型、Mapper XML 与记录服务；测试请求 ID、来源 IP、无请求上下文、参数绑定。
- [x] 五个管理服务改用操作日志服务；删除旧 Mapper 与 XML 的重复 SQL；调整单元测试装配，确认写入与日志回滚仍在同一事务。
- [x] 更新后端约定与开发文档；运行 Spotless、完整 Maven 构建与测试，核对旧类和旧 SQL 已清理。
