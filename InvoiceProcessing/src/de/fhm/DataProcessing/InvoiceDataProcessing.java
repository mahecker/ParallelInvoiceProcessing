package de.fhm.DataProcessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import de.fhm.DataOutput.InvoiceDataOutput;
import de.fhm.DataSource.InvoiceDataSource;

public class InvoiceDataProcessing {
  public static final int INVOICE_COUNT = 10; // Max: 153.252 (Max-Heap: ~147.250)
  public static final int POSITIONS_PER_INVOICE = 5;
  public static final String PATH_TO_XSL_FILE = "./src/de/fhm/DataOutput/Invoice_Template.xsl";
  private static final Logger logger = LogManager.getLogger(InvoiceDataProcessing.class);
  private static final long INITIALIZATION_TIME = System.currentTimeMillis();

  public static void main(String[] args) {
	InvoiceDataSource source = new InvoiceDataSource();
	InvoiceDataOutput output = new InvoiceDataOutput();
	Document[] docs = null;
	long start;

	logger.trace("--------------------------------------");
	logger.trace("### Starting Invoice Processing... ###");
	logger.trace("--------------------------------------");

	logger.trace("/-> [START] Getting Data from Source...");
	start = System.currentTimeMillis();
	docs = source.getDataFromSource(start);
	output.outputDataFromDocs(docs);

	logger.trace("-------------------------------------------");
	logger.trace("### Ending Invoice Processing: " + (System.currentTimeMillis() - INITIALIZATION_TIME) + " ms. ###");
	logger.trace("-------------------------------------------");
  }
}
