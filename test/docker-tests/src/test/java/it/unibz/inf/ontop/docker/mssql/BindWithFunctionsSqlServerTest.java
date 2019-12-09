package it.unibz.inf.ontop.docker.mssql;

import it.unibz.inf.ontop.docker.AbstractBindTestWithFunctions;
import it.unibz.inf.ontop.owlapi.OntopOWLReasoner;
import it.unibz.inf.ontop.owlapi.connection.OWLConnection;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to test if functions on Strings and Numerics in SPARQL are working properly.
 *
 */
public class BindWithFunctionsSqlServerTest extends AbstractBindTestWithFunctions {
    private static final String owlfile = "/mssql/sparqlBind.owl";
    private static final String obdafile = "/mssql/sparqlBindSqlServer.obda";
    private static final String propertiesfile = "/mssql/sparqlBindSqlServer.properties";

    private static OntopOWLReasoner REASONER;
    private static OWLConnection CONNECTION;

    public BindWithFunctionsSqlServerTest() throws OWLOntologyCreationException {
        super(createReasoner(owlfile, obdafile, propertiesfile));
        REASONER = getReasoner();
        CONNECTION = getConnection();
    }

    @AfterClass
    public static void after() throws OWLException {
        CONNECTION.close();
        REASONER.dispose();
    }

    @Override
    protected List<String> getAbsExpectedValues() {
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("\"8.5000\"^^xsd:decimal");
        expectedValues.add("\"5.7500\"^^xsd:decimal");
        expectedValues.add("\"6.7000\"^^xsd:decimal");
        expectedValues.add("\"1.5000\"^^xsd:decimal");
        return expectedValues;
    }

    @Override
    protected List<String> getRoundExpectedValues() {
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("\"0.00, 43.00\"^^xsd:string");
        expectedValues.add("\"0.00, 23.00\"^^xsd:string");
        expectedValues.add("\"0.00, 34.00\"^^xsd:string");
        expectedValues.add("\"0.00, 10.00\"^^xsd:string");
        return expectedValues;
    }

    @Override
    protected List<String> getStrExpectedValues() {
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("\"1967-11-05T07:50:00\"^^xsd:string");
        expectedValues.add("\"2011-12-08T12:30:00\"^^xsd:string");
        expectedValues.add("\"2014-06-05T18:47:52\"^^xsd:string");
        expectedValues.add("\"2015-09-21T09:23:06\"^^xsd:string");

        return expectedValues;
    }

    @Ignore("DATETIME does not have an offset. TODO: update the data source (use DATETIME2 instead)")
    @Test
    public void testTZ() throws Exception {
        super.testTZ();
    }

    @Ignore("not supported?")
    @Test
    public void testREGEX() throws Exception {
        super.testREGEX();
    }

    @Ignore("not supported")
    @Test
    public void testREPLACE() throws Exception {
        super.testREPLACE();
    }
}
