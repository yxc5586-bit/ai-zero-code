package com.cyx.aizerocode.controller;


import com.cyx.aizerocode.annotation.AuthCheck;
import com.cyx.aizerocode.common.BaseResponse;
import com.cyx.aizerocode.common.DeleteRequest;
import com.cyx.aizerocode.common.ResultUtils;
import com.cyx.aizerocode.constant.UserConstant;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.exception.ThrowUtils;
import com.cyx.aizerocode.model.dto.user.*;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.vo.LoginUserVO;
import com.cyx.aizerocode.model.vo.UserVO;
import com.cyx.aizerocode.service.UserService;

import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户 控制层。
 *
 * @author ccc
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;



    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果用户ID
     */
    @PostMapping("/Register")
    public BaseResponse <Long> UserRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        Long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return  ResultUtils.success(userId);
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @param request 请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User loginUser = userService.userLogin(userAccount, userPassword, request);
        LoginUserVO loginUserVO = userService.userVO(loginUser);
        System.out.println(loginUserVO);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User user = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.userVO(user);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     * @param request 请求
     * @return 注销结果
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     * @param userAddRequest 创建用户请求
     * @return 创建结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/add")
    public BaseResponse<Boolean> createUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);

    }

    /**
     * 根据ID删除用户
     * @param deleteRequest 删除用户请求
     * @return 删除结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);

    }


    /**
     * 根据ID更新用户
     * @param userUpdateRequest 更新用户请求
     * @return 更新结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 根据 id 获取用户信息
     * @param id 查询用户ID
     * @return 查询结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<User> getUserById(Long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }


    /**
     * 根据 id 获取脱敏用户信息
     * @param id 获取用户ID
     * @return 获取结果
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表
     * @param userQueryRequest 查询用户列表请求
     * @return 查询结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUser(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of( pageNum,  pageSize),
                userService.getQueryWrapper(userQueryRequest));

        //数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
