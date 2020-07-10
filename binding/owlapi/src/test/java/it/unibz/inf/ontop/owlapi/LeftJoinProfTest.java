package it.unibz.inf.ontop.owlapi;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.UnaryIQTree;
import it.unibz.inf.ontop.iq.node.NativeNode;
import it.unibz.inf.ontop.owlapi.connection.OntopOWLConnection;
import it.unibz.inf.ontop.owlapi.connection.OntopOWLStatement;
import it.unibz.inf.ontop.owlapi.resultset.OWLBindingSet;
import it.unibz.inf.ontop.owlapi.resultset.TupleOWLResultSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLLiteral;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.unibz.inf.ontop.utils.OWLAPITestingTools.executeFromFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeftJoinProfTest {

    private static final String CREATE_SCRIPT = "src/test/resources/test/redundant_join/redundant_join_fk_create.sql";
    private static final String DROP_SCRIPT = "src/test/resources/test/redundant_join/redundant_join_fk_drop.sql";
    private static final String OWL_FILE = "src/test/resources/test/redundant_join/redundant_join_fk_test.owl";
    private static final String ODBA_FILE = "src/test/resources/test/redundant_join/redundant_join_fk_test.obda";
    private static final String PROPERTY_FILE = "src/test/resources/test/redundant_join/redundant_join_fk_test.properties";
    private static final String NO_SELF_LJ_OPTIMIZATION_MSG = "The table professors should be used only once";
    private static final String LEFT_JOIN_NOT_OPTIMIZED_MSG = "The left join is still present in the output query";

    private Connection conn;


    @Before
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:professor";
        String username = "sa";
        String password = "sa";

        conn = DriverManager.getConnection(url, username, password);
        executeFromFile(conn, CREATE_SCRIPT);
    }

    @After
    public void tearDown() throws Exception {
        executeFromFile(conn, DROP_SCRIPT);
        conn.close();
    }

    @Test
    public void testSimpleFirstName() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :firstName ?v\n" +
                "  }\n" +
                "}";

        List<String> expectedValues = ImmutableList.of(
                "Roger", "Frank", "John", "Michael", "Diego", "Johann", "Barbara", "Mary"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"professors\""));
        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"PROFESSORS\""));
    }

    @Test
    public void testFullName1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :firstName ?v ;\n" +
                "          :lastName ?lastName .\n" +
                "  }\n" +
                "}";

        List<String> expectedValues = ImmutableList.of(
        "Roger", "Frank", "John", "Michael", "Diego", "Johann", "Barbara", "Mary"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"professors\""));
        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"PROFESSORS\""));
    }

    @Test
    public void testFullName2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :firstName ?v .\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "     ?p :lastName ?lastName .\n" +
                "   }\n" +
                "}";

        List<String> expectedValues = ImmutableList.of(
                "Roger", "Frank", "John", "Michael", "Diego", "Johann", "Barbara", "Mary"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"professors\""));
        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql, "\"PROFESSORS\""));
    }

    @Test
    public void testFirstNameNickname() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :firstName ?v ;\n" +
                "          :nickname ?nickname .\n" +
                "  }\n" +
                "} ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
               "Frank", "John", "Michael", "Roger"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(LEFT_JOIN_NOT_OPTIMIZED_MSG, sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testRequiredTeacherNickname() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :nickname ?v; \n" +
                "        :teaches ?c ." +
                "  }\n" +
                "  FILTER (bound(?v))\n" +
                "}\n"
                + "ORDER BY ?v\n";

        List<String> expectedValues = ImmutableList.of(
                "Johnny", "Rog"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
        assertFalse(LEFT_JOIN_NOT_OPTIMIZED_MSG, sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testMinusNickname() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?v .\n" +
                "   OPTIONAL {\n" +
                "      ?p :nickname ?nickname .\n" +
                "  }\n" +
                " FILTER (!bound(?nickname)) \n" +
                "} ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Barbara", "Diego", "Johann", "Mary"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(LEFT_JOIN_NOT_OPTIMIZED_MSG, sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testMinus2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?v ; :lastName ?l . \n" +
                "   FILTER(contains(?v, \"a\"))\n" +
                "   OPTIONAL {\n" +
                "      ?p :nickname ?nickname .\n" +
                "      BIND(true AS ?w) \n" +
                "  }\n" +
                " FILTER (!bound(?w)) \n" +
                "} ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Barbara", "Johann", "Mary"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(LEFT_JOIN_NOT_OPTIMIZED_MSG, sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testMinusLastname() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?v .\n" +
                "   OPTIONAL {\n" +
                "      ?p :lastName ?n .\n" +
                "  }\n" +
                " FILTER (!bound(?n)) \n" +
                "} ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of();
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testSimpleNickname() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {\n" +
                "     ?p :nickname ?v\n" +
                "  }\n" +
                "}";

        List<String> expectedValues = ImmutableList.of(
                "Rog", "Frankie", "Johnny", "King of Pop"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql.toLowerCase(), "\"professors\""));
    }

    @Test
    public void testNicknameAndCourse() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v ?f\n" +
                "WHERE {\n" +
                "   ?p a :Professor ;\n" +
                "      :firstName ?f ;\n" +
                "      :teaches ?c .\n" +
                "   OPTIONAL {\n" +
                "     ?p :nickname ?v\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY DESC(?v)\n";

        List<String> expectedValues = ImmutableList.of(
                "Rog", "Rog", "Johnny"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(NO_SELF_LJ_OPTIMIZATION_MSG, containsMoreThanOneOccurrence(sql.toLowerCase(), "\"professors\""));
    }

    @Test
    public void testCourseTeacherName() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   OPTIONAL {\n" +
                "     ?p :lastName ?v\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY DESC(?v)";

        List<String> expectedValues = ImmutableList.of(
                "Smith", "Poppins", "Depp"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testCourseJoinOnLeft1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?f ; \n" +
                "      :teaches ?c .\n" +
                "   OPTIONAL {\n" +
                "     ?p :lastName ?v\n" +
                "  }\n" +
                "FILTER (bound(?f))\n" +
                "}\n" +
                "ORDER BY DESC(?v)";

        List<String> expectedValues = ImmutableList.of(
                "Smith", "Poppins", "Depp"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testCourseJoinOnLeft2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?v ; \n" +
                "      :teaches ?c .\n" +
                "   OPTIONAL {\n" +
                "     ?p :lastName ?v\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "John", "Mary", "Roger"
        );
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testNotEqOrUnboundCondition() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?v . \n" +
                "   ?p :teaches ?c .\n" +
                "   OPTIONAL {\n" +
                "     ?p :nickname ?n\n" +
                "  }\n" +
                "  FILTER ((?n != \"Rog\") || !bound(?n))\n" +
                "}" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
               "John", "Mary"
        );
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }
    
    @Test
    public void testPreferences() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     ?p :nickname ?v .\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "     ?p :lastName ?v\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Dodero", "Frankie", "Gamper", "Helmer", "Johnny", "King of Pop", "Poppins", "Rog");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testUselessRightPart2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     ?p :lastName ?v .\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "     ?p :firstName ?v\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Depp", "Dodero", "Gamper", "Helmer", "Jackson", "Pitt", "Poppins", "Smith");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testOptionalTeachesAt() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor ; \n" +
                "        :lastName ?v .\n" +
                "   OPTIONAL { \n" +
                "     ?p :teachesAt ?u .\n" +
                "   }\n" +
                "   FILTER (bound(?u))\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Depp", "Poppins", "Smith");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testOptionalTeacherID() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor ; \n" +
                "        :lastName ?v .\n" +
                "   OPTIONAL { \n" +
                "     ?p :teacherID ?id .\n" +
                "   }\n" +
                "   FILTER (bound(?id))\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of(
                "Depp", "Poppins", "Smith");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);

        assertFalse(sql.toUpperCase().contains("LEFT"));
    }

    @Test
    public void testSumStudents1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT (SUM(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?c a :Course ; \n" +
                "        :nbStudents ?nb .\n" +
                "}\n";

        List<String> expectedValues = ImmutableList.of("46");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumStudents2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p \n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("12", "13", "21");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumStudents3() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :teaches ?c .\n" +
                "      ?c :nbStudents ?nb .\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "12", "13", "21");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumStudents4() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?s) (CONCAT(?fName, \": \", str(?s)) AS ?v) \n" +
                "WHERE {\n" +
                "   ?p :teaches ?c ; :firstName ?fName .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p ?fName \n" +
                "ORDER BY ?s";

        List<String> expectedValues = ImmutableList.of("John: 12", "Mary: 13", "Roger: 21");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumStudents5() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?s) (CONCAT(?fName, \": \", str(SUM(?nb))) AS ?v) \n" +
                "WHERE {\n" +
                "   ?p :teaches ?c ; :firstName ?fName .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p ?fName \n" +
                "ORDER BY ?s";

        List<String> expectedValues = ImmutableList.of("John: 12", "Mary: 13", "Roger: 21");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testDistinctAsGroupBy1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "\n" +
                "SELECT (CONCAT(?fName, \".\") AS ?v) ((1+1) AS ?y) \n" +
                "WHERE {\n" +
                "   ?p :firstName ?fName .\n" +
                "}\n" +
                "GROUP BY ?p ?fName \n" +
                "ORDER BY ?fName";

        List<String> expectedValues = ImmutableList.of("Barbara.", "Diego.", "Frank.", "Johann.", "John.", "Mary.",
                "Michael.", "Roger.");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testAvgStudents1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT (AVG(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?c a :Course ; \n" +
                "        :nbStudents ?nb .\n" +
                "}\n";

        List<String> expectedValues = ImmutableList.of("11.5");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testAvgStudents2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (AVG(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p \n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("10.5","12", "13");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testAvgStudents3() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (AVG(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :teaches ?c .\n" +
                "      ?c :nbStudents ?nb .\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "10.5", "12", "13");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMinStudents1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT (MIN(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?c a :Course ; \n" +
                "        :nbStudents ?nb .\n" +
                "}\n";

        List<String> expectedValues = ImmutableList.of("10");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMinStudents2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (MIN(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p \n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("10","12", "13");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMaxStudents1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT (MAX(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?c a :Course ; \n" +
                "        :nbStudents ?nb .\n" +
                "}\n";

        List<String> expectedValues = ImmutableList.of("13");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMaxStudents2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (MAX(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "}\n" +
                "GROUP BY ?p \n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("11","12", "13");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testDuration1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?d) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :teaches ?c .\n" +
                "      ?c :duration ?d .\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "18", "20", "54.5");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMultitypedSum1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?n) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   { ?c :duration ?n } \n" +
                "   UNION" +
                "   { ?c :nbStudents ?n }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("31", "32", "75.5");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMultitypedAvg1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (AVG(?n) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   { ?c :duration ?n } \n" +
                "   UNION" +
                "   { ?c :nbStudents ?n }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("15.5", "16", "18.875");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    /**
     * Checks that the type error is detected
     */
    @Test
    public void testMinusMultitypedSum() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor ;\n" +
                "        :lastName ?v .\n" +
                "   MINUS {\n " +
                "     SELECT ?p (SUM(?n) AS ?v){\n" +
                "       { \n" +
                "          ?p :teaches ?c .\n" +
                "          ?c :duration ?n " +
                "       } \n" +
                "       UNION" +
                "       { \n" +
                "          ?p :teaches ?c .\n" +
                "          ?p :lastName ?n " +
                "       }\n" +
                "     } GROUP BY ?p\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("Dodero", "Gamper", "Helmer", "Jackson", "Pitt");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    /**
     * Checks that the type error is detected
     */
    @Test
    public void testMinusMultitypedAvg() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p ?v\n" +
                "WHERE {\n" +
                "   ?p a :Professor ;\n" +
                "        :lastName ?v .\n" +
                "   MINUS {\n " +
                "     SELECT ?p (AVG(?n) AS ?v){\n" +
                "       { \n" +
                "          ?p :teaches ?c .\n" +
                "          ?c :duration ?n " +
                "       } \n" +
                "       UNION" +
                "       { \n" +
                "          ?p :teaches ?c .\n" +
                "          ?p :lastName ?n " +
                "       }\n" +
                "     } GROUP BY ?p\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("Dodero", "Gamper", "Helmer", "Jackson", "Pitt");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    /**
     * Tests that the FILTER is not lifted above the query modifiers
     */
    @Test
    public void testLimitSubQuery1() throws Exception {
        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v {\n" +
                "  ?p a :Professor; :lastName ?v .\n" +
                "  {\n" +
                "   SELECT ?p {\n" +
                "     ?p :teaches [ :duration ?d ]\n" +
                "     FILTER ((?d < 21) && (?d > 19))\n" +
                "    }\n" +
                "   ORDER BY ?d\n" +
                "   LIMIT 1\n" +
                "  }\n" +
                "}";

        List<String> expectedValues = ImmutableList.of("Depp");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumOverNull1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :nonExistingProperty ?nb\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "0", "0", "0");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testAvgOverNull1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (AVG(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :nonExistingProperty ?nb\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "0", "0", "0");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testCountOverNull1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (COUNT(?nb) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :nonExistingProperty ?nb\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "0", "0", "0");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMinOverNull1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (MIN(?nb) AS ?m) (0 AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :nonExistingProperty ?nb\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "0", "0", "0");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testMaxOverNull1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (MAX(?nb) AS ?m) (0 AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor .\n" +
                "   OPTIONAL {" +
                "      ?p :nonExistingProperty ?nb\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?v";

        List<String> expectedValues = ImmutableList.of("0", "0", "0", "0", "0", "0", "0", "0");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumPreferences1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?s) ?v\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "   OPTIONAL { \n" +
                "       ?p :nickname \"Rog\". \n" +
                "       BIND (\"A\" AS ?v)\n" +
                "   }\n" +
                "   OPTIONAL { \n" +
                "       ?p :firstName \"Mary\". \n" +
                "       BIND (\"B\" AS ?v)\n" +
                "   }\n" +
                "   OPTIONAL { \n" +
                "       ?p :firstName \"John\". \n" +
                "       BIND (\"C\" AS ?v)\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p ?v\n" +
                "ORDER BY ?s";

        List<String> expectedValues = ImmutableList.of("C", "B", "A");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumPreferences2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?s) ?v\n" +
                "WHERE {\n" +
                "   ?p :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "   OPTIONAL { \n" +
                "       ?p :nickname \"Rog\". \n" +
                "       BIND (\"A\"@en AS ?v)\n" +
                "   }\n" +
                "   OPTIONAL { \n" +
                "       ?p :firstName \"Mary\". \n" +
                "       BIND (\"B\" AS ?v)\n" +
                "   }\n" +
                "   OPTIONAL { \n" +
                "       ?p :firstName \"John\". \n" +
                "       BIND (\"C\" AS ?v)\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p ?v\n" +
                "ORDER BY ?s";

        List<String> expectedValues = ImmutableList.of("C", "B", "A");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testSumPreferences3() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (SUM(?nb) AS ?s) ?v\n" +
                "WHERE {\n" +
                "   ?p :firstName ?fn ; :teaches ?c .\n" +
                "   ?c :nbStudents ?nb .\n" +
                "   FILTER (?fn != \"John\")\n" +
                "   OPTIONAL { \n" +
                "       ?p :nickname \"Rog\". \n" +
                "       BIND (\"A\"@en AS ?v)\n" +
                "   }\n" +
                "   OPTIONAL { \n" +
                "       ?p :firstName \"Mary\". \n" +
                "       BIND (\"B\" AS ?v)\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p ?v\n" +
                "ORDER BY ?s";

        List<String> expectedValues = ImmutableList.of("B", "A");
        String sql = checkReturnedValuesAndReturnSql(query, expectedValues).get();

        System.out.println("SQL Query: \n" + sql);
    }

    @Test
    public void testGroupConcat1() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (GROUP_CONCAT(?n) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     ?p :nickname ?n .\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("Rog", "Frankie", "Johnny", "King of Pop", "", "", "", "");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testGroupConcat2() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (GROUP_CONCAT(?n) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     { ?p :nickname ?n }\n" +
                "     UNION \n" +
                "     { ?p :nickname ?n }\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("Rog Rog", "Frankie Frankie", "Johnny Johnny", "King of Pop King of Pop", "", "", "", "");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testGroupConcat3() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (GROUP_CONCAT(DISTINCT ?n) AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     { ?p :nickname ?n }\n" +
                "     UNION \n" +
                "     { ?p :nickname ?n }\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("Rog", "Frankie", "Johnny", "King of Pop", "", "", "", "");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testGroupConcat4() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (GROUP_CONCAT(?n ; separator='|') AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     { ?p :nickname ?n }\n" +
                "     UNION \n" +
                "     { ?p :nickname ?n }\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("Rog|Rog", "Frankie|Frankie", "Johnny|Johnny", "King of Pop|King of Pop", "", "", "", "");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testGroupConcat5() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (GROUP_CONCAT(DISTINCT ?n ; separator='|') AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     { ?p :nickname ?n }\n" +
                "     UNION \n" +
                "     { ?p :nickname ?n }\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("Rog", "Frankie", "Johnny", "King of Pop", "", "", "", "");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }
    
    @Test
    public void testGroupConcat6() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?p (COALESCE(GROUP_CONCAT(?n),'nothing') AS ?v)\n" +
                "WHERE {\n" +
                "   ?p a :Professor . \n" +
                "   OPTIONAL { \n" +
                "     { ?p :nickname ?n }\n" +
                "     UNION \n" +
                "     { ?p :teaches ?c .\n" +
                "       ?c :nbStudents ?n }\n" +
                "   }\n" +
                "}\n" +
                "GROUP BY ?p\n" +
                "ORDER BY ?p\n";

        List<String> expectedValues = ImmutableList.of("nothing", "Frankie", "nothing", "King of Pop", "", "", "", "nothing");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testProperties() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT DISTINCT ?v\n" +
                "WHERE {\n" +
                "   { [] ?p1 \"Frankie\"  }\n" +
                "    UNION \n" +
                "   { [] ?p2 10 }\n" +
                "   BIND(str(coalesce(?p1, ?p2)) AS ?v)" +
                "}\n" +
                "ORDER BY ?v\n";

        List<String> expectedValues = ImmutableList.of("http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#nbStudents", "http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#nickname");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    @Test
    public void testCommonNonProjectedVariable() throws Exception {

        String query =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "\n" +
                "SELECT ?v\n" +
                "WHERE {\n" +
                "   { SELECT ?c { ?c  :duration ?o  } }\n" +
                "   { SELECT ?c { ?o :teaches ?c } }\n" +
                "   BIND(str(?c) AS ?v)" +
                "}\n" +
                "ORDER BY ?v\n";

        List<String> expectedValues = ImmutableList.of("http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#course/AdvancedDatabases",
                "http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#course/DiscreteMathematics",
                "http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#course/LinearAlgebra",
                "http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#course/ScientificWriting");
        checkReturnedValuesAndReturnSql(query, expectedValues);
    }

    private static boolean containsMoreThanOneOccurrence(String query, String pattern) {
        int firstOccurrenceIndex = query.indexOf(pattern);
        if (firstOccurrenceIndex >= 0) {
            return query.substring(firstOccurrenceIndex + 1).contains(pattern);
        }
        return false;
    }

    private Optional<String> checkReturnedValuesAndReturnSql(String query, List<String> expectedValues) throws Exception {

        OntopOWLFactory factory = OntopOWLFactory.defaultFactory();
        OntopSQLOWLAPIConfiguration config = OntopSQLOWLAPIConfiguration.defaultBuilder()
                .nativeOntopMappingFile(ODBA_FILE)
                .ontologyFile(OWL_FILE)
                .propertyFile(PROPERTY_FILE)
                .enableTestMode()
                .build();
        OntopOWLReasoner reasoner = factory.createReasoner(config);

        // Now we are ready for querying
        OntopOWLConnection conn = reasoner.getConnection();
        Optional<String> sql;

        int i = 0;
        List<String> returnedValues = new ArrayList<>();
        try (OntopOWLStatement st = conn.createStatement()) {
            IQ executableQuery = st.getExecutableQuery(query);
            sql = Optional.of(executableQuery.getTree())
                    .filter(t -> t instanceof UnaryIQTree)
                    .map(t -> ((UnaryIQTree) t).getChild().getRootNode())
                    .filter(n -> n instanceof NativeNode)
                    .map(n -> ((NativeNode) n).getNativeQueryString());

            TupleOWLResultSet rs = st.executeSelectQuery(query);
            while (rs.hasNext()) {
                final OWLBindingSet bindingSet = rs.next();
                OWLLiteral ind1 = bindingSet.getOWLLiteral("v");
                // log.debug(ind1.toString());
                if (ind1 != null) {
                    returnedValues.add(ind1.getLiteral());
                    System.out.println(ind1.getLiteral());
                    i++;
                }
            }
        }
        finally {
            conn.close();
            reasoner.dispose();
        }
        assertTrue(String.format("%s instead of \n %s", returnedValues.toString(), expectedValues.toString()),
                returnedValues.equals(expectedValues));
        assertTrue(String.format("Wrong size: %d (expected %d)", i, expectedValues.size()), expectedValues.size() == i);

        return sql;
    }
}
