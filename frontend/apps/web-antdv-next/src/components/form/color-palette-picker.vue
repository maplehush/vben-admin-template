<script setup lang="ts">
defineProps<{
  disabled?: boolean;
  value?: null | string;
}>();

const emit = defineEmits<{
  'update:value': [value: null | string];
}>();

const colors = [
  { label: '蓝色', value: '#208EF3' },
  { label: '青色', value: '#11B8BB' },
  { label: '绿色', value: '#00C44D' },
  { label: '黄色', value: '#FFD21A' },
  { label: '橙色', value: '#FF8A00' },
  { label: '红色', value: '#F5222D' },
  { label: '玫红', value: '#E92B87' },
  { label: '紫色', value: '#6900E9' },
  { label: '靛蓝', value: '#3146C5' },
  { label: '深灰', value: '#414141' },
  { label: '浅蓝', value: '#BCE0FC' },
  { label: '浅青', value: '#B6F1EF' },
  { label: '浅绿', value: '#B7EDCA' },
  { label: '浅黄', value: '#FFF4BB' },
  { label: '浅橙', value: '#FFE0B8' },
  { label: '浅红', value: '#FFC5C7' },
  { label: '浅粉', value: '#F8C8E4' },
  { label: '浅紫', value: '#D9BDF4' },
  { label: '灰蓝', value: '#C8CEF1' },
  { label: '浅灰', value: '#CCCCCC' },
] as const;
</script>

<template>
  <div class="flex flex-wrap items-center gap-3 p-1.5" role="group" aria-label="选择颜色">
    <button
      type="button"
      class="flex size-7 items-center justify-center rounded-full border border-border text-muted-foreground hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      :class="value ? '' : 'ring-2 ring-primary ring-offset-2 ring-offset-background'"
      :disabled="disabled"
      aria-label="不设置颜色"
      :aria-pressed="!value"
      title="不设置颜色"
      @click="emit('update:value', null)"
    >
      —
    </button>
    <button
      v-for="color in colors"
      :key="color.value"
      type="button"
      class="size-7 rounded-full border border-black/10 transition-transform hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      :class="value?.toLowerCase() === color.value.toLowerCase() ? 'ring-2 ring-primary ring-offset-2 ring-offset-background' : ''"
      :style="{ backgroundColor: color.value }"
      :disabled="disabled"
      :aria-label="`${color.label} ${color.value}`"
      :aria-pressed="value?.toLowerCase() === color.value.toLowerCase()"
      :title="`${color.label} ${color.value}`"
      @click="emit('update:value', color.value)"
    ></button>
  </div>
</template>
