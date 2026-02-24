package cn.eubo.sql.forge.inter;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface SFunction<T, R> extends Serializable, Function<T, R> {
}
