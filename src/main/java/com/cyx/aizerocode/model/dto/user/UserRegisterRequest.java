package com.cyx.aizerocode.model.dto.user;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求
 *
 *
 */
@Data

public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    @Column("userAccount")
    private String userAccount;

    /**
     * 密码
     */
    @Column("userPassword")
    private String userPassword;


    /**
     * 重复校验密码
     */
    @Column("checkPassword")
    private String checkPassword;

}
