<script setup lang="ts">
import type { Dayjs } from 'dayjs';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type {
  DataScopeType,
  DeptNode,
  MenuNode,
  PermissionView,
  RoleSummary,
} from '#/api/system';

import { computed, ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page, Tree, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Select, Switch } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  deleteRole,
  getDeptTree,
  getMenuTree,
  getPermissions,
  getRoleDataScope,
  getRoleMenus,
  getRolePermissions,
  getRoles,
  replaceRoleDataScope,
  replaceRoleMenus,
  replaceRolePermissions,
  updateRole,
} from '#/api/system';
import { $t } from '#/locales';

import { columns, scopeOptions, searchSchema } from './data';
import { enabledMenuIds, sameMenuIds } from './menu-selection';
import RoleForm from './modules/form.vue';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();
const can = (code: string) => hasAccessByCodes([code]);
const isSuper = computed(() => hasAccessByRoles(['super_admin']));
type RoleSearchValues = Record<string, unknown> & { createTime?: [Dayjs, Dayjs] };
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: searchSchema, submitOnChange: true },
  gridOptions: {
    columns,
    height: 'auto',
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) => {
          const { createTime, ...filters } = values as RoleSearchValues;
          return getRoles({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...filters,
            startTime: createTime?.[0]?.startOf('day').toDate().toISOString(),
            endTime: createTime?.[1]?.add(1, 'day').startOf('day').toDate().toISOString(),
          });
        },
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<RoleSummary>,
});
const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: RoleForm,
  destroyOnClose: true,
});
const saving = ref(false);
const changingStatusId = ref<string>();
function openEditor(row?: RoleSummary) {
  formDrawerApi.setData(row?.id ?? null).open();
}

function changeStatus(row: RoleSummary, enabled: boolean) {
  if (row.roleCode === 'super_admin' || !isSuper.value || !can('system:role:update'))
    return;
  const status = enabled ? 'ENABLED' : 'DISABLED';
  Modal.confirm({
    title: '切换角色状态',
    content: `确定${enabled ? '启用' : '禁用'}角色「${row.roleName}」吗？`,
    async onOk() {
      changingStatusId.value = row.id;
      try {
        await updateRole(row.id, {
          roleName: row.roleName,
          description: row.description,
          status,
        });
        row.status = status;
        message.success('状态已更新');
        await gridApi.query();
      } finally {
        changingStatusId.value = undefined;
      }
    },
  });
}

