<script setup lang="ts">
import type { UploadFile } from 'antdv-next';

import type { DeptNode, RoleSummary, Status } from '#/api/system';

import { computed, nextTick, ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenDrawer, z } from '@vben/common-ui';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  createUser,
  getDeptTree,
  getRoles,
  getUser,
  updateUser,
} from '#/api/system';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();
const { hasAccessByRoles } = useAccess();
const editingId = ref<string>();
const depts = ref<DeptNode[]>([]);
const roles = ref<RoleSummary[]>([]);
const isSuper = computed(() => hasAccessByRoles(['super_admin']));
const schema = useFormSchema(depts, roles);
type UserDrawerData = { deptId?: string; id?: string };
const [Form, formApi] = useVbenForm({
  commonConfig: {
    formItemClass: 'col-span-2 md:col-span-1',
  },
  layout: 'vertical',
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-4',
});
const [Drawer, drawerApi] = useVbenDrawer<UserDrawerData>({
  async onOpenChange(open) {
    if (!open) return;
    const { deptId, id } = drawerApi.getData() ?? {};
    editingId.value = id;
    formApi.reset();
    formApi.updateSchema([
      { fieldName: 'username', hide: Boolean(editingId.value) },
      { fieldName: 'initialPassword', hide: Boolean(editingId.value) },
      { fieldName: 'primaryDeptId', rules: 'required' },
      {
        fieldName: 'roleIds',
        hide: Boolean(editingId.value) || !isSuper.value,
      },
      { fieldName: 'status', hide: Boolean(editingId.value) },
    ]);
    await nextTick();
    try {
      depts.value = await getDeptTree();
      if (!editingId.value && isSuper.value) {
        const first = await getRoles({ page: 1, pageSize: 100 });
        const all = [...first.items];
        for (let page = 2; all.length < first.total; page++) {
          const next = await getRoles({ page, pageSize: 100 });
          all.push(...next.items);
          if (next.items.length === 0) break;
        }
        roles.value = all.filter((role) => role.roleCode !== 'super_admin');
      }
      if (editingId.value) {
        const detail = await getUser(editingId.value);
        if (detail.systemAccount) {
          formApi.updateSchema([
            { fieldName: 'primaryDeptId', rules: z.string().optional() },
          ]);
        }
        await formApi.setValues({
          ...detail,
          avatarFiles: detail.avatarUrl
            ? [{
                uid: `avatar-${detail.id}`,
                name: '头像',
                status: 'done',
                type: 'image/png',
                url: detail.avatarUrl,
              }]
            : [],
          primaryDeptId: detail.primaryDeptId ?? undefined,
        });
      } else {
        await formApi.setValues({
          avatarFiles: [],
          primaryDeptId: deptId,
          initialPassword: '12345678',
          roleIds: [],
          status: 'ENABLED',
        });
      }
    } catch {
      drawerApi.close();
    }
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const username = String(values.username ?? '');
    const realName = String(values.realName ?? '').trim();
    if (!editingId.value && !/^[A-Za-z0-9_.-]{3,64}$/.test(username))
      return message.warning('用户名须为 3–64 位字母、数字或 _ . -');
    if (!realName || realName.length > 128)
      return message.warning('请填写姓名（最多 128 字）');
    if (!values.primaryDeptId && !editingId.value)
      return message.warning('请选择部门');
    const avatarFile = (values.avatarFiles as undefined | UploadFile[])?.[0];
    if (avatarFile?.status === 'uploading')
      return message.warning('头像正在上传，请稍后保存');
    if (avatarFile?.status === 'error')
      return message.warning('头像上传失败，请重新上传');
    const avatarUrl = avatarFile?.url ?? avatarFile?.response?.url ?? null;
    if (avatarFile && !avatarUrl)
      return message.warning('头像尚未上传完成');
    const data = {
      realName,
      remark: String(values.remark ?? '').trim() || null,
      email: String(values.email ?? '').trim() || null,
      phone: String(values.phone ?? '').trim() || null,
      primaryDeptId: (values.primaryDeptId as string | undefined) || null,
      avatarUrl,
      homePath: String(values.homePath ?? '').trim() || null,
    };
    drawerApi.lock();
    try {
      await (editingId.value
        ? updateUser(editingId.value, data)
        : createUser({
            ...data,
            primaryDeptId: String(values.primaryDeptId),
            username,
            status: (values.status as Status) ?? 'ENABLED',
            roleIds: (values.roleIds as string[]) ?? [],
          }));
      message.success('保存成功');
      drawerApi.close();
      emit('success');
    } catch {
      /* 全局请求拦截器展示错误 */
    } finally {
      drawerApi.unlock();
    }
  },
});
defineExpose({ drawerApi });
</script>

<template>
  <Drawer class="w-full max-w-200" :title="editingId ? '编辑用户' : '新增用户'">
    <Form class="mx-4" />
  </Drawer>
</template>
