package com.cyx.aizerocode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.ai.AiCodeGenTypeRoutingService;
import com.cyx.aizerocode.ai.AiCodeGenTypeRoutingServiceFactory;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.constant.AppConstant;
import com.cyx.aizerocode.core.AiCodeGeneratorFacade;
import com.cyx.aizerocode.core.builder.VueProjectBuilder;
import com.cyx.aizerocode.core.handler.StreamHandlerExecutor;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.exception.ThrowUtils;
import com.cyx.aizerocode.mapper.AppMapper;
import com.cyx.aizerocode.model.dto.app.AppAddRequest;
import com.cyx.aizerocode.model.dto.app.AppQueryRequest;
import com.cyx.aizerocode.model.entity.App;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.cyx.aizerocode.model.vo.AppVO;
import com.cyx.aizerocode.model.vo.UserVO;
import com.cyx.aizerocode.monitor.MonitorContext;
import com.cyx.aizerocode.monitor.MonitorContextHolder;
import com.cyx.aizerocode.service.AppService;
import com.cyx.aizerocode.service.ChatHistoryService;
import com.cyx.aizerocode.service.ScreenshotService;
import com.cyx.aizerocode.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现
 *
 * @author 25038
 * @since 2026-08-02
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    /**
     * 默认应用名称长度
     */
    private static final int DEFAULT_APP_NAME_LENGTH = 12;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    protected ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .eq("priority", priority)
                .eq("userId", userId)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .like("codeGenType", codeGenType)
                .like("deployKey", deployKey);
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }
        return queryWrapper;
    }



    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);

        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            appVO.setCreateUser(userService.getUserVO(user));
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setCreateUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }


    @Override
    public String getDefaultAppName(String initPrompt) {
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用初始化 prompt 不能为空");
        }
        String appName = initPrompt.trim();
        if (appName.length() <= DEFAULT_APP_NAME_LENGTH) {
            return appName;
        }
        return appName.substring(0, DEFAULT_APP_NAME_LENGTH);
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User LoginUser) {

        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR);

        //查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 权限校验
        if(!app.getUserId().equals(LoginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无访问权限");
        }

        // 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum enumByValue = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");

        // 保存消息到数据库
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), app.getUserId());
        // 6. 设置监控上下文
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(LoginUser.getId().toString())
                        .appId(appId.toString())
                        .build()
        );
        //调用门面类获取 AI 输出结果
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, enumByValue, appId);

        //收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(contentFlux, chatHistoryService, appId, LoginUser, enumByValue)
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public String deployApp(Long appId, User LoginUser) {

        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(LoginUser == null || LoginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 权限校验
        if(!app.getUserId().equals(LoginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无访问权限");
        }
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)){
            deployKey = RandomUtil.randomString(6);
        }

        // 获取代码类型和生成路径
        String codeGenTypeEnum = app.getCodeGenType();
        String sourceDirName = codeGenTypeEnum + "_" + app.getId();
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }

        // 如果文件类型是Vue，则需要特殊处理，需要先安装打包后再复制

        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(codeGenTypeEnum);
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }

        //  复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        //  更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        //  构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        //  异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }


    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 校验非空
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "应用初始化 prompt 不能为空");
        // 构建app对象并设置应用信息
        App app = new App();
        app.setInitPrompt(initPrompt);
        app.setAppName(this.getDefaultAppName(initPrompt));
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        app.setUserId(loginUser.getId());
        //使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = routingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入到数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建应用失败");
        return app.getId();
    }

    /**
     * 删除应用
     * @param id 应用id
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(Serializable id){
        //校验
        ThrowUtils.throwIf(id == null , ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    };
}
