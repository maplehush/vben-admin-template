import type { Status } from '#/api/system';

/** 与后端 CommonStatus 的 API 值保持一致。 */
export const statusFormOptions: { label: string; value: Status }[] = [
  { label: '已启用', value: 'ENABLED' },
  { label: '已禁用', value: 'DISABLED' },
];

export const statusFilterOptions: { label: string; value: Status }[] = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
];
