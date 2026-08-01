package com.cyx.aizerocode.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Description("html代码结果")
@Data
public class HtmlCodeResult {

    @Description("html代码")
    private String htmlCode;

    @Description("描述")
    private String description;
}
