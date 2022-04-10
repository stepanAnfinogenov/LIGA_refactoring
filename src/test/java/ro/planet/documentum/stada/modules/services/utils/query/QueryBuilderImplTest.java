package ro.planet.documentum.stada.modules.services.utils.query;

import com.documentum.fc.common.DfException;
import org.junit.jupiter.api.Test;
import ro.planet.documentum.stada.modules.services.utils.query.impl.QueryBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class QueryBuilderImplTest {
  private static final String LOGICAL_OR = "or";
  private static final String LOGICAL_AND = "and";

  SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
  private final String CURRENT_DATE = format.format(( new Date()   ));

  @Test
  void getDql_correctBehavior_AllParametersCorrect() throws DfException {
    QueryBuilder q = new QueryBuilderImpl();
    q.addSelectedType("dm_sysobject s");
    q.addSelectedType("dm_document d");
    q.addSelectedAttribute("pere");
    q.addClause(LOGICAL_OR, "z", 20, "=");
    q.addClause(LOGICAL_AND, "y", "", "=");
    q.addClause(LOGICAL_AND, "k", "d", "=");
    q.addClause(LOGICAL_AND, "d", new Date(), "MM/dd/yyyy", "=");
    q.addClause(LOGICAL_AND, "lower(dss_description) like lower('%%1%');", new String[] { "a" });

    String expected = "select pere from dm_sysobject s, dm_document d  where  z=20 and  k='d' and  " +
        "d=date('"+ CURRENT_DATE + "','MM/dd/yyyy') and lower(dss_description) like lower('%a%'); ";

    assertEquals(expected, q.getDql());
  }
}