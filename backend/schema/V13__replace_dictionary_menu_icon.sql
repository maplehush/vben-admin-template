-- Replace the unavailable dictionary icon without overriding customized menu icons.
UPDATE sys_menu SET icon = 'lucide:book-open-text'
WHERE menu_key = 'menu.system.dict' AND icon = 'carbon:dictionary';
