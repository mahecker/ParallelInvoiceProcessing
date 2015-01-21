package de.fhm.DataProcessing;

import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.fhm.Database.DataSource;

public class DocumentGenerator {
  private static final int AMNT_INVOICES = 1000; // Max: 153.252
  private static final Logger logger = LogManager.getLogger(DocumentGenerator.class);
  private static final long INITIALIZATION_TIME = System.currentTimeMillis();

  public static void main(String[] args) {
	DataSource source = new DataSource(AMNT_INVOICES);
	long start;
	ResultSet rs = null;

	logger.trace("--------------------------------------");
	logger.trace("### Starting Invoice Processing... ###");
	logger.trace("--------------------------------------");

	logger.trace("/-> Start generating Data...");
	start = System.currentTimeMillis();
	rs = source.generateData();
	logger.trace("/-> Finish generating Data: " + (System.currentTimeMillis() - start) + " ms.");

	logger.trace("-------------------------------------------");
	logger.trace("### Ending Invoice Processing: " + (System.currentTimeMillis() - INITIALIZATION_TIME) + " ms. ###");
	logger.trace("-------------------------------------------");

  }

}
