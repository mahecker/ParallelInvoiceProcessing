package de.fhm.DataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.fhm.DataOutput.InvoiceDataOutputTask;
import de.fhm.DataProcessing.InvoiceDataProcessing;

public class InvoiceDataSourceTask extends RecursiveAction {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private int[] ids = null;;
  private Document doc = null;
  private ResultSetMetaData rsmd = null;
  private long latestTimeStamp;

  private static final long START_TIME_STAMP = System.currentTimeMillis();
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(InvoiceDataSourceTask.class);

  // --------------------------------------

  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public InvoiceDataSourceTask(int[] ids) {
	setLatestTimeStamp(START_TIME_STAMP);
	logger.info("Anzahl IDs in Constructor: " + ids.length);
	setIds(ids);
  }

  // --------------------------------------

  // ######################################
  // +++++++++ Get-/Set-Methods +++++++++++
  // ######################################
  private int[] getIds() {
	return this.ids;
  }

  private void setIds(int[] ids) {
	this.ids = ids;
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
  @Override
  protected void compute() {
	int n = InvoiceDataProcessing.INVOICES_PER_TASK_SOURCE;
	int[] ids = getIds();

	if (ids.length <= n) {
	  processInvoices(ids);
	} else {
	  List<InvoiceDataSourceTask> tasks = new ArrayList<InvoiceDataSourceTask>();
	  while (ids.length > n) {
		int[] partial = new int[n];
		int[] rest = new int[(ids.length - n)];

		System.arraycopy(ids, 0, partial, 0, n);
		System.arraycopy(ids, n, rest, 0, rest.length);
		ids = rest;

		InvoiceDataSourceTask task = new InvoiceDataSourceTask(partial);
		task.fork();
		tasks.add(task);
	  }
	  for (InvoiceDataSourceTask task : tasks) {
		task.join();
	  }

	}

  }

  private void processInvoices(int[] ids) {
	Document[] docs = null;
	Connection connection = null;
	Statement statement = null;
	ResultSet rs = null;

	logger.info("Anzahl IDs: " + ids.length);

	String sql = getQueryInvoiceDetail(ids);

	try {
	  connection = getConnection();
	  statement = connection.createStatement();
	  rs = statement.executeQuery(sql);

	  // TODO: Weiterverarbeiten zu XML
	  docs = formatDataFromResultSet(rs, ids.length);
	  InvoiceDataOutputTask task = new InvoiceDataOutputTask(docs);
	  task.fork();
	  task.join();

	  // task.invoke();
	} catch (SQLException e) {
	  logger.error(e.getMessage());
	} finally {
	  try {
		if (!connection.isClosed()) {
		  connection.close();
		}
	  } catch (SQLException e) {
		logger.error(e.getMessage());
	  }
	}
  }

  public Connection getConnection() {
	Connection connection = null;

	try {
	  Class.forName(InvoiceDataSource.JDBC_CLASS);
	  connection = DriverManager.getConnection(InvoiceDataSource.URL, InvoiceDataSource.USER, InvoiceDataSource.PWD);
	} catch (ClassNotFoundException | SQLException e) {
	  logger.error(e.getMessage());
	}

	return connection;
  }

  private String getQueryInvoiceDetail(int[] ids) {
	String sql;

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
	sql += "WHERE cs_bill_cdemo_sk <> 0 AND cs_bill_cdemo_sk IS NOT NULL AND cs_bill_cdemo_sk REGEXP ('^[0-9]') AND ";
	sql += "cs_bill_customer_sk <> 0 AND cs_bill_customer_sk IS NOT NULL AND cs_bill_customer_sk REGEXP ('^[0-9]') AND ";
	sql += "cs_bill_addr_sk <> 0 AND cs_bill_addr_sk IS NOT NULL AND cs_bill_addr_sk REGEXP ('^[0-9]') AND ";
	sql += "cs_item_sk <> 0 AND cs_item_sk IS NOT NULL AND cs_item_sk REGEXP ('^[0-9]') ";
	sql += "ORDER BY cs_bill_cdemo_sk ";
	sql += ") AS cs JOIN customer AS c ON c.c_customer_sk = cs.cs_bill_customer_sk ";
	sql += "JOIN customer_address AS ca ON ca.ca_address_sk = cs.cs_bill_addr_sk ";
	sql += "JOIN item AS i ON i.i_item_sk = cs.cs_item_sk ";
	sql += "WHERE cs.cs_bill_cdemo_sk IN (" + getStringFromIntArray(ids) + ") AND ";
	sql += "cs.rn <= " + InvoiceDataProcessing.POSITIONS_PER_INVOICE + " ";
	sql += "ORDER BY cs.cs_bill_cdemo_sk, c.c_customer_sk, cs.rn;";
	// logger.debug("Invoices-Query: " + sql);

	return sql;
  }

  private String getStringFromIntArray(int[] ids) {
	String dummy = "";

	if (ids.length > 0) {
	  dummy = Integer.toString(ids[0]);
	  if (ids.length > 1) {
		for (int i = 1; i < ids.length; i++) {
		  dummy += "," + Integer.toString(ids[i]);
		}
	  }
	}

	return dummy;
  }

  // In Anlehnung an:
  // http://www.developer.com/xml/article.php/3329001/Converting-JDBC-Result-Sets-to-XML.htm
  public Document[] formatDataFromResultSet(ResultSet rs, int invoiceCount) {
	Document[] docs = null;
	DocumentBuilderFactory factory = null;
	DocumentBuilder builder = null;
	Element customer = null;
	Element positions = null;
	Element position = null;

	boolean nextInvoice = true;
	boolean hasNextInvoicePosition;
	int currentInvoiceID = 0;
	int nextInvoiceID = 0;
	int currentPosition = 0;
	double sumListPrice = 0;

	docs = new Document[invoiceCount];
	try {
	  factory = DocumentBuilderFactory.newInstance();
	  builder = factory.newDocumentBuilder();
	  rsmd = rs.getMetaData();

	  hasNextInvoicePosition = rs.next();
	  logger.info("DataFormat-Setup:\t" + getDurationSinceLastTimeStamp() + "ms.");
	  while (hasNextInvoicePosition) {
		if (nextInvoice) {
		  doc = builder.newDocument();
		  customer = getCustomerFromResultSet(rs);
		  doc.appendChild(customer);
		  positions = doc.createElement("Positions");
		  customer.appendChild(positions);
		  currentInvoiceID = rs.getInt("cs.cs_bill_cdemo_sk");
		}
		position = getPositionFromResultSet(rs);
		positions.appendChild(position);
		sumListPrice += rs.getDouble("cs.cs_ext_list_price");

		hasNextInvoicePosition = rs.next();
		if (hasNextInvoicePosition) {
		  nextInvoiceID = rs.getInt("cs.cs_bill_cdemo_sk");
		}

		if ((hasNextInvoicePosition && (currentInvoiceID != nextInvoiceID)) || !(hasNextInvoicePosition)) {
		  positions = addTotalsAndTax(positions, sumListPrice);
		  customer.appendChild(positions);
		  docs[currentPosition] = doc;
		  currentPosition++;
		  sumListPrice = 0;

		  if (hasNextInvoicePosition) {
			nextInvoice = true;
		  }
		} else {
		  nextInvoice = false;
		}
	  }
	} catch (SQLException | ParserConfigurationException e) {
	  logger.error(e.getMessage());
	}
	logger.info("Format to XML:\t\t" + getDurationSinceLastTimeStamp() + "ms.");

	return docs;
  }

  private Element getCustomerFromResultSet(ResultSet rs) throws DOMException, SQLException {
	Element customer = doc.createElement("Customer");

	// Query-Structure is designed to define the customer within the first 11
	// Columns
	for (int i = 1; i <= 11; i++) {
	  Element node = doc.createElement(rsmd.getColumnName(i));
	  node.appendChild(doc.createTextNode(rs.getObject(i).toString()));
	  customer.appendChild(node);
	}

	return customer;
  }

  private Element getPositionFromResultSet(ResultSet rs) throws DOMException, SQLException {
	Element position = doc.createElement("Position");

	// Query-Structure is designed to define the Position off the 12th to the
	// last Column
	for (int i = 12; i <= rsmd.getColumnCount(); i++) {
	  Element node = doc.createElement(rsmd.getColumnName(i));
	  node.appendChild(doc.createTextNode(rs.getObject(i).toString()));
	  position.appendChild(node);
	}

	return position;
  }

  private Element addTotalsAndTax(Element positions, double sumListPrice) {
	Element netTotal = doc.createElement("cs_net_sum");
	Element salesTax = doc.createElement("cs_tax");
	Element grossTotal = doc.createElement("cs_total_sum");

	netTotal.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(sumListPrice)));
	salesTax.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(sumListPrice * 0.19)));
	grossTotal.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(sumListPrice * 1.19)));

	positions.appendChild(netTotal);
	positions.appendChild(salesTax);
	positions.appendChild(grossTotal);

	return positions;
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