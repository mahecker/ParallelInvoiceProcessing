package de.fhm.DataProcessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import de.fhm.DataOutput.InvoiceDataOutput;
import de.fhm.DataSource.InvoiceDataSource;

public class InvoiceDataProcessing {
  private static final long START_TIME_STAMP = System.currentTimeMillis();
  public static final int INVOICE_COUNT = 5; // Max-ALL: 153.252 (Max-5POS: 139.373)
  public static final int POSITIONS_PER_INVOICE = 5;
  public static final String PATH_TO_XSL_FILE = "./src/de/fhm/DataOutput/Invoice_Template.xsl";
  private static final Logger logger = LogManager.getLogger(InvoiceDataProcessing.class);
  
  private static long latestTimeStamp = START_TIME_STAMP;
  
  public static void main(String[] args) {
	Document[] docs = null;
	long duration;
	long currentTimeStamp;

	logger.info("Processing " + INVOICE_COUNT + " invoice(s) (" + POSITIONS_PER_INVOICE + " Pos.).");
	currentTimeStamp = System.currentTimeMillis();
	duration = currentTimeStamp - latestTimeStamp;
	latestTimeStamp = currentTimeStamp;
	logger.info("JVM-Setup:\t\t" + duration + "ms.");
	
	// Measurement starts on instantiating a new class-object
	InvoiceDataSource source = new InvoiceDataSource();
	docs = source.getDataFromSource();

	// Measurement starts on instantiating a new class-object
	InvoiceDataOutput output = new InvoiceDataOutput();
	output.outputDataFromDocs(docs);

	currentTimeStamp = System.currentTimeMillis();
	duration = currentTimeStamp - START_TIME_STAMP;
	logger.info("Total Process:\t\t" + duration + "ms.");
  }
}
