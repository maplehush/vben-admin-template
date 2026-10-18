import { createSSRApp, h } from 'vue';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';

import { describe, expect, it } from 'vitest';
import { renderToString } from 'vue/server-renderer';

import { generateRoutesByBackend } from '../generate-routes-backend';

describe('generateRoutesByBackend', () => {
  it('renders a page menu at its own path and nested content below it', async () => {
    const routes = await generateRoutesByBackend({
      fetchMenuListAsync: async () => [
        {
          name: 'ParentPage',
          path: '/parent',
          component: '/parent/index',
          meta: { title: 'Parent page' },
          children: [
            {
              name: 'ChildPage',
              path: '/parent/child',
              component: '/child/index',
              meta: { title: 'Child page' },
            },
            {
              name: 'NestedDirectory',
              path: '/parent/nested',
              meta: { title: 'Nested directory' },
              children: [
                {
                  name: 'NestedPage',
                  path: '/parent/nested/page',
                  component: '/child/index',
                  meta: { title: 'Nested page' },
                },
              ],
            },
          ],
        },
      ],
      layoutMap: {},
      pageMap: {
        '/parent/index.vue': async () => ({
          render: () => h('div', 'Parent page content'),
        }),
        '/child/index.vue': async () => ({
          render: () => h('div', 'Child page content'),
        }),
      },
    });
    const router = createRouter({ history: createMemoryHistory(), routes });
    const renderAt = async (path: string) => {
      await router.push(path);
      await router.isReady();
      const app = createSSRApp({ render: () => h(RouterView) });
      app.use(router);
      return renderToString(app);
    };

    expect(await renderAt('/parent')).toContain('Parent page content');
    const childHtml = await renderAt('/parent/child');
    expect(childHtml).toContain('Child page content');
    expect(childHtml).not.toContain('Parent page content');
    expect(await renderAt('/parent/nested/page')).toContain('Child page content');
  });
});
