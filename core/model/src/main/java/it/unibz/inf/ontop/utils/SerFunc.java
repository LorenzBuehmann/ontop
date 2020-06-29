package it.unibz.inf.ontop.utils;

import java.io.Serializable;
import java.util.function.Function;

public interface SerFunc<T,R> extends Function<T,R>, Serializable {}