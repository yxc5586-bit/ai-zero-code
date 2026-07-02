package com.cyx.aizerocode.service;


import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.vo.LoginUserVO;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户 服务层。
 *
 * @author 25038
 * @since 2026-07-02
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登录
     *
     * @return 脱敏后的用户
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest  request);


    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest  request);

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 是否注销成功
     */
    Boolean userDelete(HttpServletRequest request);



    /**
     * 获取加密密码
     *
     * @param userPassword 密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的登录用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的用户信息
     */
    LoginUserVO userVO(User user);
}
