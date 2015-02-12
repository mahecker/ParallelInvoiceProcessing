package de.fhm.DataOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import de.fhm.DataProcessing.InvoiceDataProcessing;
import de.fhm.DataSource.InvoiceDataSourceTask;

public class InvoiceDataOutputTask extends RecursiveAction {
  // ######################################
  // +++++++++ Member-Variables +++++++++++
  // ######################################
  private static final long serialVersionUID = 1L;
  private long latestTimeStamp;
  private Document[] docs = null;

  private static final long START_TIME_STAMP = System.currentTimeMillis();
  private static final Logger logger = LogManager.getLogger(InvoiceDataOutput.class);
  
  // --------------------------------------
  
  // ######################################
  // +++++++++++ Constructors +++++++++++++
  // ######################################
  public InvoiceDataOutputTask(Document[] docs) {
	setLatestTimeStamp(START_TIME_STAMP);
	logger.info("Anzahl Documente in Constructor: " + docs.length);
	this.docs = docs;
  }
  
  // --------------------------------------
  
  // ######################################
  // +++++++++ Get-/Set-Methods +++++++++++
  // ######################################
  public long getLatestTimeStamp() {
	return latestTimeStamp;
  }

  public void setLatestTimeStamp(long latestTimeStamp) {
	this.latestTimeStamp = latestTimeStamp;
  }
  
  // --------------------------------------

  // ######################################
  // +++++++++++ Class-Methods ++++++++++++
  // ######################################
  
  @Override
  protected void compute() {
	int n = InvoiceDataProcessing.INVOICES_PER_TASK_OUTPUT;
	
	if (docs.length <= n) {
	  outputInvoices(docs);
	} else {
	  List<InvoiceDataOutputTask> tasks = new ArrayList<InvoiceDataOutputTask>();
	  while (docs.length > n) {
		Document[] partial = new Document[n];
		Document[] rest = new Document[(docs.length - n)];

		System.arraycopy(docs, 0, partial, 0, n);
		System.arraycopy(docs, n, rest, 0, rest.length);
		docs = rest;

		InvoiceDataOutputTask task = new InvoiceDataOutputTask(partial);
		task.fork();
		tasks.add(task);
	  }
	  for (InvoiceDataOutputTask task : tasks) {
		task.join();
	  }

	}
  }
  
  private void outputInvoices(Document[] docs) {
	NullOutputStream dev0 = new NullOutputStream();
	Templates cachedXSLT;
	FopFactory fopFactory;
	
	try {
	  cachedXSLT = getCachedXSLT();
	  fopFactory = FopFactory.newInstance();
	  logger.info("Anzahl Dokumente: " + docs.length);
	  logger.info("DataOutput-Setup:\t" + getDurationSinceLastTimeStamp() + "ms.");
	  for (Document doc : docs) {
		try {
		  Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, dev0);
		  Transformer transformer = cachedXSLT.newTransformer();
		  Source xml = new DOMSource(doc);
		  Result result = new SAXResult(fop.getDefaultHandler());
		  transformer.transform(xml, result);
		} finally {
		  dev0.close();
		}
	  }
	} catch (FOPException | TransformerException | IOException e) {
	  logger.error(e.getMessage());
	}
	
	logger.info("Generating PDFs:\t" + getDurationSinceLastTimeStamp() + "ms.");
	logger.info("Total DataOutput:\t" + (System.currentTimeMillis() - START_TIME_STAMP) + "ms.");
	
  }

  // In Anlehnung an:
  // http://www.javaworld.com/article/2073394/java-xml/transparently-cache-xsl-transformations-with-jaxp.html
  private Templates getCachedXSLT() throws TransformerConfigurationException {
	Source XSLT = null;
	TransformerFactory factory = null;
	Templates cachedXSLT = null;

	XSLT = new StreamSource(InvoiceDataProcessing.PATH_TO_XSL_FILE);
	factory = TransformerFactory.newInstance();
	cachedXSLT = factory.newTemplates(XSLT);

	return cachedXSLT;
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
