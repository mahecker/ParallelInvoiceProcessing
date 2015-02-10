package de.fhm.DataFormat;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class InvoiceDataFormat {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private int invoiceCount;
  private Document[] docs;
  private Document doc;
  private ResultSetMetaData rsmd;

  private static final int DEFAULT_INVOICE_COUNT = 1;
  private static final Logger logger = LogManager.getLogger(InvoiceDataFormat.class);

  // --------------------------------------

  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public InvoiceDataFormat() {
	setInvoiceCount(DEFAULT_INVOICE_COUNT);
  }

  public InvoiceDataFormat(int invoiceCount) {
	setInvoiceCount(invoiceCount);
  }

  // --------------------------------------

  // ######################################
  // +++++++++ Get-/Set-Methods +++++++++++
  // ######################################
  public int getInvoiceCount() {
	return invoiceCount;
  }

  public void setInvoiceCount(int invoiceCount) {
	this.invoiceCount = invoiceCount;
  }

  // --------------------------------------

  // ######################################
  // +++++++++++ Class-Methods ++++++++++++
  // ######################################

  // In Anlehnung an:
  // http://www.developer.com/xml/article.php/3329001/Converting-JDBC-Result-Sets-to-XML.htm
  public Document[] formatDataFromResultSet(ResultSet rs) {
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

	docs = new Document[getInvoiceCount()];
	try {
	  factory = DocumentBuilderFactory.newInstance();
	  builder = factory.newDocumentBuilder();
	  rsmd = rs.getMetaData();

	  hasNextInvoicePosition = rs.next();
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
	  logger.error("TEST: " + e.getMessage());
	}

	return docs;
  }

  private Element getCustomerFromResultSet(ResultSet rs) throws DOMException, SQLException {
	Element customer = doc.createElement("Customer");

	//Query-Structure is designed to define the customer within the first 11 Columns
	for (int i = 1; i <= 11; i++) {
	  Element node = doc.createElement(rsmd.getColumnName(i));
	  node.appendChild(doc.createTextNode(rs.getObject(i).toString()));
	  customer.appendChild(node);
	}

	return customer;
  }

  private Element getPositionFromResultSet(ResultSet rs) throws DOMException, SQLException {
	Element position = doc.createElement("Position");

	//Query-Structure is designed to define the Position off the 12th to the last Column	
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

  // --------------------------------------
}