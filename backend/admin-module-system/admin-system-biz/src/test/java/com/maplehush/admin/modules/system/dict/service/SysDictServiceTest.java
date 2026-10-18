package com.maplehush.admin.modules.system.dict.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dict.dto.DictDataDTO;
import com.maplehush.admin.modules.system.dict.dto.DictQueryDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemDataDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemQueryDTO;
import com.maplehush.admin.modules.system.dict.mapper.SysDictItemMapper;
import com.maplehush.admin.modules.system.dict.mapper.SysDictMapper;
import com.maplehush.admin.modules.system.dict.service.impl.SysDictServiceImpl;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class SysDictServiceTest {
    private ISysDictService service;
    private JdbcTemplate jdbc;
    private DriverManagerDataSource source;
    private SysDictMapper dictMapper;
    private SysDictItemMapper itemMapper;

    @BeforeEach
    void setUp() throws Exception {
        source = new DriverManagerDataSource("jdbc:h2:mem:dict_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "CREATE TABLE sys_dict (id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_code VARCHAR(64) UNIQUE, dict_name VARCHAR(128), status TINYINT, built_in TINYINT, remark VARCHAR(500), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute(
                "CREATE TABLE sys_dict_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_id BIGINT, item_value VARCHAR(128), item_label VARCHAR(128), sort_order INT, status TINYINT, item_color VARCHAR(16), remark VARCHAR(500), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT uk_value UNIQUE(dict_id,item_value))");
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        jdbc.update("INSERT INTO sys_dict (dict_code,dict_name,status,built_in) VALUES ('common_status','启禁用状态',1,1)");
        jdbc.update(
                "INSERT INTO sys_dict_item (dict_id,item_value,item_label,sort_order,status) VALUES (1,'ENABLED','已启用',10,1),(1,'DISABLED','已禁用',20,1)");
        var mapper = TestMapperFactory.create(source);
        mapper.getConfiguration().addMapper(SysDictMapper.class);
        mapper.getConfiguration().addMapper(SysDictItemMapper.class);
        dictMapper = mapper.getMapper(SysDictMapper.class);
        itemMapper = mapper.getMapper(SysDictItemMapper.class);
        service = transactionalService(new OperationAuditServiceImpl(mapper.getMapper(OperationAuditMapper.class)));
    }

    @Test
    void builtInStatusIsReadOnlyAndOptionsFollowEnabledFlag() {
        assertEquals(2, service.options("common_status").size());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> service.update(1, 1, new DictDataDTO("common_status", "更改", "ENABLED", null)))
                        .code());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> service.deleteItem(1, 1, 1))
                        .code());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log", Integer.class));
        long id = service.create(1, new DictDataDTO("priority", "优先级", "ENABLED", "旧备注"));
        service.createItem(1, id, new ItemDataDTO("high", "高", 1, "ENABLED", null, null));
        service.createItem(1, id, new ItemDataDTO("low", "低", 2, "DISABLED", null, null));
        assertEquals(
                2, service.items(1, id, new ItemQueryDTO(1, 20, null, null)).total());
        assertEquals(1, service.options("priority").size());
        service.update(1, id, new DictDataDTO("priority", "优先级", "DISABLED", null));
        assertEquals(null, jdbc.queryForObject("SELECT remark FROM sys_dict WHERE id = ?", String.class, id));
        assertEquals(
                40400,
                assertThrows(ApiException.class, () -> service.options("priority"))
                        .code());
    }

    @Test
    void duplicateCodesAndValuesAreRejectedByDatabase() {
        service.create(1, new DictDataDTO("priority", "优先级", "ENABLED", null));
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> service.create(1, new DictDataDTO("priority", "重复", "ENABLED", null)))
                        .code());
        long id = jdbc.queryForObject("SELECT id FROM sys_dict WHERE dict_code = 'priority'", Long.class);
        service.createItem(1, id, new ItemDataDTO("high", "高", 1, "ENABLED", null, null));
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> service.createItem(
                                        1, id, new ItemDataDTO("high", "重复", 2, "ENABLED", null, null)))
                        .code());
    }

    @Test
    void itemColorIsOptionalAndCanBeCleared() {
        long dictId = service.create(1, new DictDataDTO("priority", "优先级", "ENABLED", null));
        long itemId = service.createItem(1, dictId, new ItemDataDTO("high", "高", 1, "ENABLED", null, "error"));
        assertEquals(
                "error",
                service.items(1, dictId, new ItemQueryDTO(1, 20, null, null))
                        .items()
                        .getFirst()
                        .itemColor());
        assertEquals("error", service.options("priority").getFirst().itemColor());

        service.updateItem(1, dictId, itemId, new ItemDataDTO("high", "高", 1, "ENABLED", null, null));
        assertEquals(
                null, jdbc.queryForObject("SELECT item_color FROM sys_dict_item WHERE id = ?", String.class, itemId));
        assertEquals(
                null,
                service.items(1, dictId, new ItemQueryDTO(1, 20, null, null))
                        .items()
                        .getFirst()
                        .itemColor());
        assertEquals(null, service.options("priority").getFirst().itemColor());
    }

    @Test
    void itemColorAcceptsHexButRejectsUnsafeValues() {
        long dictId = service.create(1, new DictDataDTO("priority", "优先级", "ENABLED", null));
        long itemId = service.createItem(1, dictId, new ItemDataDTO("high", "高", 1, "ENABLED", null, "#AABBCC"));
        assertEquals(
                "#AABBCC",
                service.items(1, dictId, new ItemQueryDTO(1, 20, null, null))
                        .items()
                        .getFirst()
                        .itemColor());
        assertEquals("#AABBCC", service.options("priority").getFirst().itemColor());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> service.updateItem(
                                        1,
                                        dictId,
                                        itemId,
                                        new ItemDataDTO("high", "高", 1, "ENABLED", null, "url(javascript:alert(1))")))
                        .code());
    }

    @Test
    void dictionaryPagesApplyFiltersBeforeCountingAndLimitRows() {
        service.create(1, new DictDataDTO("order_type", "订单类型", "ENABLED", null));
        service.create(1, new DictDataDTO("order_status", "订单状态", "ENABLED", null));
        service.create(1, new DictDataDTO("other_status", "其他状态", "DISABLED", null));

        var first = service.list(1, new DictQueryDTO(1, 1, "order", null, "ENABLED"));
        var second = service.list(1, new DictQueryDTO(2, 1, "order", null, "ENABLED"));
        assertEquals(2, first.total());
        assertEquals(1, first.items().size());
        assertEquals("order_type", first.items().getFirst().dictCode());
        assertEquals("order_status", second.items().getFirst().dictCode());

        var named = service.list(1, new DictQueryDTO(1, 20, null, "订单状态", null));
        assertEquals(1, named.total());
        assertEquals("order_status", named.items().getFirst().dictCode());
        var beyondLastPage = service.list(1, new DictQueryDTO(3, 1, "order", null, null));
        assertEquals(2, beyondLastPage.total());
        assertEquals(0, beyondLastPage.items().size());
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> service.list(1, new DictQueryDTO(0, 20, null, null, null)))
                        .code());
    }

    @Test
    void dictionaryItemPagesApplyFiltersBeforeCountingAndLimitRows() {
        long dictId = service.create(1, new DictDataDTO("priority", "优先级", "ENABLED", null));
        service.createItem(1, dictId, new ItemDataDTO("high", "高优先级", 10, "ENABLED", null, null));
        service.createItem(1, dictId, new ItemDataDTO("medium", "中优先级", 20, "ENABLED", null, null));
        service.createItem(1, dictId, new ItemDataDTO("low", "低优先级", 30, "DISABLED", null, null));

        var first = service.items(1, dictId, new ItemQueryDTO(1, 1, "优先级", "ENABLED"));
        var second = service.items(1, dictId, new ItemQueryDTO(2, 1, "优先级", "ENABLED"));
        assertEquals(2, first.total());
        assertEquals("high", first.items().getFirst().itemValue());
        assertEquals("medium", second.items().getFirst().itemValue());
        assertEquals(
                1,
                service.items(1, dictId, new ItemQueryDTO(1, 20, null, "DISABLED"))
                        .total());
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> service.items(1, dictId, new ItemQueryDTO(1, 101, null, null)))
                        .code());
    }

    @Test
    void writesAuditRecordsForDictionaryAndItemMutations() {
        long dictId = service.create(7, new DictDataDTO("priority", "优先级", "ENABLED", null));
        service.update(7, dictId, new DictDataDTO("priority", "优先级更新", "ENABLED", null));
        long itemId = service.createItem(7, dictId, new ItemDataDTO("high", "高", 1, "ENABLED", null, null));
        service.updateItem(7, dictId, itemId, new ItemDataDTO("high", "最高", 1, "ENABLED", null, null));
        service.deleteItem(7, dictId, itemId);
        service.delete(7, dictId);

        var records = jdbc.query(
                "SELECT action_code, target_type, target_id FROM sys_operation_log ORDER BY id",
                (rs, row) -> rs.getString(1) + ":" + rs.getString(2) + ":" + rs.getString(3));
        assertEquals(
                java.util.List.of(
                        "system:dict:create:DICT:" + dictId,
                        "system:dict:update:DICT:" + dictId,
                        "system:dict-item:create:DICT_ITEM:" + itemId,
                        "system:dict-item:update:DICT_ITEM:" + itemId,
                        "system:dict-item:delete:DICT_ITEM:" + itemId,
                        "system:dict:delete:DICT:" + dictId),
                records);
        assertEquals(
                6,
                jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE actor_user_id = 7", Integer.class));
    }

    @Test
    void auditFailureRollsBackDictionaryAndItemWrites() {
        IOperationAuditService failingAudit = (actor, action, targetType, targetId) -> {
            throw new IllegalStateException("audit unavailable");
        };
        var failingService = transactionalService(failingAudit);
        assertThrows(
                IllegalStateException.class,
                () -> failingService.create(7, new DictDataDTO("priority", "优先级", "ENABLED", null)));
        assertEquals(
                0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_dict WHERE dict_code = 'priority'", Integer.class));

        long dictId = service.create(7, new DictDataDTO("priority", "优先级", "ENABLED", null));
        long itemId = service.createItem(7, dictId, new ItemDataDTO("high", "高", 1, "ENABLED", null, null));
        assertThrows(IllegalStateException.class, () -> failingService.deleteItem(7, dictId, itemId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sys_dict_item WHERE id = ?", Integer.class, itemId));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log", Integer.class));
    }

    private ISysDictService transactionalService(IOperationAuditService audit) {
        var permissions = mock(IManagementPermissionService.class);
        doNothing().when(permissions).require(anyLong(), anyString());
        var target = new SysDictServiceImpl(dictMapper, itemMapper, permissions, audit);
        var factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        return (ISysDictService) factory.getProxy();
    }
}
