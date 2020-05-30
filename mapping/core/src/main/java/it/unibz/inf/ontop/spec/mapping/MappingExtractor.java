package it.unibz.inf.ontop.spec.mapping;


import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.dbschema.DBParameters;
import it.unibz.inf.ontop.exception.MappingException;
import it.unibz.inf.ontop.exception.MetadataExtractionException;
import it.unibz.inf.ontop.spec.OBDASpecInput;
import it.unibz.inf.ontop.iq.tools.ExecutorRegistry;
import it.unibz.inf.ontop.spec.mapping.pp.PreProcessedMapping;
import it.unibz.inf.ontop.spec.ontology.Ontology;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface MappingExtractor {

    /**
     * TODO: in a near future, drop DummyDBMetadataBuilder and use Mapping instead of this interface
     */
    interface MappingAndDBParameters {
        ImmutableList<MappingAssertion> getMapping();
        DBParameters getDBParameters();
    }

    MappingAndDBParameters extract(@Nonnull OBDASpecInput specInput,
                                   @Nonnull Optional<Ontology> saturatedTBox)
            throws MappingException, MetadataExtractionException;

    MappingAndDBParameters extract(@Nonnull PreProcessedMapping ppMapping,
                                   @Nonnull OBDASpecInput specInput,
                                   @Nonnull Optional<Ontology> saturatedTBox)
            throws MappingException, MetadataExtractionException;
}
