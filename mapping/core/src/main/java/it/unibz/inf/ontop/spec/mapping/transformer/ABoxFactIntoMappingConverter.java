package it.unibz.inf.ontop.spec.mapping.transformer;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.spec.mapping.MappingAssertion;
import it.unibz.inf.ontop.spec.ontology.RDFFact;

public interface ABoxFactIntoMappingConverter {

    ImmutableList<MappingAssertion> convert(ImmutableSet<RDFFact> assertions, boolean isOntologyAnnotationQueryingEnabled);
}
