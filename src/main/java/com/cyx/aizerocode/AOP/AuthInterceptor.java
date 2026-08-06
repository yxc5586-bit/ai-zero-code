package com.cyx.aizerocode.AOP;


import com.cyx.aizerocode.annotation.AuthCheck;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.enums.UserRoleEnum;
import com.cyx.aizerocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect
public class AuthInterceptor {

    @Resource
    UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取当前request
        String mustRole = authCheck.mustRole();
        ServletRequestAttributes currentUser = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = currentUser.getRequest();

        //根据request获取当前用户,以及权限要求等级
        User user = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 1. 不需要权限
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 2. 当需要权限时，先获取用户的权限等级
        UserRoleEnum enumWithUser = UserRoleEnum.getEnumByValue(user.getUserRole());

        // 校验权限是否存在
        if (enumWithUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 3. 管理员接口仅允许管理员访问
        if (!UserRoleEnum.ADMIN.equals(enumWithUser) && UserRoleEnum.ADMIN.equals(mustRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();

    }

}
