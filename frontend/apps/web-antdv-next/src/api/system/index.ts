import { requestClient } from '#/api/request';

export type Status = 'DISABLED' | 'ENABLED';
export interface PageResult<T> {
  items: T[];
  total: number;
}
export interface UserSummary {
  id: string;
  username: string;
  realName: string;
  remark: null | string;
  email: null | string;
  phone: null | string;
  primaryDeptId: null | string;
  primaryDeptName: null | string;
  status: Status;
  createTime: string;
  systemAccount: boolean;
}
export interface UserDetail extends UserSummary {
  avatarUrl: null | string;
  homePath: null | string;
  roleIds: string[];
  updateTime: string;
}
export interface RoleSummary {
  id: string;
  roleCode: string;
  roleName: string;
  description: null | string;
  status: Status;
  dataScopeType: DataScopeType;
  createTime: string;
}
export interface RoleDetail extends RoleSummary {
  updateTime: string;
}
export type DataScopeType =
  | 'ALL'
  | 'CUSTOM'
  | 'DEPT'
  | 'DEPT_AND_CHILDREN'
  | 'SELF';
export interface DeptNode {
  id: string;
  parentId: null | string;
  deptCode: string;
  deptName: string;
  sortOrder: number;
  status: Status;
  remark: null | string;
  createTime: string;
  children: DeptNode[];
}
export interface DeptDetail extends DeptNode {
  updateTime: string;
}
export interface DeptWrite {
  parentId: null | string;
  deptName: string;
  sortOrder: number;
  status: Status;
  remark: null | string;
}
export interface MenuNode {
  id: string;
  parentId: null | string;
  type: 'BUTTON' | 'CATALOG' | 'EMBEDDED' | 'LINK' | 'MENU';
  menuKey: string;
  routeName: null | string;
  routePath: null | string;
  component: null | string;
  redirectPath: null | string;
  authCode: null | string;
  titleKey: string;
  icon: null | string;
  sortOrder: number;
  routeMeta: MenuRouteMeta | null;
  status: Status;
  children: MenuNode[];
}
export interface MenuDetail extends MenuNode {
  createTime: string;
  updateTime: string;
}
export interface PermissionView {
  id: string;
  menuId: null | string;
  permissionKey: string;
  permissionCode: string;
  titleKey: string;
  sortOrder: number;
  status: Status;
  createTime: string;
  updateTime: string;
}
export interface DictView {
  id: string;
  dictCode: string;
  dictName: string;
  status: Status;
  builtIn: boolean;
  remark: null | string;
  createTime: string;
  updateTime: string;
}
export interface DictItemView {
  id: string;
  dictId: string;
  itemValue: string;
  itemLabel: string;
  sortOrder: number;
  status: Status;
  remark: null | string;
  itemColor: DictItemColor | null;
  createTime: string;
  updateTime: string;
}
export interface DictWrite {
  dictCode: string;
  dictName: string;
  status: Status;
  remark: null | string;
}
export interface DictItemWrite {
  itemValue: string;
  itemLabel: string;
  sortOrder: number;
  status: Status;
  remark: null | string;
  itemColor: DictItemColor | null;
}
export type DictItemColor = 'error' | 'processing' | 'success' | 'warning' | `#${string}`;
export interface DictQuery {
  page: number;
  pageSize: number;
  dictCode?: string;
  dictName?: string;
  status?: Status;
}
export interface DictItemQuery {
  page: number;
  pageSize: number;
  itemLabel?: string;
  status?: Status;
}
export interface SessionView {
  id: string;
  username: string;
  deviceInfo: null | string;
  ipAddress: null | string;
  createTime: string;
  expireTime: string;
  current: boolean;
}
export interface LoginLog {
  id: string;
  username: string;
  success: boolean;
  failureReason: null | string;
  ipAddress: null | string;
  userAgent: null | string;
  occurTime: string;
}
export interface OperationLog {
  id: string;
  username: null | string;
  actionCode: string;
  targetType: null | string;
  targetId: null | string;
  success: boolean;
  ipAddress: null | string;
  occurTime: string;
}
export interface MenuWrite {
  parentId: null | string;
  routeName: null | string;
  routePath: null | string;
  component: null | string;
  redirectPath: null | string;
  titleKey: string;
  icon: null | string;
  sortOrder: number;
  routeMeta: MenuRouteMeta | null;
  status: Status;
}
export interface MenuRouteMeta {
  activeIcon?: string;
  affixTab?: boolean;
  badge?: string;
  badgeType?: 'dot' | 'normal';
  badgeVariants?: 'default' | 'destructive' | 'primary' | 'success' | 'warning';
  hideChildrenInMenu?: boolean;
  hideInBreadcrumb?: boolean;
  hideInMenu?: boolean;
  hideInTab?: boolean;
  icon?: string;
  iframeSrc?: string;
  keepAlive?: boolean;
  link?: string;
  order?: number;
  title?: string;
}
export interface UserWrite {
  realName: string;
  remark: null | string;
  email: null | string;
  phone: null | string;
  primaryDeptId: null | string;
  avatarUrl: null | string;
  homePath: null | string;
}
export interface RoleWrite {
  roleName: string;
  description: null | string;
  status: Status;
  menuIds?: string[];
}

