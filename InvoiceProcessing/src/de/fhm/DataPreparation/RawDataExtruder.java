package de.fhm.DataPreparation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RawDataExtruder {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private int invoiceCount;
  private Document[] invoicesXML;

  private static final int DEFAULT_INVOICE_COUNT = 1;
  private static final Logger logger = LogManager.getLogger(RawDataExtruder.class);

  // --------------------------------------

  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public RawDataExtruder() {
	setInvoiceCount(DEFAULT_INVOICE_COUNT);
  }

  public RawDataExtruder(int invoiceCount) {
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
  public Document[] test(ResultSet rs) {
	DocumentBuilderFactory factory = null;
	DocumentBuilder builder = null;
	Document doc = null;
	Element customer = null;
	Element positions = null;
	int invoiceID = 0;
	int currentSlot = 0;
	double netTotal = 0;
	invoicesXML = new Document[getInvoiceCount()];

	try {
	  factory = DocumentBuilderFactory.newInstance();
	  builder = factory.newDocumentBuilder();

	  while (rs.next()) {
		if (rs.getInt("cs.cs_bill_cdemo_sk") != invoiceID) {
		  if (!rs.isFirst()) {
			Element netSum = doc.createElement("cs_net_sum");
			Element tax = doc.createElement("cs_tax");
			Element totalSum = doc.createElement("cs_total_sum");

			netSum.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal)));
			tax.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal * 0.19)));
			totalSum.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal * 1.19)));

			positions.appendChild(netSum);
			positions.appendChild(tax);
			positions.appendChild(totalSum);

			invoicesXML[currentSlot] = doc;
			currentSlot += 1;
			netTotal = 0;
		  }
		  doc = builder.newDocument();
		  customer = doc.createElement("Customer");
		  positions = doc.createElement("Positions");
		  doc.appendChild(customer);
		  invoiceID = rs.getInt("cs.cs_bill_cdemo_sk");

		  for (int i = 1; i <= 11; i++) {
			Element node = doc.createElement(rs.getMetaData().getColumnName(i));

			node.appendChild(doc.createTextNode(rs.getObject(i).toString()));
			customer.appendChild(node);
		  }
		  customer.appendChild(positions);

		}

		Element position = doc.createElement("Position");
		positions.appendChild(position);
		for (int i = 12; i <= rs.getMetaData().getColumnCount(); i++) {
		  Element node = doc.createElement(rs.getMetaData().getColumnName(i));
		  node.appendChild(doc.createTextNode(rs.getObject(i).toString()));
		  position.appendChild(node);
		}
		netTotal += rs.getDouble("cs.cs_ext_list_price");
	  }
	  Element netSum = doc.createElement("cs_net_sum");
	  Element tax = doc.createElement("cs_tax");
	  Element totalSum = doc.createElement("cs_total_sum");

	  netSum.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal)));
	  tax.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal * 0.19)));
	  totalSum.appendChild(doc.createTextNode(new DecimalFormat("#.00").format(netTotal * 1.19)));

	  positions.appendChild(netSum);
	  positions.appendChild(tax);
	  positions.appendChild(totalSum);

	  invoicesXML[currentSlot] = doc;
	  currentSlot += 1;
	  netTotal = 0;
	} catch (SQLException | ParserConfigurationException e) {
	  logger.error(e.getMessage());
	}
	return invoicesXML;
  }

  // --------------------------------------
}