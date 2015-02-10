package de.fhm.DataOutput;

import java.io.IOException;

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

public class InvoiceDataOutput {
  private static final Logger logger = LogManager.getLogger(InvoiceDataOutput.class);

  public void outputDataFromDocs(Document[] docs) {
	NullOutputStream dev0 = new NullOutputStream();
	Templates cachedXSLT;
	FopFactory fopFactory;

	try {
	  cachedXSLT = getCachedXSLT();
	  fopFactory = FopFactory.newInstance();
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
}
