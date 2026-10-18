-- Align the four system menu icons with the playground's second-level navigation.
-- Match the previous seed values so later user customizations are preserved.
UPDATE sys_menu SET icon = 'mdi:user'
WHERE menu_key = 'menu.system.user' AND icon = 'carbon:user';

UPDATE sys_menu SET icon = 'mdi:account-group'
WHERE menu_key = 'menu.system.role' AND icon = 'carbon:user-role';

UPDATE sys_menu SET icon = 'charm:organisation'
WHERE menu_key = 'menu.system.dept' AND icon = 'carbon:tree-view';

UPDATE sys_menu SET icon = 'mdi:menu'
WHERE menu_key = 'menu.system.menu' AND icon = 'carbon:menu';
