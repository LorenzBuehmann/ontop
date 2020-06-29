package it.unibz.inf.ontop.model.term.functionsymbol.db;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface DBFunctionSymbolSerializer extends Serializable {

    /**
     * Returns a String in the native query language.
     *
     */
    String getNativeDBString(ImmutableList<? extends ImmutableTerm> terms,
                             Function<ImmutableTerm, String> termConverter, TermFactory termFactory);
}
