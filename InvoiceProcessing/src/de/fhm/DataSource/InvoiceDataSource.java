package de.fhm.DataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import de.fhm.DataFormat.InvoiceDataFormat;
import de.fhm.DataProcessing.InvoiceDataProcessing;

public class InvoiceDataSource {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private Connection connection;
  private int invoiceCount;
  private int[] invoiceIds;
  private long latestTimeStamp;

  private static final long START_TIME_STAMP = System.currentTimeMillis();
  private static final String USER = "mh";
  private static final String PWD = "mh";
  private static final String JDBC_CLASS = "com.mysql.jdbc.Driver";
  private static final String URL = "jdbc:mysql://localhost:3306/test";
  private static final Logger logger = LogManager.getLogger(InvoiceDataSource.class);

  // --------------------------------------

  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public InvoiceDataSource() {
	setInvoiceCount(InvoiceDataProcessing.INVOICE_COUNT);
	setConnection();
	setLatestTimeStamp(START_TIME_STAMP);
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
	  Class.forName(JDBC_CLASS);
	  this.connection = DriverManager.getConnection(URL, USER, PWD);
	} catch (ClassNotFoundException | SQLException e) {
	  logger.error(e.getMessage());
	}
  }

  private int getInvoiceCount() {
	return this.invoiceCount;
  }

  private void setInvoiceCount(int invoiceCount) {
	this.invoiceCount = invoiceCount;
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

  private boolean setInvoiceIds(ResultSet rs) throws SQLException {
	boolean hasRecords = false;

	// If receive is different from the required amount of invoices
	if (getRecordCount(rs) != getInvoiceCount()) {
	  setInvoiceCount(getRecordCount(rs));
	}

	if (getInvoiceCount() != 0) {
	  this.invoiceIds = new int[getInvoiceCount()];
	  while (rs.next()) {
		this.invoiceIds[rs.getRow() - 1] = rs.getInt("cs.cs_bill_cdemo_sk");
	  }
	  hasRecords = true;
	}
	logger.debug("Invoice-IDs: " + getInvoiceCount());

	return hasRecords;
  }

  private long getLatestTimeStamp() {
	return latestTimeStamp;
  }

  private void setLatestTimeStamp(long latestTimeStamp) {
	this.latestTimeStamp = latestTimeStamp;
  }

  // --------------------------------------

  // ######################################
  // +++++++++++ Class-Methods ++++++++++++
  // ######################################
  public Document[] getDataFromSource() {
	Document[] docs = null;
	ResultSet rs = null;
	InvoiceDataFormat formatter = null;
	int invoicePositionsCount = 0;

	logger.info("DataSource-Setup:\t" + getDurationSinceLastTimeStamp() + "ms.");
	try {
	  if (setInvoiceIds(getRandomInvoiceIds())) {
		logger.info("Random Invoice-IDs:\t" + getDurationSinceLastTimeStamp() + "ms.");
		rs = getInvoices();
		logger.info("Invoice-Details:\t" + getDurationSinceLastTimeStamp() + "ms.");
		invoicePositionsCount = getRecordCount(rs);
		if (invoicePositionsCount != 0) {
		  formatter = new InvoiceDataFormat(getInvoiceCount());
		  logger.info("Total DataSource:\t" + (System.currentTimeMillis() - START_TIME_STAMP) + "ms.");
		  docs = formatter.formatDataFromResultSet(rs);
		} else
		  logger.error("No Invoice-Positions available!");
	  } else
		logger.error("No Invoices available!");
	  logger.debug("Invoice-Positions: " + invoicePositionsCount);
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

	return docs;
  }

  private ResultSet getRandomInvoiceIds() throws SQLException {
	Statement statement = null;
	ResultSet rs = null;
	String sql = getQueryInvoiceId();

	statement = getConnection().createStatement();
	rs = statement.executeQuery(sql);

	return rs;
  }

  private ResultSet getInvoices() throws SQLException {
	Statement statement = null;
	ResultSet rs = null;
	String sql = getQueryInvoiceDetail();

	statement = getConnection().createStatement();
	rs = statement.executeQuery(sql);

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
	sql += "(SELECT cs_bill_cdemo_sk ";
	sql += "FROM catalog_sales GROUP BY cs_bill_cdemo_sk HAVING COUNT(cs_bill_cdemo_sk) >= " + InvoiceDataProcessing.POSITIONS_PER_INVOICE + ") AS cs ";
	sql += "ORDER BY RAND() LIMIT " + InvoiceDataProcessing.INVOICE_COUNT + ";";
	logger.debug("/-> Query used for Invoice-IDs: " + sql);

	return sql;
  }

  private String getQueryInvoiceDetail() {
	String sql;

	/*
	 * W/O limiting positions per invoice sql =
	 * "SELECT c.c_customer_sk, c.c_first_name, c.c_last_name, "; sql +=
	 * "ca.ca_street_name, ca.ca_street_type, ca.ca_street_number, ca.ca_zip, ca.ca_county, ca.ca_state, ca.ca_country, "
	 * ; sql += "cs.cs_bill_cdemo_sk, "; sql +=
	 * "i.i_item_sk, i.i_category, i.i_class, i.i_brand, i.i_product_name, i.i_units, "
	 * ; sql += "cs.cs_quantity, cs.cs_list_price, cs.cs_ext_list_price "; sql
	 * +=
	 * "FROM catalog_sales AS cs JOIN customer AS c ON c.c_customer_sk = cs.cs_bill_customer_sk "
	 * ; sql +=
	 * "JOIN customer_address AS ca ON ca.ca_address_sk = cs.cs_bill_addr_sk ";
	 * sql += "JOIN item AS i ON i.i_item_sk = cs.cs_item_sk "; sql +=
	 * "WHERE cs.cs_bill_cdemo_sk IN (" + getInvoiceIds() + ") "; sql +=
	 * "ORDER BY cs.cs_bill_cdemo_sk, c.c_customer_sk;";
	 * logger.debug("/-> Query used for Invoices: " + sql);
	 */

	sql = "SELECT c.c_customer_sk, c.c_first_name, c.c_last_name, ";
	sql += "ca.ca_street_name, ca.ca_street_type, ca.ca_street_number, ca.ca_zip, ca.ca_county, ca.ca_state, ca.ca_country, ";
	sql += "cs.cs_bill_cdemo_sk, ";
	sql += "i.i_item_sk, i.i_category, i.i_class, i.i_brand, i.i_product_name, i.i_units, ";
	sql += "cs.cs_quantity, cs.cs_list_price, cs.cs_ext_list_price ";
	sql += "FROM (";
	sql += "SELECT cs_bill_cdemo_sk, cs_quantity, cs_list_price, cs_ext_list_price, cs_bill_customer_sk, cs_bill_addr_sk, cs_item_sk, ";
	sql += "@rn := IF(@prev = cs_bill_cdemo_sk, @rn + 1, 1) AS rn, @prev := cs_bill_cdemo_sk ";
	sql += "FROM catalog_sales ";
	sql += "JOIN (SELECT @prev := NULL, @rn := 0) AS vars ";
	sql += "ORDER BY cs_bill_cdemo_sk";
	sql += ") AS cs JOIN customer AS c ON c.c_customer_sk = cs.cs_bill_customer_sk ";
	sql += "JOIN customer_address AS ca ON ca.ca_address_sk = cs.cs_bill_addr_sk ";
	sql += "JOIN item AS i ON i.i_item_sk = cs.cs_item_sk ";
	sql += "WHERE cs.cs_bill_cdemo_sk IN (" + getInvoiceIds() + ") AND ";
	sql += "cs.rn <= " + InvoiceDataProcessing.POSITIONS_PER_INVOICE + " ";
	sql += "ORDER BY cs.cs_bill_cdemo_sk, c.c_customer_sk, cs.rn;";
	logger.debug("/-> Query used for Invoices: " + sql);

	return sql;
  }

  private long getDurationSinceLastTimeStamp() {
	long duration;
	long currentTimeStamp;

	currentTimeStamp = System.currentTimeMillis();
	duration = currentTimeStamp - getLatestTimeStamp();
	setLatestTimeStamp(currentTimeStamp);

	return duration;
  }
  // --------------------------------------
}
