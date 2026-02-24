package cn.eubo.sql.forge.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

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

    public String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}
