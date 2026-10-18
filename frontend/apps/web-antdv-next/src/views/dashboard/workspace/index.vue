<script setup lang="ts">
import type { WorkbenchQuickNavItem } from '@vben/common-ui';

import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { useAccess } from '@vben/access';
import { Page, WorkbenchHeader, WorkbenchQuickNav } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { preferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';

import { Card, Tag } from 'antdv-next';

const router = useRouter();
const userStore = useUserStore();
const accessStore = useAccessStore();
const { hasAccessByCodes } = useAccess();

// 工作台统计用于展示布局，接入公司业务时替换这里的数据即可。
const overviewItems = [
  { title: '待办事项', value: '6', icon: 'carbon:task', color: '#3b82f6' },
  { title: '进行中项目', value: '8', icon: 'carbon:development', color: '#8b5cf6' },
  { title: '团队成员', value: '24', icon: 'carbon:group', color: '#06b6d4' },
  { title: '本周已完成', value: '18', icon: 'carbon:checkmark-outline', color: '#22c55e' },
];

const projectItems = [
  { title: '产品规划', description: '整理需求与版本目标', progress: 72, color: '#3b82f6' },
  { title: '研发交付', description: '跟进开发与测试进度', progress: 58, color: '#8b5cf6' },
  { title: '运营支持', description: '协调上线与日常反馈', progress: 86, color: '#22c55e' },
];

const todoItems = [
  { title: '确认本周工作重点', description: '与团队同步优先级' },
  { title: '检查待处理事项', description: '汇总需要跟进的问题' },
  { title: '回顾项目进展', description: '记录阶段成果与风险' },
];

const navConfig = [
  { title: '用户管理', icon: 'carbon:user-multiple', color: '#3b82f6', url: '/system/user', code: 'system:user:list' },
  { title: '角色管理', icon: 'carbon:user-role', color: '#8b5cf6', url: '/system/role', code: 'system:role:list' },
  { title: '部门管理', icon: 'carbon:tree-view', color: '#06b6d4', url: '/system/dept', code: 'system:dept:list' },
  { title: '菜单管理', icon: 'carbon:menu', color: '#f59e0b', url: '/system/menu', code: 'system:menu:list' },
  { title: '接口权限', icon: 'carbon:security', color: '#ef4444', url: '/system/permission', code: 'system:permission:list' },
  { title: '个人中心', icon: 'carbon:user-avatar', color: '#22c55e', url: '/profile' },
];

const quickNavItems = computed<WorkbenchQuickNavItem[]>(() =>
  navConfig.filter(({ code, url }) =>
    !code ||
    (hasAccessByCodes([code]) && !!accessStore.getMenuByPath(url)),
  ),
);

const displayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '同事',
);

function navTo(item: WorkbenchQuickNavItem) {
  if (item.url) void router.push(item.url);
}
</script>

<template>
  <Page title="工作台" description="Maple Hush Admin 管理系统">
    <div class="space-y-5">
      <WorkbenchHeader
        :avatar="userStore.userInfo?.avatar || preferences.app.defaultAvatar"
      >
        <template #title>欢迎回来，{{ displayName }}！</template>
        <template #description>
          从这里开始今天的工作，常用管理功能可以通过下方快捷入口打开。
        </template>
        <template #actions>
          <div class="flex flex-col justify-center text-right">
            <span class="text-muted-foreground text-sm">当前账号</span>
            <span class="mt-1 font-medium">{{ userStore.userInfo?.username }}</span>
          </div>
        </template>
      </WorkbenchHeader>

      <section>
        <div class="mb-3 flex items-center gap-2">
          <h2 class="text-base font-semibold">业务概览</h2>
          <Tag color="blue">示例数据</Tag>
          <span class="text-muted-foreground text-xs">
            可根据公司的实际业务替换
          </span>
        </div>
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Card
            v-for="item in overviewItems"
            :key="item.title"
            :bordered="false"
          >
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">{{ item.title }}</span>
              <span class="rounded-lg bg-muted p-2">
                <IconifyIcon
                  :icon="item.icon"
                  :style="{ color: item.color }"
                  class="size-6"
                />
              </span>
            </div>
            <div class="mt-3 text-3xl font-semibold">{{ item.value }}</div>
          </Card>
        </div>
      </section>

      <div class="grid gap-5 lg:grid-cols-5">
        <Card :bordered="false" class="lg:col-span-3" title="项目示例">
          <div class="grid gap-4 md:grid-cols-3">
            <div
              v-for="project in projectItems"
              :key="project.title"
              class="rounded-lg border border-border p-4"
            >
              <div class="font-medium">{{ project.title }}</div>
              <p class="text-muted-foreground mt-2 text-sm">
                {{ project.description }}
              </p>
              <div class="mt-6 flex items-center gap-3">
                <div class="h-2 flex-1 overflow-hidden rounded-full bg-muted">
                  <div
                    class="h-full rounded-full"
                    :style="{
                      width: `${project.progress}%`,
                      backgroundColor: project.color,
                    }"
                  ></div>
                </div>
                <span class="text-muted-foreground text-xs">
                  {{ project.progress }}%
                </span>
              </div>
            </div>
          </div>
        </Card>
        <div class="lg:col-span-2">
          <WorkbenchQuickNav
            v-if="quickNavItems.length"
            title="快捷导航"
            :items="quickNavItems"
            @click="navTo"
          />
        </div>
      </div>

      <div class="grid gap-5 lg:grid-cols-5">
        <Card :bordered="false" class="lg:col-span-3" title="本周安排（示例）">
          <div class="divide-y divide-border">
            <div
              v-for="(todo, index) in todoItems"
              :key="todo.title"
              class="flex items-start gap-3 py-4 first:pt-0 last:pb-0"
            >
              <span
                class="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary"
              >
                {{ index + 1 }}
              </span>
              <div>
                <div class="font-medium">{{ todo.title }}</div>
                <p class="text-muted-foreground mt-1 text-sm">
                  {{ todo.description }}
                </p>
              </div>
            </div>
          </div>
        </Card>
        <Card :bordered="false" class="lg:col-span-2" title="工作提示">
          <div class="space-y-5 text-sm">
            <div>
              <div class="font-medium">管理组织与成员</div>
              <p class="text-muted-foreground mt-1">
                在部门和用户管理中维护组织结构及账号信息。
              </p>
            </div>
            <div>
              <div class="font-medium">配置访问权限</div>
              <p class="text-muted-foreground mt-1">
                通过角色管理分配菜单与操作权限。
              </p>
            </div>
            <div>
              <div class="font-medium">按需接入业务指标</div>
              <p class="text-muted-foreground mt-1">
                上方统计为静态示例，可在业务模块确定后替换。
              </p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  </Page>
</template>
