package com.cyx.aizerocode.common;

import lombok.Data;

@Data
public class PageRequest {

    /**
     * 页码
     */
    private  int pageNum = 1;
    /**
     * 页大小
     */
    private  int pageSize = 10;
    /**
     * 排序字段
     */
    private  String sortField;
    /**
     * 排序方式
     */
    private String sortOrder = "descend";


}
