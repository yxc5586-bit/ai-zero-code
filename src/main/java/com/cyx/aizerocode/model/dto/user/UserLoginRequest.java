package com.cyx.aizerocode.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author 25038
 * @since 2026-07-02
 */
@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
