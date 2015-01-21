package de.fhm.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataSource {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private Connection connection;
  private int amntInvoices;
  private int[] invoiceIds;

  private static final int DEFAULT_AMNT = 1;
  private static final String USER = "mh";
  private static final String PWD = "mh";
  private static final String DRIVER = "com.mysql.jdbc.Driver";
  private static final String URL = "jdbc:mysql://localhost:3306/test";
  private static final Logger logger = LogManager.getLogger(DataSource.class);

  // --------------------------------------

  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public DataSource() {
	setAmntInvoices(DEFAULT_AMNT);
	setConnection();
  }

  public DataSource(int amntInvoices) {
	setAmntInvoices(amntInvoices);
	setConnection();
  }

  // --------------------------------------

  // ######################################
  // +++++++++ Get-/Set-Methods +++++++++++
  // ######################################
  public Connection getConnection() {
	return this.connection;
  }

  public void setConnection() {
	try {
	  Class.forName(DRIVER);
	  this.connection = DriverManager.getConnection(URL, USER, PWD);
	} catch (ClassNotFoundException | SQLException e) {
	  logger.error(e.getMessage());
	}
  }

  private int getAmntInvoices() {
	return this.amntInvoices;
  }

  private void setAmntInvoices(int amntInvoices) {
	this.amntInvoices = amntInvoices;
  }

  private String getInvoiceIds() {
	String invoiceIds = "";

	for (int i = 0; i < this.invoiceIds.length; i++) {
	  if (i == 0) {
		invoiceIds = Integer.toString(this.invoiceIds[i]);
	  } else {
		invoiceIds += "," + Integer.toString(this.invoiceIds[i]);
	  }
	}

	return invoiceIds;
  }

  private void setInvoiceIds(ResultSet rs) throws SQLException {
	int recordCount = getRecordCount(rs);

	if (recordCount != 0) {
	  this.invoiceIds = new int[recordCount];
	  while (rs.next()) {
		this.invoiceIds[rs.getRow() - 1] = rs.getInt("cs.cs_bill_cdemo_sk");
	  }
	}
  }
  // --------------------------------------

  // ######################################
  // +++++++++++ Class-Methods ++++++++++++
  // ######################################
  public ResultSet generateData() {
	ResultSet rs = null;

	try {
	  setInvoiceIds(getRandomInvoiceIds());
	  rs = getInvoices();
	  logger.trace("/-> Total amount of Invoice-Positions: " + getRecordCount(rs));
	} catch (SQLException e) {
	  logger.error(e.getMessage());
	} finally {
	  try {
		if (!getConnection().isClosed()) {
		  getConnection().close();
		}
	  } catch (SQLException e) {
		logger.error(e.getMessage());
	  }
	}
	
	return rs;
  }

  private ResultSet getRandomInvoiceIds() throws SQLException {
	long start;
	Statement statement = null;
	ResultSet rs = null;
	String sql = getQueryInvoiceId();

	statement = getConnection().createStatement();
	start = System.currentTimeMillis();
	rs = statement.executeQuery(sql);
	logger.trace("/-> Execution of ID-Query: " + (System.currentTimeMillis() - start) + " ms.");

	return rs;
  }

  private ResultSet getInvoices() throws SQLException {
	long start;
	Statement statement = null;
	ResultSet rs = null;
	String sql = getQueryInvoiceDetail();

	statement = getConnection().createStatement();
	start = System.currentTimeMillis();
	rs = statement.executeQuery(sql);
	logger.trace("/-> Execution of Invoice-Query: " + (System.currentTimeMillis() - start) + " ms.");

	return rs;
  }

  private int getRecordCount(ResultSet rs) throws SQLException {
	int recordCount;

	rs.last();
	recordCount = rs.getRow();
	rs.beforeFirst();

	return recordCount;
  }

  private String getQueryInvoiceId() {
	String sql;

	sql = "SELECT cs.cs_bill_cdemo_sk FROM ";
	sql += "(SELECT DISTINCT(cs_bill_cdemo_sk) FROM catalog_sales) AS cs ";
	sql += "ORDER BY RAND() LIMIT " + getAmntInvoices() + ";";
	logger.debug("/-> Query used for Invoice-IDs: " + sql);

	return sql;
  }

  private String getQueryInvoiceDetail() {
	String sql;

	sql = "SELECT c.c_customer_sk, c.c_first_name, c.c_last_name, ";
	sql += "ca.ca_street_name, ca.ca_street_type, ca.ca_street_number, ca.ca_zip, ca.ca_county, ca.ca_state, ca.ca_country, ";
	sql += "cs.cs_bill_cdemo_sk, ";
	sql += "i.i_item_sk, i.i_category, i.i_class, i.i_brand, i.i_product_name, i.i_units, ";
	sql += "cs.cs_quantity, cs.cs_list_price, cs.cs_ext_list_price ";
	sql += "FROM catalog_sales AS cs JOIN customer AS c ON c.c_customer_sk = cs.cs_bill_customer_sk ";
	sql += "JOIN customer_address AS ca ON ca.ca_address_sk = cs.cs_bill_addr_sk ";
	sql += "JOIN item AS i ON i.i_item_sk = cs.cs_item_sk ";
	sql += "WHERE cs.cs_bill_cdemo_sk IN (" + getInvoiceIds() + ") ";
	sql += "ORDER BY cs.cs_bill_cdemo_sk, c.c_customer_sk;";
	logger.debug("/-> Query used for Invoices: " + sql);

	return sql;
  }
  // --------------------------------------
}
