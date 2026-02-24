package cn.wubo.sql.forge;

import cn.wubo.sql.forge.records.SqlScript;
import org.mvel2.MVEL;
import java.util.*;
import java.util.regex.*;

import static cn.wubo.sql.forge.constant.Constant.QUESTION_MARK;
import static org.springframework.core.log.LogFormatUtils.formatValue;

public class TemplateSqlEngine {
    private static final Pattern TAG_PATTERN = Pattern.compile("<(\\w+)\\s+([^>]*)>(.*?)</\\1>", Pattern.DOTALL);
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]*)\\}");

    public SqlScript process(String template, Map<String, Object> input) {
        return process(template, input, GenerationSqlMode.WITH_PLACEHOLDERS);
    }

    public SqlScript process(String template, Map<String, Object> input, GenerationSqlMode mode) {
        Context context = new Context(input,mode);
        processTemplate(template, context);
        return new SqlScript(context.getSql(), context.getParamMap());
    }

    private void processTemplate(String template, Context context) {
        Matcher matcher = TAG_PATTERN.matcher(template);
        int lastIndex = 0;

        while (matcher.find()) {
            // 处理标签前的文本
            String textBefore = template.substring(lastIndex, matcher.start());
            processText(textBefore, context);

            // 处理标签
            String tagName = matcher.group(1);
            String attrs = matcher.group(2);
            String body = matcher.group(3);

            switch (tagName) {
                case "if":
                    processIf(attrs, body, context);
                    break;
                case "foreach":
                    processForeach(attrs, body, context);
                    break;
                default:
                    context.appendSql(matcher.group(0)); // 未知标签原样输出
            }
            lastIndex = matcher.end();
        }

        // 处理剩余文本
        String textAfter = template.substring(lastIndex);
        processText(textAfter, context);
    }

    private void processText(String text, Context context) {
        Matcher matcher = PARAM_PATTERN.matcher(text);
        int lastIndex = 0;
        while (matcher.find()) {
            // 追加普通文本
            context.appendSql(text.substring(lastIndex, matcher.start()));

            // 处理 #{var} 表达式
            String varName = matcher.group(1).trim();
            Object value = context.getVariable(varName);
            if (value == null) {
                throw new IllegalArgumentException("Variable not found: " + varName);
            }

            // 根据模式选择处理方式
            if (context.getMode() == GenerationSqlMode.WITH_PLACEHOLDERS) {
                context.addParam(value);
                context.appendSql(QUESTION_MARK);
            } else {
                // 直接拼接值（需注意SQL注入风险）
                context.appendSql(formatValue(value));
            }

            lastIndex = matcher.end();
        }
        context.appendSql(text.substring(lastIndex)); // 追加剩余文本
    }

    private String formatValue(Object value) {
        if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            // 字符串需要加上引号并转义
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }

    private void processIf(String attrs, String body, Context context) {
        String testExpr = extractAttribute(attrs, "test");
        if (testExpr == null) throw new IllegalArgumentException("Missing 'test' attribute in <if>");

        // 评估表达式
        boolean condition = (Boolean) MVEL.eval(testExpr, context.getCurrentScope());
        if (condition) {
            processTemplate(body, context); // 递归处理子模板
        }
    }

    private void processForeach(String attrs, String body, Context context) {
        String collectionExpr = extractAttribute(attrs, "collection");
        String itemVar = extractAttribute(attrs, "item");
        String open = extractAttribute(attrs, "open");
        String close = extractAttribute(attrs, "close");
        String separator = extractAttribute(attrs, "separator");

        if (collectionExpr == null || itemVar == null)
            throw new IllegalArgumentException("Missing 'collection' or 'item' in <foreach>");

        // 获取集合并转为 Iterable
        Object collectionObj = MVEL.eval(collectionExpr, context.getCurrentScope());
        Iterable<?> items = toIterable(collectionObj);

        if (open != null) context.appendSql(open);
        int index = 0;

        for (Object item : items) {
            if (index > 0 && separator != null) context.appendSql(separator);

            // 进入新作用域，设置 item 变量
            context.enterScope();
            context.setVariable(itemVar, item);
            processTemplate(body, context); // 递归处理循环体
            context.exitScope();

            index++;
        }

        if (close != null) context.appendSql(close);
    }

    // 辅助方法：提取标签属性
    private String extractAttribute(String attrs, String name) {
        Pattern pattern = Pattern.compile(name + "\\s*=\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(attrs);
        return matcher.find() ? matcher.group(1) : null;
    }

    // 辅助方法：对象转 Iterable
    private Iterable<?> toIterable(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof Iterable) return (Iterable<?>) obj;
        if (obj.getClass().isArray()) return Arrays.asList((Object[]) obj);
        throw new IllegalArgumentException("Unsupported collection type: " + obj.getClass());
    }
}
