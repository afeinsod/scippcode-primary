/*
 * SQLITEWrapper.java
 *
 * Created on Apr 21, 2011, 9:03 PM
 *
 * Provides access to SQLITE Data base in a usable manner.
 * @author Alex Bogert
 * @version 1
 */
package org.lcsim.contrib.scipp.beamcal.sqlite;

import org.sqlite.JDBC;

import java.lang.String;

import java.sql.*;

public class SQLITEWrapper {

  public SQLITEWrapper(){
  } 

  public void close()
      throws java.sql.SQLException { 
    this._conn.close();
  }

  public void addTable(String table)
      throws java.sql.SQLException { 
      String sqlstat = "create table ";
      sqlstat = sqlstat.concat(table);
      sqlstat = sqlstat.concat(" ( column1, column2, column3);");
      PreparedStatement stat = 
          this._conn.prepareStatement(sqlstat);
      stat.execute();
  }

  //Get String related to the reference object
  public Object[] getRow(Object ref, String table) 
      throws java.sql.SQLException { 
      return selectResults(table,ref);
  }

  //Get String related to the reference object
  public String getString(Object ref, String table) 
      throws java.sql.SQLException { 
      return (String)selectResults(table,ref)[0];
  }

  //Get String related to the reference object
  public Double getDouble(Object ref, String table) 
      throws java.sql.SQLException { 
      return (Double)selectResults(table,ref)[1];
  }

  //Get String related to the reference object
  public Integer getInt(Object ref, String table)
      throws java.sql.SQLException { 
      return (Integer)selectResults(table,ref)[2];
  }

  public void connection(String filename)
      throws java.sql.SQLException,java.lang.ClassNotFoundException { 
    Class.forName("org.sqlite.JDBC");

    String connectName = "jdbc:sqlite:";
    connectName        = connectName.concat(filename);
    this._conn =
      DriverManager.getConnection(connectName);
  }

  //Private Methods

  public PreparedStatement insertStatement(String table)
      throws java.sql.SQLException { 
      String prefixStr = "insert into ";
      prefixStr        = prefixStr.concat(table);
      prefixStr        = prefixStr.concat(" values ( ?, ?, ?);");

      return this._conn.prepareStatement(prefixStr);
  }

  public void commit(PreparedStatement statement)
      throws java.sql.SQLException { 
      this._conn.setAutoCommit(false);
      statement.executeBatch();
      this._conn.setAutoCommit(true);

      //Clear the parameters so they can't be commited twice
      statement.clearParameters();
  }

  public PreparedStatement updateStatement(String table) 
      throws java.sql.SQLException { 

      String prefixStr = "update ";
      prefixStr = prefixStr.concat(table);
      prefixStr = prefixStr.concat(" set column1=?, column2=?, column3=? where column1=?;");
      return this._conn.prepareStatement(prefixStr);
  }

  private Object[] selectResults(String table, Object param) 
      throws java.sql.SQLException { 
      String prefix = "select * from ";
      prefix        = prefix.concat(table);
      prefix        = prefix.concat(" where column1='");
      prefix        = prefix.concat(param.toString());
      prefix        = prefix.concat("'");
      PreparedStatement stat = this._conn.prepareStatement(prefix);

      ResultSet set = stat.executeQuery();

      Object[] data = { set.getString(1)
                       ,set.getDouble(2)
                       ,set.getInt(3) };
      set.close();
      set  = null;
      stat = null;
      return data;
  }

  private Connection        _conn;
}
