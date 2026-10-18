import type { MenuNode } from '#/api/system';

export function enabledMenuIds(nodes: MenuNode[]): string[] {
  const ids: string[] = [];
  const visit = (items: MenuNode[]) => {
    for (const item of items) {
      if (item.status !== 'ENABLED') continue;
      ids.push(item.id);
      visit(item.children ?? []);
    }
  };
  visit(nodes);
  return ids;
}

export function sameMenuIds(expected: string[], actual: string[]): boolean {
  return expected.length === actual.length &&
    expected.every((id) => actual.includes(id));
}
