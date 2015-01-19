package de.fhm.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataConnection {
  private Connection connection;
  private int amntInvoices;

  private static final Logger logger = LogManager.getLogger(DataConnection.class);

  /**
   * Konstruktur übernimmt die Anzahl zu erstellender Rechnungen.
   * 
   * @param amntInvoices
   */
  public DataConnection(int amntInvoices) {
	this.amntInvoices = amntInvoices;
  }

  /**
   * Standard-Konstruktur setzt die Anzahl zu erstellender Rechnungen auf 1.
   * 
   * @param amntInvoices
   */
  public DataConnection() {
	this.amntInvoices = 1;
  }

  /**
   * Start-Methode für Datenbeschaffung. 1) Instanziiert eine Verbindung zur
   * Datenquelle 2) Startet Abfrage der Rechnungs-Nummern 3) Startet Abfrage der
   * Rechnungs-Details
   * 
   */
  public void startDataSourceProcess() {
	ResultSet rs = null;
	try {
	  setConnection();
	  rs = getInvoiceIds();
	  
	  while(rs.next()) {
		logger.debug(rs.getInt(1));
	  }

	} catch (ClassNotFoundException | SQLException e) {
	  logger.error(e.getMessage());
	}

  }

  private void setConnection() throws ClassNotFoundException, SQLException {
	String driver = "com.mysql.jdbc.Driver";
	String url = "jdbc:mysql://localhost:3306/test";

	Class.forName(driver);
	this.connection = DriverManager.getConnection(url, "mh", "mh");
  }

  /**
   * Bezieht ein Set aus zufälligen Rechnungsnummern des Rechnungsbestandes.
   * 
   * @return
   * @throws SQLException 
   */
  private ResultSet getInvoiceIds() throws SQLException {
	String sql = getInvoiceIdTemplate();
	
	PreparedStatement ps = this.connection.prepareStatement(sql);
	ps.setInt(1, this.amntInvoices);
	
	logger.debug("'" + ps.toString() + "'");
	ResultSet rs = ps.executeQuery();

	return rs;
  }

  /**
   * Fragt Rechnungsdetails gegebener Rechnungsnummern ab.
   * 
   * @return
   */
  private ResultSet getInvoiceDetails() {
	ResultSet rs = null;

	String sql;
	sql = getInvoiceDetailsTemplate();

	// TODO:

	return rs;
  }

  /**
   * Gibt ein SQL-Statement zur Auswahl zufälliger Rechnungsnummern zurück.
   * 
   * @return
   */
  private String getInvoiceIdTemplate() {
	String sql;

	sql = "SELECT cs.cs_bill_cdemo_sk FROM ";
	sql += "(SELECT DISTINCT(cs_bill_cdemo_sk) FROM catalog_sales) AS cs ";
	sql += "ORDER BY RAND() LIMIT ?;";

	return sql;
  }

  /**
   * Gibt ein SQL-Statement zur Abfrage von Rechnungsdetails zurück.
   * 
   * @return
   */
  private String getInvoiceDetailsTemplate() {
	String sql = "";

	return sql;
  }

}
