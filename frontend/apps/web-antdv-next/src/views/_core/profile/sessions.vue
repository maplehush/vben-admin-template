<script setup lang="ts">
import type { SessionView } from '#/api/system';

import { onMounted, ref } from 'vue';

import { Button, message, Modal, Tag } from 'antdv-next';

import { getOwnSessions, revokeOwnSession } from '#/api/system';
import { useAuthStore } from '#/store';

const authStore = useAuthStore();
const sessions = ref<SessionView[]>([]);
const loading = ref(false);

async function refresh() {
  loading.value = true;
  try {
    sessions.value = await getOwnSessions();
  } finally {
    loading.value = false;
  }
}

function revoke(session: SessionView) {
  Modal.confirm({
    title: session.current ? '退出当前会话' : '撤销会话',
    content: session.current
      ? '当前设备会立即退出登录，确定继续吗？'
      : '该设备会立即退出登录，确定继续吗？',
    async onOk() {
      await revokeOwnSession(session.id);
      if (session.current) {
        await authStore.logout(false);
      } else {
        message.success('会话已撤销');
        await refresh();
      }
    },
  });
}

onMounted(refresh);
</script>

<template>
  <div class="max-w-3xl">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-base font-semibold">我的在线会话</h2>
        <p class="text-muted-foreground mt-1 text-sm">
          撤销其他设备的会话后，该设备需要重新登录。
        </p>
      </div>
      <Button :loading="loading" @click="refresh">刷新</Button>
    </div>
    <div v-if="!sessions.length && !loading" class="text-muted-foreground py-8">
      暂无在线会话
    </div>
    <div
      v-for="session in sessions"
      :key="session.id"
      class="flex flex-wrap items-center justify-between gap-4 border-b border-border py-4"
    >
      <div>
        <div class="flex items-center gap-2 font-medium">
          {{ session.deviceInfo || '未知设备' }}
          <Tag v-if="session.current" color="blue">当前设备</Tag>
        </div>
        <p class="text-muted-foreground mt-1 text-sm">
          IP：{{ session.ipAddress || '未知' }} · 登录时间：{{ session.createTime }}
        </p>
      </div>
      <Button danger type="link" @click="revoke(session)">
        {{ session.current ? '退出' : '撤销' }}
      </Button>
    </div>
  </div>
</template>
