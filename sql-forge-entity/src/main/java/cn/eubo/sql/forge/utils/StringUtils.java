package cn.eubo.sql.forge.utils;

import lombok.experimental.UtilityClass;

/**
 * 字符串工具类，提供驼峰与下划线命名之间的转换。
 */
@UtilityClass
public class StringUtils {

    /**
     * 将驼峰命名转换为下划线命名（如 userName → user_name）。
     *
     * @param camelCaseStr 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToUnderscore(String camelCaseStr) {
        if (camelCaseStr == null || camelCaseStr.isEmpty()) {
            return camelCaseStr;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCaseStr.length(); i++) {
            char ch = camelCaseStr.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 将首字母转为小写（如 UserName → userName）。
     *
     * @param input 输入字符串
     * @return 首字母小写的字符串
     */
    public String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}