function removeRole(row: RoleSummary) {
  Modal.confirm({
    title: '删除角色',
    content: `确定删除 ${row.roleName} 吗？此操作不可撤销。`,
    async onOk() {
      await deleteRole(row.id);
      message.success('删除成功');
      gridApi.query();
    },
  });
}
const [GrantModal, grantModalApi] = useVbenModal({ onConfirm: saveGrant });
const grantRole = ref<RoleSummary>();
const menuTree = ref<MenuNode[]>([]);
const menuIds = ref<string[]>([]);
function mapMenu(nodes: MenuNode[]): Array<Record<string, unknown>> {
  return nodes.map((node) => ({
    ...node,
    label: $t(node.titleKey),
    disabled: node.status !== 'ENABLED',
    children: mapMenu(node.children ?? []),
  }));
}
const visibleMenus = computed(() => mapMenu(menuTree.value));
async function openGrant(row: RoleSummary) {
  try {
    grantRole.value = row;
    const [tree, granted] = await Promise.all([
      getMenuTree(),
      getRoleMenus(row.id),
    ]);
    menuTree.value = tree;
    menuIds.value = [...granted.menuIds];
    grantModalApi.open();
  } catch {
    /* 全局请求拦截器展示错误 */
  }
}
async function saveGrant() {
  if (!grantRole.value) return;
  saving.value = true;
  try {
    const selected = [...new Set(menuIds.value)];
    await replaceRoleMenus(grantRole.value.id, selected);
    const saved = await getRoleMenus(grantRole.value.id);
    if (!sameMenuIds(selected, saved.menuIds)) {
      message.error('授权保存结果与勾选内容不一致，请重试');
      return;
    }
    grantModalApi.close();
    message.success('菜单与按钮授权已保存');
  } catch {
    /* 全局请求拦截器展示错误 */
  } finally {
    saving.value = false;
  }
}
const [PermissionModal, permissionModalApi] = useVbenModal({
  onConfirm: savePermissionGrant,
});
const permissionRole = ref<RoleSummary>();
const permissionIds = ref<string[]>([]);
const permissionOptions = ref<Array<{ label: string; value: string }>>([]);
async function openPermissionGrant(row: RoleSummary) {
  try {
    permissionRole.value = row;
    const [first, granted] = await Promise.all([
      getPermissions({ page: 1, pageSize: 100 }),
      getRolePermissions(row.id),
    ]);
    const items: PermissionView[] = [...first.items];
    for (let page = 2; items.length < first.total; page++) {
      const next = await getPermissions({ page, pageSize: 100 });
      items.push(...next.items);
      if (next.items.length === 0) break;
    }
    permissionOptions.value = items
      .filter((item) => item.status === 'ENABLED')
      .map((item) => ({
        label: `${item.titleKey} (${item.permissionCode})`,
        value: item.id,
      }));
    permissionIds.value = [...granted.permissionIds];
    permissionModalApi.open();
  } catch {
    /* 全局请求拦截器展示错误 */
  }
}
async function savePermissionGrant() {
  if (!permissionRole.value) return;
  saving.value = true;
  try {
    await replaceRolePermissions(permissionRole.value.id, permissionIds.value);
    permissionModalApi.close();
    message.success('接口权限授权已保存');
  } catch {
    /* 全局请求拦截器展示错误 */
  } finally {
    saving.value = false;
  }
}
const [ScopeModal, scopeModalApi] = useVbenModal({ onConfirm: saveScope });
const scopeRole = ref<RoleSummary>();
const scopeType = ref<DataScopeType>('SELF');
const scopeDeptIds = ref<string[]>([]);
const depts = ref<DeptNode[]>([]);
async function openScope(row: RoleSummary) {
  try {
    scopeRole.value = row;
    const [scope, tree] = await Promise.all([
      getRoleDataScope(row.id),
      getDeptTree(),
    ]);
    scopeType.value = scope.type;
    scopeDeptIds.value = [...scope.deptIds];
    depts.value = tree;
    scopeModalApi.open();
  } catch {
    /* 全局请求拦截器展示错误 */
  }
}
async function saveScope() {
  if (!scopeRole.value) return;
  saving.value = true;
  try {
    await replaceRoleDataScope(
      scopeRole.value.id,
      scopeType.value,
      scopeType.value === 'CUSTOM' ? scopeDeptIds.value : [],
    );
    scopeModalApi.close();
    message.success('数据范围已保存');
    gridApi.query();
  } catch {
    /* 全局请求拦截器展示错误 */
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="角色列表">
      <template #toolbar-tools>
        <Button
          v-if="isSuper && can('system:role:create')"
          type="primary"
          @click="openEditor()"
        >
          <Plus class="size-5" />新增角色
        </Button>
      </template>
      <template #status="{ row }">
        <Switch
          :checked="row.roleCode === 'super_admin' || row.status === 'ENABLED'"
          :disabled="row.roleCode === 'super_admin' || !isSuper || !can('system:role:update')"
          :loading="changingStatusId === row.id"
          checked-children="已启用"
          un-checked-children="已禁用"
          @change="(checked) => changeStatus(row, checked)"
        />
      </template>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          :actions="[
            {
              auth: 'system:role:update',
              ifShow: isSuper && row.roleCode !== 'super_admin',
              text: '编辑',
              onClick: () => openEditor(row),
            },
            {
              auth: 'system:role:grant',
              ifShow: isSuper && row.roleCode !== 'super_admin',
              text: '菜单与按钮',
              onClick: () => openGrant(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:role:grant',
              ifShow: isSuper && row.roleCode !== 'super_admin',
              text: '接口权限',
              onClick: () => openPermissionGrant(row),
            },
            {
              auth: 'system:role:data-scope',
              ifShow: isSuper && row.roleCode !== 'super_admin',
              text: '数据范围',
              onClick: () => openScope(row),
            },
            {
              auth: 'system:role:delete',
              ifShow: isSuper && row.roleCode !== 'super_admin',
              text: '删除',
              danger: true,
              onClick: () => removeRole(row),
            },
          ]"
          more-text="更多"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.query()" />
    <GrantModal title="菜单与按钮授权" :loading="saving">
      <p class="mb-3">{{ grantRole?.roleName }}：页面与按钮分别勾选。</p>
      <div class="mb-3 flex items-center gap-3">
        <Button size="small" @click="menuIds = enabledMenuIds(menuTree)">全选</Button>
        <Button size="small" @click="menuIds = []">清空</Button>
        <span class="text-muted-foreground text-xs">已选 {{ menuIds.length }} 项</span>
      </div>
      <div class="max-h-96 overflow-auto">
        <Tree
          v-model="menuIds"
          :tree-data="visibleMenus"
          multiple
          include-indeterminate
          bordered
          label-field="label"
          value-field="id"
          :default-expanded-level="2"
        />
      </div>
    </GrantModal>
    <PermissionModal title="接口权限授权" :loading="saving">
      <p class="mb-3">
        {{ permissionRole?.roleName }}：仅无按钮对应的接口权限。
      </p>
      <Select
        v-model:value="permissionIds"
        mode="multiple"
        :options="permissionOptions"
        class="w-full"
        placeholder="选择接口权限"
      />
    </PermissionModal>
    <ScopeModal title="数据范围" :loading="saving">
      <p class="mb-3">{{ scopeRole?.roleName }}</p>
      <Select
        v-model:value="scopeType"
        :options="scopeOptions"
        class="w-full"
      />
      <div v-if="scopeType === 'CUSTOM'" class="mt-3 max-h-80 overflow-auto">
        <Tree
          v-model="scopeDeptIds"
          :tree-data="depts"
          multiple
          check-strictly
          :auto-check-parent="false"
          label-field="deptName"
          value-field="id"
          :default-expanded-level="2"
        />
      </div>
    </ScopeModal>
  </Page>
</template>
