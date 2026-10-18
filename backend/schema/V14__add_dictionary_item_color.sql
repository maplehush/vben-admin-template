-- 字典项展示颜色可为空；仅使用前端支持的语义色。
ALTER TABLE sys_dict_item
    ADD COLUMN item_color VARCHAR(16) NULL AFTER status;

UPDATE sys_dict_item i
JOIN sys_dict d ON d.id = i.dict_id
SET i.item_color = CASE i.item_value
    WHEN 'ENABLED' THEN 'success'
    WHEN 'DISABLED' THEN 'error'
END
WHERE d.dict_code = 'common_status'
  AND i.item_value IN ('ENABLED', 'DISABLED');
