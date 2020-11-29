package it.unibz.inf.ontop.dbschema.impl;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unibz.inf.ontop.dbschema.QuotedID;
import it.unibz.inf.ontop.exception.MetadataExtractionException;
import it.unibz.inf.ontop.model.type.TypeFactory;

import java.sql.Connection;

public class SQLServerDBMetadataProvider extends DefaultDBMetadataProvider {

    private final ImmutableSet<String> ignoredSchemas = ImmutableSet.of("sys", "INFORMATION_SCHEMA");
    private final QuotedID defaultSchema;

    @AssistedInject
    SQLServerDBMetadataProvider(@Assisted Connection connection, TypeFactory typeFactory) throws MetadataExtractionException {
        super(connection, typeFactory);
        // https://msdn.microsoft.com/en-us/library/ms175068.aspx
        this.defaultSchema = retrieveDefaultSchema("SELECT SCHEMA_NAME()");
    }

    @Override
    public QuotedID getDefaultSchema() { return defaultSchema; }

    @Override
    protected boolean isSchemaIgnored(String schema) { return ignoredSchemas.contains(schema); }

    /*       return "SELECT TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME " +
					"FROM INFORMATION_SCHEMA.TABLES " +
					"WHERE TABLE_TYPE='BASE TABLE' OR TABLE_TYPE='VIEW'";
    */
}
