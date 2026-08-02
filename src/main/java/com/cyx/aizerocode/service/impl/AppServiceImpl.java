package com.cyx.aizerocode.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.cyx.aizerocode.model.entity.App;
import com.cyx.aizerocode.mapper.AppMapper;
import com.cyx.aizerocode.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author 25038
 * @since 2026-08-02
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
