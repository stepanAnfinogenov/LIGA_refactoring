package ro.planet.documentum.stada.modules.services.utils.query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ro.planet.documentum.stada.common.utils.query.QueryUtils;

import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.expr.internal.Pair;
import ro.planet.documentum.stada.modules.services.utils.query.impl.QueryBuilder;

@AllArgsConstructor
@NoArgsConstructor
public class QueryBuilderImpl implements QueryBuilder {
  private static final String FROM_CLAUSE = "from";
  private static final String SELECT_CLAUSE = "select";
  private static final String WHERE_CLAUSE = "where";
  private static final String DATE_CLAUSE = "date";
  private static final String SPACE = " ";
  private static final String EMPTY = "";
  private static final String APOSTROPHE = "'";
  private static final String LEFT_BRACKET = "(";
  private static final String RIGHT_BRACKET = ")";
  private static final String COMMA = ",";
  private static final String PERCENT_CLAUSE = "%";
  private static final String EQUALS = "=";

  private List<String> selectedAttributesList = new ArrayList<>();
  private List<Pair<String, String>> clauseList = new ArrayList<>();
  private List<String> selectedTypes = new ArrayList<>();
  private String dql;

  public void addSelectedType(String selectedType) {
    this.selectedTypes.add(selectedType);
  }

  public String getDql() throws DfException {
    buildDQL();
    return dql;
  }

  public void addSelectedAttribute(String attribute) {
    if (attribute != null && attribute.trim().length() > 0) {
      selectedAttributesList.add(attribute);
    }
  }

  public void addSelectedAttributes(String[] attributes) {
    if (attributes != null && attributes.length > 0) {
      selectedAttributesList.addAll(Arrays.asList(attributes));
    }
  }

  // move all methods addClause into new class
  public void addClause(String logicalOpertaor, String attribute, String value, String operator) {
    if (value != null && value.length() > 0) {
      StringBuffer clause = new StringBuffer();
      clause.append(SPACE)
          .append(attribute)
          .append(operator)
          .append(APOSTROPHE)
          .append(value)
          .append(APOSTROPHE);
      clauseList.add(new Pair<>(logicalOpertaor, clause.toString()));
    }
  }

  public void addClause(String logicalOpertaor, String attribute, Number value, String operator) {
    if (value != null) {
      StringBuffer clause = new StringBuffer();
      clause.append(SPACE)
          .append(attribute)
          .append(operator)
          .append(value);
      clauseList.add(new Pair<>(logicalOpertaor, clause.toString()));
    }
  }

  public void addClause(String logicalOpertaor, String attribute, IDfId value) {
    if (value != null && value.getId().length() > 0 && !value.isNull()) {
      StringBuffer clause = new StringBuffer();
      clause.append(SPACE)
          .append(attribute)
          .append(EQUALS)
          .append(APOSTROPHE)
          .append(value.getId())
          .append(APOSTROPHE);
      clauseList.add(new Pair<>(logicalOpertaor, clause.toString()));
    }

  }

  public void addClause(String logicalOpertaor, String attribute, Date value, String format, String operator) {
    if (value != null) {
      String date = formatDate(value, format);
      StringBuffer clause = new StringBuffer();
      clause.append(SPACE)
          .append(attribute)
          .append(operator)
          .append(DATE_CLAUSE)
          .append(LEFT_BRACKET)
          .append(APOSTROPHE)
          .append(date)
          .append(APOSTROPHE)
          .append(COMMA)
          .append(APOSTROPHE)
          .append(format)
          .append(APOSTROPHE)
          .append(RIGHT_BRACKET);

      clauseList.add(new Pair<>(logicalOpertaor, clause.toString()));
    }
  }

  public void addClause(String logicalOpertaor, String clause, String[] values) {
    boolean addClause = true;
    if (values != null && values.length > 0) {
      for (int i = 0; i < values.length; i++) {
        if (values[i] != null && values[i].length() > 0) {
          String placeholder = PERCENT_CLAUSE + (i + 1);
          clause = clause.replaceAll(placeholder, QueryUtils.toDqlToken(values[i]));
        } else {
          addClause = false;
        }
      }
    }
    if (addClause) {
      clauseList.add(new Pair<>(logicalOpertaor, clause));
    }
  }

  public void addClause(String logicalOpertaor, String clause) {
    clauseList.add(new Pair<>(logicalOpertaor, clause));
  }

  private void buildDQL() throws DfException {
    StringBuffer dqlBuilder = new StringBuffer();
    addSelect(dqlBuilder);
    addFrom(dqlBuilder);
    addWhere(dqlBuilder);
    dql = dqlBuilder.toString();
  }

  private void addWhere(StringBuffer dqlBuilder) {
    if (!clauseList.isEmpty()) {
      int i = 0;
      for (Pair<String, String> clause : clauseList) {
        String firstStatement = (i == 0) ? WHERE_CLAUSE : clause.first();
        dqlBuilder.append(firstStatement)
            .append(SPACE)
            .append(clause.second())
            .append(SPACE);
        i++;
      }
    }
  }

  private void addFrom(StringBuffer dqlBuilder) {
    dqlBuilder.append(FROM_CLAUSE)
        .append(SPACE);
    for (String selectedType : selectedTypes) {
      dqlBuilder.append(selectedType)
          .append(COMMA)
          .append(SPACE);
    }
    dqlBuilder.append(SPACE)
        .deleteCharAt(dqlBuilder.lastIndexOf(COMMA));
  }

  private void addSelect(StringBuffer dqlBuilder) throws DfException {
    dqlBuilder.append(SELECT_CLAUSE)
        .append(SPACE);

    if (!selectedAttributesList.isEmpty()) {
      for (String attribute : selectedAttributesList) {
        dqlBuilder.append(attribute)
            .append(COMMA)
            .append(SPACE);
      }
    } else {
      throw new DfException("Selected attributes cannot be null!");
    }
    dqlBuilder.deleteCharAt(dqlBuilder.lastIndexOf(COMMA));
  }

  private String formatDate(Date date, String format) {
    String outDate = EMPTY;
    if (date != null) {
      SimpleDateFormat simpFormat = new SimpleDateFormat(format);
      outDate = simpFormat.format(date);
    }
    return outDate;
  }
}
