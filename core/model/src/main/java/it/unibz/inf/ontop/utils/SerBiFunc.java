package it.unibz.inf.ontop.utils;

import java.io.Serializable;
import java.util.function.BiFunction;

public interface SerBiFunc<T,U, R> extends BiFunction<T,U, R>, Serializable {}