<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type {
  DeptNode,
  RoleSummary,
  Status,
  UserDetail,
  UserSummary,
} from '#/api/system';

import { computed, onMounted, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { Page, Tree, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Card,
  InputPassword,
  InputSearch,
  message,
  Modal,
  Select,
  Switch,
} from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  getDeptTree,
  getRoles,
  getUser,
  getUsers,
  replaceUserRoles,
  resetUserPassword,
  updateUserStatus,
} from '#/api/system';

import { columns, searchSchema } from './data';
import UserDetailDrawer from './modules/detail.vue';
import UserForm from './modules/form.vue';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();
const userStore = useUserStore();
const can = (code: string) => hasAccessByCodes([code]);
const isSuper = computed(() => hasAccessByRoles(['super_admin']));
const departmentTree = ref<DeptNode[]>([]);
const departmentSearch = ref('');
const selectedDeptId = ref<string>();
const visibleDepartments = computed(() => {
  const keyword = departmentSearch.value.trim().toLocaleLowerCase();
  if (!keyword) return departmentTree.value;
  const filter = (nodes: DeptNode[]): DeptNode[] =>
    nodes.flatMap((node) => {
      const children = filter(node.children);
      return node.deptName.toLocaleLowerCase().includes(keyword) || children.length
        ? [{ ...node, children }]
        : [];
    });
  return filter(departmentTree.value);
});
onMounted(async () => {
  if (!can('system:dept:list')) return;
  try {
    departmentTree.value = await getDeptTree();
  } catch {
    /* 全局请求拦截器展示错误 */
  }
});
watch(selectedDeptId, () => gridApi.query());
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: searchSchema, submitOnChange: true },
  gridOptions: {
    columns,
    height: 'auto',
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) =>
          getUsers({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...values,
            deptId: selectedDeptId.value,
          }),
      },
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<UserSummary>,
});

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: UserForm,
  destroyOnClose: true,
});
const [DetailDrawer, detailDrawerApi] = useVbenDrawer({
  connectedComponent: UserDetailDrawer,
  destroyOnClose: true,
});
const saving = ref(false);
const changingStatusId = ref<string>();
function openDetail(row: UserSummary) {
  detailDrawerApi.setData(row.id).open();
}
function openEditor(row?: UserSummary) {
  formDrawerApi
    .setData({ id: row?.id, deptId: row ? undefined : selectedDeptId.value })
    .open();
}

