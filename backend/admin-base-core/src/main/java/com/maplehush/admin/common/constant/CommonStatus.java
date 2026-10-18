package com.maplehush.admin.common.constant;

/** 用户、角色、部门、菜单及接口权限共用的启禁用状态 */
public enum CommonStatus {
    /** 启用，数据库存储 1 */
    ENABLED(1),
    /** 禁用，数据库存储 0 */
    DISABLED(0);

    private final int databaseValue;

    CommonStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return name();
    }

    public static CommonStatus fromApi(String value) {
        for (CommonStatus status : values()) {
            if (status.apiValue().equals(value)) return status;
        }
        throw new IllegalArgumentException("未知启禁用状态");
    }

    public static CommonStatus fromDatabase(int value) {
        for (CommonStatus status : values()) {
            if (status.databaseValue == value) return status;
        }
        throw new IllegalArgumentException("未知启禁用状态值");
    }
}
