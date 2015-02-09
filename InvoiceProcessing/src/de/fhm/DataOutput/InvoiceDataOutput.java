package de.fhm.DataOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
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

  public void test(Document[] docs) {
	NullOutputStream dev0 = new NullOutputStream();

	try {
	  // In Anlehnung an:
	  // http://www.javaworld.com/article/2073394/java-xml/transparently-cache-xsl-transformations-with-jaxp.html
	  Source xsl = new StreamSource(InvoiceDataProcessing.PATH_TO_XSL_FILE);
	  TransformerFactory transFact = TransformerFactory.newInstance();
	  Templates cachedXSL = transFact.newTemplates(xsl);

	  for (Document doc : docs) {
		FopFactory fopFactory = FopFactory.newInstance();
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, dev0);

		Transformer transformer = cachedXSL.newTransformer();
		Source xml = new DOMSource(doc);
		Result result = new SAXResult(fop.getDefaultHandler());
		transformer.transform(xml, result);

		dev0.close();
	  }
	} catch (FOPException | TransformerException | IOException e) {
	  logger.error(e.getMessage());
	}

	// for (int i = 0; i < docs.length; i++) {
	// try {
	// printDocument(docs[i], System.out);
	// } catch (IOException | TransformerException e) {
	// logger.error(e.getMessage());
	// }
	// }
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
