package de.fhm.DataProcessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.fhm.Database.DataConnection;

public class DocumentGenerator {
  private static final int AMNT_INVOICES = 10;
  private static final Logger logger = LogManager.getLogger(DocumentGenerator.class);

  public static void main(String[] args) {
	DataConnection source = new DataConnection(AMNT_INVOICES);

	logger.trace("Startup Invoice Processing!");
	source.startDataSourceProcess();

	
  }

}
