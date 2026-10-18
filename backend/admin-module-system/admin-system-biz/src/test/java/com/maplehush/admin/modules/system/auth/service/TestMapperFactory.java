package com.maplehush.admin.modules.system.auth.service;

import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.maplehush.admin.modules.system.common.config.MybatisPlusConfiguration;
import javax.sql.DataSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** 让单元测试使用与应用相同的 MyBatis-Plus 映射配置。 */
public final class TestMapperFactory {
    private TestMapperFactory() {}

    public static SqlSessionTemplate create(DataSource source) throws Exception {
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setPlugins(new MybatisPlusConfiguration().mybatisPlusInterceptor());
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/system/*.xml"));
        factory.afterPropertiesSet();
        return new SqlSessionTemplate(factory.getObject());
    }
}