export const getUsers = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<UserSummary>>('/system/users', { params });
export const getUser = (id: string) =>
  requestClient.get<UserDetail>(`/system/users/${id}`);
export const createUser = (
  body: Omit<UserWrite, 'primaryDeptId'> & {
    primaryDeptId: string;
    username: string;
    status: Status;
    roleIds: string[];
  },
) => requestClient.post<boolean>('/system/users', body);
export const updateUser = (id: string, body: UserWrite) =>
  requestClient.put<boolean>(`/system/users/${id}`, body);
export const updateUserStatus = (id: string, status: Status) =>
  requestClient.put<boolean>(`/system/users/${id}/status`, { status });
export const replaceUserRoles = (id: string, roleIds: string[]) =>
  requestClient.put<boolean>(`/system/users/${id}/roles`, { roleIds });
export const resetUserPassword = (id: string, temporaryPassword: string) =>
  requestClient.post<boolean>(`/system/users/${id}/reset-password`, {
    temporaryPassword,
  });

export const getRoles = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<RoleSummary>>('/system/roles', { params });
export const getRole = (id: string) =>
  requestClient.get<RoleDetail>(`/system/roles/${id}`);
export const createRole = (body: RoleWrite & { roleCode: string }) =>
  requestClient.post<boolean>('/system/roles', body);
export const updateRole = (id: string, body: RoleWrite) =>
  requestClient.put<boolean>(`/system/roles/${id}`, body);
export const deleteRole = (id: string) =>
  requestClient.delete<boolean>(`/system/roles/${id}`);
export const getRoleMenus = (id: string) =>
  requestClient.get<{ menuIds: string[] }>(`/system/roles/${id}/menus`);
export const replaceRoleMenus = (id: string, menuIds: string[]) =>
  requestClient.put<boolean>(`/system/roles/${id}/menus`, { menuIds });
export const getRolePermissions = (id: string) =>
  requestClient.get<{ permissionIds: string[] }>(
    `/system/roles/${id}/permissions`,
  );
export const replaceRolePermissions = (id: string, permissionIds: string[]) =>
  requestClient.put<boolean>(`/system/roles/${id}/permissions`, {
    permissionIds,
  });
export const getRoleDataScope = (id: string) =>
  requestClient.get<{ type: DataScopeType; deptIds: string[] }>(
    `/system/roles/${id}/data-scope`,
  );
export const replaceRoleDataScope = (
  id: string,
  type: DataScopeType,
  deptIds: string[],
) =>
  requestClient.put<boolean>(`/system/roles/${id}/data-scope`, {
    type,
    deptIds,
  });
