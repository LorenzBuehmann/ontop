package it.unibz.inf.ontop.model.type;

import java.io.Serializable;

public interface RDFTermType extends TermType, Serializable {

    /**
     * If the RDF term type has a canonical natural DB type, returns it.
     * Otherwise, returns a DB string, if the type is not abstract.
     *
     * Throws an UnsupportedOperationException if is abstract
     */
    DBTermType getClosestDBType(DBTypeFactory dbTypeFactory) throws UnsupportedOperationException;

}
