package it.unibz.inf.ontop.model.term;

import java.io.Serializable;

import it.unibz.inf.ontop.model.type.MetaRDFTermType;
import it.unibz.inf.ontop.model.type.RDFTermType;

/**
 * When the RDF term types are "reified" as constants
 *
 * Should disappear from the query before its translation
 * into a native query.
 *
 * Plays at a "meta-level".
 *
 */
public interface RDFTermTypeConstant extends NonNullConstant, Serializable {

    @Override
    MetaRDFTermType getType();

    RDFTermType getRDFTermType();
}
