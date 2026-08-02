package com.cyx.aizerocode.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.ai.model.HtmlCodeResult;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;


import java.io.File;
import java.nio.charset.StandardCharsets;

@Deprecated
public class CodeFileSaver {

    //1、定义文件保存的根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    //2、构建文件保存的唯一路径

    /**
     * 根据类型构建唯一路径
     * @param bizType 生成的文件类型（单HTML或多文件）
     * @return 文件路径
     */
    private static String buildUniqueDir(String bizType){
        String uniqueDir = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDir;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    //3、保存HTML网页代码
    public static File saveHtmlCodeResult(HtmlCodeResult result){
        String dirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        return new File(dirPath);
    }

    //4、保存多文件代码
    public static File saveMultiFileCodeResult(MultiFileCodeResult result){
        String dirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
        return new File(dirPath);
    }


    //保存单个文件的方法
    /**
     *
     * @param dirPath 当前文件保存的目录
     * @param fileName 文件名
     * @param content 文件内容
     */
    private static void writeToFile(String dirPath, String fileName, String content){
        String FilePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, FilePath, StandardCharsets.UTF_8);
    }

}
