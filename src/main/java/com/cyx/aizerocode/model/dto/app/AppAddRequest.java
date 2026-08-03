package com.cyx.aizerocode.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用创建请求
 *
 * @author 25038
 * @since 2026-08-02
 */
@Data
public class AppAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用初始化 prompt
     */
    private String initPrompt;
}