function changeStatus(row: UserSummary, enabled: boolean) {
  if (
    row.systemAccount ||
    row.id === userStore.userInfo?.userId ||
    !can('system:user:status')
  ) return;
  const status: Status = enabled ? 'ENABLED' : 'DISABLED';
  Modal.confirm({
    title: `${status === 'ENABLED' ? '启用' : '禁用'}用户`,
    content: `确定要${status === 'ENABLED' ? '启用' : '禁用'}账号 ${row.username} 吗？`,
    async onOk() {
      changingStatusId.value = row.id;
      try {
        await updateUserStatus(row.id, status);
        row.status = status;
        message.success('状态已更新');
        await gridApi.query();
      } finally {
        changingStatusId.value = undefined;
      }
    },
  });
}
const [GrantModal, grantModalApi] = useVbenModal({ onConfirm: saveGrant });
const grantUser = ref<UserDetail>();
const roleOptions = ref<RoleSummary[]>([]);
const selectedRoles = ref<string[]>([]);
async function loadRoleOptions() {
  const first = await getRoles({ page: 1, pageSize: 100 });
  const all = [...first.items];
  for (let page = 2; all.length < first.total; page++) {
    const next = await getRoles({ page, pageSize: 100 });
    all.push(...next.items);
    if (next.items.length === 0) break;
  }
  return all;
}
async function openGrant(row: UserSummary) {
  try {
    const [user, allRoles] = await Promise.all([
      getUser(row.id),
      loadRoleOptions(),
    ]);
    const superRole = allRoles.find((role) => role.roleCode === 'super_admin');
    if (superRole && user.roleIds.includes(superRole.id)) {
      message.warning('系统初始化账号不支持分配角色');
      return;
    }
    grantUser.value = user;
    selectedRoles.value = [...user.roleIds];
    roleOptions.value = allRoles.filter(
      (role) => role.roleCode !== 'super_admin',
    );
    grantModalApi.open();
  } catch {
    /* 全局请求拦截器展示错误 */
  }
}
async function saveGrant() {
  if (!grantUser.value) return;
  saving.value = true;
  try {
    await replaceUserRoles(grantUser.value.id, selectedRoles.value);
    grantModalApi.close();
    message.success('角色分配成功');
    gridApi.query();
  } catch {
    /* 全局请求拦截器展示错误 */
  } finally {
    saving.value = false;
  }
}
const [PasswordModal, passwordModalApi] = useVbenModal({
  onConfirm: savePassword,
});
const passwordUser = ref<UserSummary>();
const temporaryPassword = ref('');
function openPassword(row: UserSummary) {
  passwordUser.value = row;
  temporaryPassword.value = '';
  passwordModalApi.open();
}
async function savePassword() {
  if (!passwordUser.value) return;
  if (
    temporaryPassword.value.length < 12 ||
    temporaryPassword.value.length > 128
  )
    return message.warning('临时密码须为 12–128 位');
  saving.value = true;
  try {
    await resetUserPassword(passwordUser.value.id, temporaryPassword.value);
    passwordModalApi.close();
    temporaryPassword.value = '';
    message.success('密码已重置，请安全地通知该用户');
  } catch {
    /* 全局请求拦截器展示错误 */
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <div class="flex size-full min-h-0 flex-col gap-4 lg:flex-row">
      <Card
        v-if="can('system:dept:list')"
        class="h-56 w-full shrink-0 overflow-auto lg:h-full lg:w-64"
      >
        <InputSearch
          v-model:value="departmentSearch"
          placeholder="搜索部门..."
          class="mb-3"
          allow-clear
        />
        <Tree
          v-model="selectedDeptId"
          :tree-data="visibleDepartments"
          label-field="deptName"
          value-field="id"
          :default-expanded-level="2"
          allow-clear
        />
      </Card>
      <Grid class="min-h-0 min-w-0 flex-1" table-title="用户列表">
        <template #toolbar-tools>
          <Button
            v-if="can('system:user:create')"
            type="primary"
            @click="openEditor()"
          >
            <Plus class="size-5" />新增用户
          </Button>
        </template>
        <template #status="{ row }">
          <Switch
            :checked="row.status === 'ENABLED'"
            :disabled="row.systemAccount || row.id === userStore.userInfo?.userId || !can('system:user:status')"
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
                text: '详情',
                icon: 'lucide:eye',
                onClick: () => openDetail(row),
              },
              {
                auth: 'system:user:update',
                text: '编辑',
                onClick: () => openEditor(row),
              },
            ]"
            :dropdown-actions="[
              {
                auth: 'system:user:grant',
                ifShow: isSuper,
                text: '分配角色',
                onClick: () => openGrant(row),
              },
              {
                auth: 'system:user:reset-password',
                text: '重置密码',
                onClick: () => openPassword(row),
              },
            ]"
            more-text="更多"
          />
        </template>
      </Grid>
    </div>
    <FormDrawer @success="gridApi.query()" />
    <DetailDrawer />
    <GrantModal title="分配角色" :loading="saving">
      <p class="mb-3">{{ grantUser?.username }}</p>
      <Select
        v-model:value="selectedRoles"
        mode="multiple"
        :options="
          roleOptions.map((role) => ({
            label: `${role.roleName} (${role.roleCode})`,
            value: role.id,
            disabled: role.status !== 'ENABLED',
          }))
        "
        class="w-full"
      />
    </GrantModal>
    <PasswordModal title="重置密码" :loading="saving">
      <p class="mb-3">
        为 {{ passwordUser?.username }} 设置临时密码。下次登录须修改密码。
      </p>
      <InputPassword
        v-model:value="temporaryPassword"
        :maxlength="128"
        autocomplete="new-password"
      />
    </PasswordModal>
  </Page>
</template>
