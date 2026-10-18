# Vben Admin 5 后端架构

## 前端与入口

### Vben Admin 5

- 登录与退出
- 动态菜单与路由
- 按钮权限展示
- 用户、角色、部门、菜单管理页面
- 业务管理页面

### Nginx / API 入口

- 托管前端静态文件
- 将 /api 请求转发给 Spring Boot
- HTTPS 与访问日志

## Spring Boot 模块化单体

### 认证与会话 auth

- 登录：POST /auth/login
- 刷新令牌：POST /auth/refresh
- 退出登录：POST /auth/logout
- Access Token：Bearer Token
- Refresh Token：HttpOnly Cookie
- 登录限流与会话撤销

### 用户与组织 iam

- 用户管理：sys_user
- 角色管理：sys_role、sys_user_role
- 部门管理：sys_dept
- 用户信息：GET /user/info

### 权限与菜单 navigation

- 菜单模型：目录、页面、按钮；独立接口权限：API
- 角色授权：sys_role_menu、sys_role_permission
- 动态路由：GET /menu/all
- 权限码：GET /auth/codes
- 服务端接口鉴权：Spring Security
- 数据范围：全部、部门、本人、自定义

### 业务模块 business

- 按业务领域拆分模块
- 业务接口执行权限校验
- 列表查询叠加数据范围
- 关键操作写入审计日志

### 系统支撑 system

- 字典与系统参数
- 文件与对象存储
- 登录日志与操作审计
- 统一异常与响应格式：code = 0 表示成功

## MySQL 8.4 持久化

### 身份与组织

- sys_user：用户与密码哈希
- sys_role：角色与数据范围类型
- sys_user_role：用户角色关系
- sys_dept：部门树

### 权限与导航

- sys_menu：目录、页面、按钮
- sys_permission：独立 API 权限
- sys_role_menu：角色菜单与按钮授权
- sys_role_permission：角色接口权限授权
- sys_role_dept_scope：自定义部门数据范围

### 会话与审计

- sys_session：刷新令牌摘要与撤销状态
- sys_login_log：登录记录
- sys_operation_log：关键操作记录

### 设计约束

- InnoDB、utf8mb4、数据库迁移使用 Flyway
- 用户名、角色编码、权限码设置唯一索引
- 菜单常用字段独立成列，少量展示属性使用 JSON

## Redis

### 活跃会话与令牌撤销

### 登录限流

### 权限与菜单缓存

- 角色授权变化时清理缓存或更新权限版本

## 关键流程

### 登录

- 校验用户与密码 → 创建会话 → 返回 Access Token

### 页面初始化

- 获取 /user/info → 获取 /auth/codes → 获取 /menu/all → 生成可访问路由

### 访问业务接口

- 校验 Token → 校验操作权限 → 应用数据范围 → 执行业务 → 记录审计

## 建设顺序

### 第一阶段

- 打通登录、用户信息、权限码、动态菜单
- 完成用户、角色、菜单管理
- 前端切换真实 API 并验证不同角色

### 第二阶段

- 部门与数据权限
- 审计、字典、文件管理

### 第三阶段

- 扩展实际业务模块
- 根据负载和团队边界决定是否拆服务
