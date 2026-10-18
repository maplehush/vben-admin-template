<script setup lang="ts">
import type { UserDetail } from '#/api/system';

import { computed, ref } from 'vue';

import { useVbenDrawer, VbenDescriptions } from '@vben/common-ui';

import { getUser } from '#/api/system';

import { useDescriptionItems } from '../data';

const detail = ref<UserDetail>();
const items = computed(() => useDescriptionItems(detail.value));
const [Drawer, drawerApi] = useVbenDrawer<string>({
  async onOpenChange(open) {
    if (!open) return;
    detail.value = undefined;
    const id = drawerApi.getData();
    if (!id) return;
    try {
      detail.value = await getUser(id);
    } catch {
      drawerApi.close();
    }
  },
});
</script>

<template>
  <Drawer :footer="false" title="用户详情">
    <VbenDescriptions bordered :column="1" :items="items" />
  </Drawer>
</template>
