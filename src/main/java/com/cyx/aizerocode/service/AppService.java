package com.cyx.aizerocode.service;

import com.cyx.aizerocode.model.dto.app.AppQueryRequest;
import com.cyx.aizerocode.model.entity.App;
import com.cyx.aizerocode.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 应用 服务层
 *
 * @author 25038
 * @since 2026-08-02
 */
public interface AppService extends IService<App> {

    /**
     * 构造查询条件
     *
     * @param appQueryRequest 应用查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);



    /**
     * 获取应用视图
     *
     * @param app 应用
     * @return 应用视图
     */
    AppVO getAppVO(App app);

    /**
     * 批量获取应用视图
     *
     * @param appList 应用列表
     * @return 应用视图列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据 prompt 生成默认应用名称
     *
     * @param initPrompt 应用初始化 prompt
     * @return 默认应用名称
     */
    String getDefaultAppName(String initPrompt);
}
