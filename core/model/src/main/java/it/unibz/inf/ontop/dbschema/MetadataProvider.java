package it.unibz.inf.ontop.dbschema;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.exception.MetadataExtractionException;

/**
 */

public interface MetadataProvider extends MetadataLookup {

    /**
     * Extracts relation IDs for all relations
     * @return relation IDs
     */
    ImmutableList<RelationID> getRelationIDs() throws MetadataExtractionException;

    /**
     * Inserts the user-supplied primary keys, unique constraints and foreign keys
     * into the metadata object
     */
    void insertIntegrityConstraints(DatabaseRelationDefinition relation, MetadataLookup metadataLookup) throws MetadataExtractionException;

    DBParameters getDBParameters();

}
