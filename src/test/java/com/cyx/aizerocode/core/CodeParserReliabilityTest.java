package com.cyx.aizerocode.core;

import com.cyx.aizerocode.ai.model.HtmlCodeResult;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.core.parser.CodeParserExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不依赖 Spring、Redis 或真实大模型的解析器回归测试。
 *
 * <p>测试通过循环生成 200 组不同内容，验证 HTML 与多文件解析链路能够稳定提取代码块。
 * 这个数字可以作为工程质量证据，但不能等同于真实大模型生成成功率。</p>
 */
class CodeParserReliabilityTest {

    private static final int CASES_PER_MODE = 100;

    @Test
    void shouldParseOneHundredHtmlCodeBlocks() {
        for (int index = 0; index < CASES_PER_MODE; index++) {
            String expected = "<main data-case=\"" + index + "\">case-" + index + "</main>";
            String language = index % 2 == 0 ? "html" : "HTML";
            String response = "说明文本\n```" + language + "  \n" + expected + "\n```\n结束文本";

            HtmlCodeResult result = (HtmlCodeResult) CodeParserExecutor.parseCode(
                    response,
                    CodeGenTypeEnum.HTML
            );

            assertEquals(expected, result.getHtmlCode(), "HTML case " + index + " failed");
        }
    }

    @Test
    void shouldParseOneHundredMultiFileResponses() {
        for (int index = 0; index < CASES_PER_MODE; index++) {
            String html = "<div id=\"case-" + index + "\"></div>";
            String css = "#case-" + index + " { color: rgb(" + index + ", 0, 0); }";
            String js = "document.querySelector('#case-" + index + "').dataset.ready = 'true';";
            String jsLanguage = index % 2 == 0 ? "js" : "javascript";
            String response = """
                    ```html
                    %s
                    ```
                    ```css
                    %s
                    ```
                    ```%s
                    %s
                    ```
                    """.formatted(html, css, jsLanguage, js);

            MultiFileCodeResult result = (MultiFileCodeResult) CodeParserExecutor.parseCode(
                    response,
                    CodeGenTypeEnum.MULTI_FILE
            );

            assertEquals(html, result.getHtmlCode(), "Multi-file HTML case " + index + " failed");
            assertEquals(css, result.getCssCode(), "Multi-file CSS case " + index + " failed");
            assertEquals(js, result.getJsCode(), "Multi-file JS case " + index + " failed");
        }
    }

    @Test
    void shouldFallbackToRawHtmlWhenCodeFenceIsMissing() {
        String rawHtml = "<html><body>raw fallback</body></html>";

        HtmlCodeResult result = (HtmlCodeResult) CodeParserExecutor.parseCode(
                rawHtml,
                CodeGenTypeEnum.HTML
        );

        assertTrue(result.getHtmlCode().contains("raw fallback"));
    }
}
