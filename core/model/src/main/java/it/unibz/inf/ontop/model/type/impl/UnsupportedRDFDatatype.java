package it.unibz.inf.ontop.model.type.impl;


import it.unibz.inf.ontop.model.type.DBTypeFactory;
import it.unibz.inf.ontop.model.type.RDFDatatype;
import it.unibz.inf.ontop.model.type.TermTypeAncestry;
import it.unibz.inf.ontop.model.vocabulary.OntopInternal;
import org.apache.commons.rdf.api.IRI;

public class UnsupportedRDFDatatype extends SimpleRDFDatatype {

    private UnsupportedRDFDatatype(TermTypeAncestry parentAncestry) {
        super(OntopInternal.UNSUPPORTED, parentAncestry, (DBTypeFactorySerializable) DBTypeFactory::getDBStringType);
    }

    private UnsupportedRDFDatatype(TermTypeAncestry parentAncestry, IRI concreteIRI) {
        super(concreteIRI, parentAncestry, (DBTypeFactorySerializable) DBTypeFactory::getDBStringType);
    }

    static RDFDatatype createUnsupportedDatatype(TermTypeAncestry parentAncestry, IRI concreteIRI) {
        return new UnsupportedRDFDatatype(parentAncestry, concreteIRI);
    }

    static RDFDatatype createUnsupportedDatatype(TermTypeAncestry parentAncestry) {
        return new UnsupportedRDFDatatype(parentAncestry);
    }
}
