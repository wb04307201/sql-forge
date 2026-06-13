package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.inter.SFunction;
import lombok.experimental.UtilityClass;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Lambda 表达式工具类，从可序列化的 Lambda 方法引用中提取 Java 字段名。
 */
@UtilityClass
public class LambdaUtils {

    /**
     * 从 Lambda 方法引用中提取字段名（去除 get/is 前缀并转为 camelCase）。
     *
     * @param fn  可序列化的 Lambda 引用
     * @param <T> 实体类型
     * @return Java 字段名
     * @throws NoSuchMethodException     未找到 writeReplace 方法
     * @throws InvocationTargetException 反射调用异常
     * @throws IllegalAccessException    访问权限异常
     */
    public <T> String getFieldName(SFunction<T, ?> fn) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method writeReplace = fn.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        Object replacement = writeReplace.invoke(fn);
        if (replacement instanceof SerializedLambda serializedLambda) {
            String implMethodName = serializedLambda.getImplMethodName();
            if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
                return StringUtils.toCamelCase(implMethodName.substring(3));
            } else if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
                return StringUtils.toCamelCase(implMethodName.substring(2));
            } else {
                return implMethodName;
            }
        }else{
            throw new IllegalStateException("Not a serializable lambda");
        }
    }
}
