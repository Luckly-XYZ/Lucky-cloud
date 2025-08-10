package com.xy.core.enums;

/**
 * 群成员角色枚举
 */
public enum IMemberStatus {

    NORMAL(0, "普通成员"),
    ADMIN(1, "管理员"),
    GROUP_OWNER(2, "群主"),
    MUTED(3, "禁言成员"),
    REMOVED(4, "已移除成员");

    private final Integer code;
    private final String desc;

    IMemberStatus(Integer status, String desc) {
        this.desc = desc;
        this.code = status;
    }

    public static IMemberStatus getByCode(Integer code) {
        for (IMemberStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }


    public String getDesc() {
        return desc;
    }

    public Integer getCode() {
        return code;
    }
}
