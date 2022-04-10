package ro.planet.documentum.stada.modules.services.utils.query.impl;

import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

import java.util.Date;

public interface QueryBuilder {

  void addSelectedType(String selectedType) throws DfException;

  String getDql() throws DfException;

  void addSelectedAttribute(String attribute);

  void addSelectedAttributes(String[] attributes);

  // move all methods addClause into new class
  void addClause(String logicalOpertaor, String attribute, String value, String operator);

  void addClause(String logicalOpertaor, String attribute, Number value, String operator);

  void addClause(String logicalOpertaor, String attribute, IDfId value);

  void addClause(String logicalOpertaor, String attribute, Date value, String format, String operator);

  void addClause(String logicalOpertaor, String clause, String[] values);

  void addClause(String logicalOpertaor, String clause);
}