export const getDeptTree = () =>
  requestClient.get<DeptNode[]>('/system/depts/tree');
export const getDept = (id: string) =>
  requestClient.get<DeptDetail>(`/system/depts/${id}`);
export const createDept = (body: DeptWrite) =>
  requestClient.post<boolean>('/system/depts', body);
export const updateDept = (id: string, body: DeptWrite) =>
  requestClient.put<boolean>(`/system/depts/${id}`, body);
export const deleteDept = (id: string) =>
  requestClient.delete<boolean>(`/system/depts/${id}`);
export const getMenuTree = () =>
  requestClient.get<MenuNode[]>('/system/menus/tree');
export const getMenu = (id: string) =>
  requestClient.get<MenuDetail>(`/system/menus/${id}`);
export const createMenu = (
  body: MenuWrite & {
    type: MenuNode['type'];
    menuKey: string;
    authCode: null | string;
  },
) => requestClient.post<boolean>('/system/menus', body);
export const updateMenu = (id: string, body: MenuWrite) =>
  requestClient.put<boolean>(`/system/menus/${id}`, body);
export const deleteMenu = (id: string) =>
  requestClient.delete<boolean>(`/system/menus/${id}`);
export const getPermissions = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<PermissionView>>('/system/permissions', {
    params,
  });
export const getPermission = (id: string) =>
  requestClient.get<PermissionView>(`/system/permissions/${id}`);
export const createPermission = (body: {
  menuId: null | string;
  permissionKey: string;
  permissionCode: string;
  titleKey: string;
  sortOrder: number;
  status: Status;
}) => requestClient.post<boolean>('/system/permissions', body);
export const updatePermission = (
  id: string,
  body: {
    menuId: null | string;
    titleKey: string;
    sortOrder: number;
    status: Status;
  },
) => requestClient.put<boolean>(`/system/permissions/${id}`, body);
export const deletePermission = (id: string) =>
  requestClient.delete<boolean>(`/system/permissions/${id}`);
export const getDicts = (params: DictQuery) =>
  requestClient.get<PageResult<DictView>>('/system/dicts', { params });
export const createDict = (body: DictWrite) =>
  requestClient.post<boolean>('/system/dicts', body);
export const updateDict = (id: string, body: DictWrite) =>
  requestClient.put<boolean>(`/system/dicts/${id}`, body);
export const deleteDict = (id: string) =>
  requestClient.delete<boolean>(`/system/dicts/${id}`);
export const getDictItems = (dictId: string, params: DictItemQuery) =>
  requestClient.get<PageResult<DictItemView>>(`/system/dicts/${dictId}/items`, { params });
export const createDictItem = (dictId: string, body: DictItemWrite) =>
  requestClient.post<boolean>(`/system/dicts/${dictId}/items`, body);
export const updateDictItem = (dictId: string, id: string, body: DictItemWrite) =>
  requestClient.put<boolean>(`/system/dicts/${dictId}/items/${id}`, body);
export const deleteDictItem = (dictId: string, id: string) =>
  requestClient.delete<boolean>(`/system/dicts/${dictId}/items/${id}`);
export const getDictOptions = (code: string) =>
  requestClient.get<Array<{ itemColor: DictItemColor | null; label: string; value: string }>>(
    `/system/dict-options/${encodeURIComponent(code)}`,
  );
export const getOwnSessions = () =>
  requestClient.get<SessionView[]>('/auth/sessions');
export const revokeOwnSession = (id: string) =>
  requestClient.delete<boolean>(`/auth/sessions/${id}`);
export const getSessions = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<SessionView>>('/system/sessions', { params });
export const revokeSession = (id: string) =>
  requestClient.delete<boolean>(`/system/sessions/${id}`);
export const getLoginLogs = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<LoginLog>>('/system/login-logs', { params });
export const getOperationLogs = (params: Record<string, unknown>) =>
  requestClient.get<PageResult<OperationLog>>('/system/operation-logs', {
    params,
  });
