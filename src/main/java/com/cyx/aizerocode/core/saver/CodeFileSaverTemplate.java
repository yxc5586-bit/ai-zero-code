package com.cyx.aizerocode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码保存模板类
 * @param <T> 输入的类型
 */
public abstract class CodeFileSaverTemplate<T> {

    //定义文件保存的根目录
    protected static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 模板方法，保存代码的流程
     * @param result 代码结果
     * @return 保存的文件目录
     */
    public final File saveCode(T result){
        //验证输入、定义文件保存的根目录
        validateInput(result);
        //2、构建文件保存的唯一路径
        String dirPath = buildUniqueDir();
        //3、保存文件
        saveFiles(result, dirPath);
        //4、返回保存的文件
        return new File(dirPath);
    }

    /**
     * 验证输入
     * @param result 输入
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"代码结果对象不能为空");
        }
    }


    /**
     * 根据类型构建唯一路径
     * @return 文件路径
     */
    protected final String buildUniqueDir(){
        String codeType = getCodeType().getValue();
        String uniqueDir = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDir;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存代码文件
     *
     * @param result  代码结果
     * @param dirPath 文件保存的基础路径
     */
    protected abstract void saveFiles(T result, String dirPath);

    /**
     * 获取输入的类型
     *
     * @return 输入的类型
     */
    protected abstract CodeGenTypeEnum getCodeType();


    //保存单个文件的方法
    /**
     *
     * @param dirPath 当前文件保存的目录
     * @param fileName 文件名
     * @param content 文件内容
     */
    protected static void writeToFile(String dirPath, String fileName, String content){
        String FilePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, FilePath, StandardCharsets.UTF_8);
    }

}
