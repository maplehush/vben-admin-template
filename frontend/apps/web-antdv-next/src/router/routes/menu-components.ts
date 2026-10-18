const VIEW_PREFIX = '../../views/';

/** 菜单只能指向已打包的业务页面，不能选择登录页或页面内部组件。 */
function toMenuComponentOptions(paths: string[]) {
  return paths
    .filter((path) => path.startsWith(VIEW_PREFIX) && path.endsWith('.vue'))
    .map((path) => path.slice(VIEW_PREFIX.length, -'.vue'.length))
    .filter((path) =>
      path
        .split('/')
        .every((part) => !['_core', 'components', 'modules'].includes(part)),
    )
    .map((path) => {
      const value = `/${path}`;
      return { label: value, value };
    })
    .toSorted((left, right) => left.value.localeCompare(right.value));
}

const menuComponentOptions = toMenuComponentOptions(
  Object.keys(import.meta.glob('../../views/**/*.vue')),
);

export { menuComponentOptions, toMenuComponentOptions };
