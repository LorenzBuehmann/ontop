package it.unibz.inf.ontop.spec.mapping;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.dbschema.MetadataLookup;
import it.unibz.inf.ontop.exception.InvalidMappingSourceQueriesException;
import it.unibz.inf.ontop.iq.tools.ExecutorRegistry;
import it.unibz.inf.ontop.spec.mapping.pp.PreProcessedTriplesMap;

/**
 * TODO: explain
 */
public interface PPMappingConverter<T extends PreProcessedTriplesMap> {

    ImmutableList<MappingAssertion> convert(ImmutableList<T> ppMapping, MetadataLookup dbMetadata) throws InvalidMappingSourceQueriesException;

}
