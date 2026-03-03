package cn.eubo.sql.forge.utils;

import cn.eubo.sql.forge.inter.SFunction;
import lombok.experimental.UtilityClass;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@UtilityClass
public class LambdaUtils {

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
