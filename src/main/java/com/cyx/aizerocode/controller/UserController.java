package com.cyx.aizerocode.controller;


import com.cyx.aizerocode.common.BaseResponse;
import com.cyx.aizerocode.common.ResultUtils;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.exception.ThrowUtils;
import com.cyx.aizerocode.model.dto.UserLoginRequest;
import com.cyx.aizerocode.model.dto.UserRegisterRequest;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.vo.LoginUserVO;
import com.cyx.aizerocode.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;


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
        boolean result = userService.userDelete(request);
        return ResultUtils.success(result);
    }



}
