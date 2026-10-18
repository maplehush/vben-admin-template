# 前端开发约定

适用于本目录。实际产品应用是 `apps/web-antdv-next`；`playground` 保留为 Vben 示例和布局参考。实现系统页面前，先查看 `playground/src/views/system` 中的对应页面，再按本项目真实 API、字段和权限调整，不直接复制 Mock 数据或演示功能。

## 页面与组件

- 使用 Vben Admin 5 已封装的 `useVbenForm`、`useVbenVxeGrid`、`useVbenDrawer`、`useVbenModal`。系统表单的字段配置尽量放在页面同级 `data.ts`，列表组件负责数据加载和交互。
- 新增、编辑使用与同类页面一致的抽屉或对话框；表单重置、加载、提交、关闭和成功后刷新应完整。页面风格优先对齐 `playground`，同时保留本项目后端必需字段。
- 抽屉和对话框中的二态“状态”字段统一使用 `RadioGroup`，配置 `buttonStyle: 'solid'`、`optionType: 'button'`，选项为“已启用”(`ENABLED`) 和“已禁用”(`DISABLED`)；新增时默认 `ENABLED`。列表筛选器可以使用下拉框；可直接切换状态的列表使用带确认的开关，不可执行时禁用开关；其余列表值使用状态标签。
- 行操作的无权限项隐藏；有权限但因业务条件不可执行的项保留并设置 `disabled`，显示灰色。危险操作仅在可执行时显示红色。前端禁用只负责交互，后端仍须校验相同业务规则。
- 菜单标题和动态路由使用现有国际化机制；新增键时同时补齐中文、英文资源。避免把未翻译的 key 显示给用户。
- 响应时间统一按数据库时间原值格式化为 `yyyy-MM-dd HH:mm:ss`，由后端 `DateUtils` 和响应对象的 `@JsonFormat` 输出，不在展示阶段换算时区。列表、详情和会话页面直接显示接口返回值。

## 接口与验证

- API 类型、字段和权限码与后端契约保持一致。不要在页面写假数据或只靠按钮显隐实现鉴权。后端错误由统一请求处理展示，页面只处理必要的业务反馈。
- 不在表格适配层或页面再次转换时区或格式。日期范围筛选仍按接口要求提交带时区偏移的 ISO 8601 参数。
- 修改页面后，在本目录执行 `pnpm --filter @vben/web-antdv-next typecheck`，对改动文件执行 `pnpm exec eslint <files>`；涉及构建配置或公共组件时再运行 `pnpm --filter @vben/web-antdv-next build`。
- 注释说明业务原因、权限边界或不直观的组件约束；简单字段映射和显而易见的事件处理无需逐行注释。
