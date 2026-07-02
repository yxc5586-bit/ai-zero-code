package com.cyx.aizerocode.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.constant.UserConstant;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.mapper.UserMapper;
import com.cyx.aizerocode.model.dto.user.UserQueryRequest;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.enums.UserRoleEnum;
import com.cyx.aizerocode.model.vo.LoginUserVO;
import com.cyx.aizerocode.model.vo.UserVO;
import com.cyx.aizerocode.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 服务层实现。
 *
 * @author 25038
 * @since 2026-07-02
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    @Resource
    UserMapper userMapper;



    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {

        // 1、校验参数
        if (StrUtil.hasBlank(userAccount , userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2、判断是否已经存在
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("userAccount", userAccount);
        if (userMapper.selectCountByQuery(queryWrapper) > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 3、加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 4、插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("游客");
        user.setUserRole(UserRoleEnum.USER.getValue());

        if (!this.save(user)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }


    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1、校验参数
        if (StrUtil.hasBlank(userAccount , userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户错误");
        }
        if (userPassword.length() < 8 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }

        // 2、加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 3、查询用户是否存在
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOneByQuery(queryWrapper);

        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或账号密码不匹配");
        }

        // 4、记录用户登录态（若用户存在）
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //返回用户信息
        return user;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1、获取当前登录用户，校验用户登录态
        Object LoginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) LoginUser;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2、从数据库中获取当前登录的用户（上述结果已经可以返回，但可能因缓存问题导致数据错误）
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return currentUser;
    }

    @Override
    public Boolean userLogout(HttpServletRequest request) {
        // 1、获取当前登录用户，校验用户登录态
        Object LoginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) LoginUser;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2、直接逻辑登录态删除并返回boolean值判断是否删除完成
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));

    }

    @Override
    public String getEncryptPassword(String userPassword){
        final String SALT = "ccc";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }


    @Override
    public LoginUserVO userVO(User user) {
        if (user == null){
           return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }


    @Override
    public UserVO getUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }
}
