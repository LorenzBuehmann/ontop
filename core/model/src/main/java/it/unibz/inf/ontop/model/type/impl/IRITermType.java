package it.unibz.inf.ontop.model.type.impl;

import it.unibz.inf.ontop.model.type.ObjectRDFType;
import it.unibz.inf.ontop.model.type.TermTypeAncestry;


public class IRITermType extends RDFTermTypeImpl implements ObjectRDFType {

    protected IRITermType(TermTypeAncestry parentAncestry) {
        super("IRI", parentAncestry, (SerializableDBTypeFactory) (dbTypeFactory) -> dbTypeFactory.getDBStringType());
    }

    @Override
    public boolean isBlankNode() {
        return false;
    }
}
