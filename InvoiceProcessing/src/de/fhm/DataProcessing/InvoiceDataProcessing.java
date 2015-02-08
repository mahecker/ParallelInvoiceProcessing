package de.fhm.DataProcessing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import de.fhm.DataSource.InvoiceDataSource;

public class InvoiceDataProcessing {
  private static final int INVOICE_COUNT = 2; // Max: 153.252 (Max-Heap: ~147.250)
  private static final Logger logger = LogManager.getLogger(InvoiceDataProcessing.class);
  private static final long INITIALIZATION_TIME = System.currentTimeMillis();

  public static void main(String[] args) {
	InvoiceDataSource source = new InvoiceDataSource(INVOICE_COUNT);
	Document[] docs = null;
	long start;

	logger.trace("--------------------------------------");
	logger.trace("### Starting Invoice Processing... ###");
	logger.trace("--------------------------------------");

	logger.trace("/-> [START] Getting Data from Source...");
	start = System.currentTimeMillis();
	docs = source.generateData(start);

	for (int i = 0; i < docs.length; i++) {
	  try {
		printDocument(docs[i], System.out);
	  } catch (IOException | TransformerException e) {
		logger.error(e.getMessage());
	  }
	}

	logger.trace("-------------------------------------------");
	logger.trace("### Ending Invoice Processing: " + (System.currentTimeMillis() - INITIALIZATION_TIME) + " ms. ###");
	logger.trace("-------------------------------------------");

  }

  // Quelle:
  // http://stackoverflow.com/questions/2325388/java-shortest-way-to-pretty-print-to-stdout-a-org-w3c-dom-document
  private static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
	TransformerFactory tf = TransformerFactory.newInstance();
	Transformer transformer = tf.newTransformer();
	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
  }

}
