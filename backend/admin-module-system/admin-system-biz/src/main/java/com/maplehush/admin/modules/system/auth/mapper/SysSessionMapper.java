package com.maplehush.admin.modules.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.auth.entity.SysSession;
import com.maplehush.admin.modules.system.auth.vo.SessionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话持久化；联表列表和原子撤销在对应 XML 中实现。 */
@Mapper
public interface SysSessionMapper extends BaseMapper<SysSession> {
    List<SessionVO> listOwn(
            @Param("userId") long userId,
            @Param("now") LocalDateTime now,
            @Param("currentSessionId") long currentSessionId);

    long countActive(@Param("now") LocalDateTime now, @Param("username") String username);

    List<SessionVO> listActive(
            @Param("now") LocalDateTime now,
            @Param("username") String username,
            @Param("limit") int limit,
            @Param("offset") int offset);

    int revokeOwn(@Param("userId") long userId, @Param("sessionId") long sessionId, @Param("now") LocalDateTime now);

    int revokeManaged(
            @Param("sessionId") long sessionId,
            @Param("now") LocalDateTime now,
            @Param("protectSuperAdmin") boolean protectSuperAdmin);

    boolean isProtectedTarget(@Param("sessionId") long sessionId);

    int revokeAllForUser(@Param("userId") long userId, @Param("now") LocalDateTime now);

    int revokeByHash(@Param("hash") String hash, @Param("now") LocalDateTime now);

    RefreshSessionState lockByRefreshHash(@Param("hash") String hash);
}
