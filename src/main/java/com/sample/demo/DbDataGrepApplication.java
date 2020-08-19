package com.sample.demo;

import com.opencsv.CSVWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.derby.drda.NetworkServerControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DbDataGrepApplication extends SpringBootServletInitializer {

	/**
	 * logger
	 */
	private static Logger logger = LoggerFactory.getLogger(DbDataGrepApplication.class);

	/**
	 * The constant server.
	 */
	private static NetworkServerControl server;

	/**
	 * The constant properties.
	 */
	private static Properties properties;

	/**
	 * The constant file count.
	 */
	private static int fileCount;

	public static void main(String[] args) {
		startDbDownload();
		SpringApplication.run(DbDataGrepApplication.class, args);
	}

//	@Override
//	public void onStartup(ServletContext servletContext) throws ServletException {
//		startDbDownload();
//		super.onStartup(servletContext);
//	}

	/**
	 * Start DB table raw download
	 *
	 */
	private static void startDbDownload() {
		if (server == null) {
			try {
				String jdbcUrl = getProperty("spring.datasource.url",
						"jdbc:derby://localhost:1527/wasupDB;");
				String driverClass = getProperty("spring.datasource.driver-class-name",
						"org.apache.derby.jdbc.ClientDriver");
				String ddlAutoCreate = getProperty("spring.jpa.hibernate.ddl-auto", "none");
				String username = getProperty("spring.datasource.username", "wasup");
				String password = getProperty("spring.datasource.password", "wasup");

				if (driverClass.contains("mysql") || driverClass.contains("mariadb")) {
					Class.forName(driverClass).newInstance();
					Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
					Statement stmt = connection.createStatement();

					ResultSet results = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + connection.getCatalog() + "';");

					List<String> tableNames = new ArrayList<>();
					while (results.next()) {
						tableNames.add(results.getString("table_name").toLowerCase());
					}
					fileCount = tableNames.size();

					// generated csv file if table exists.
					if (fileCount > 0) {
						for (String tableName : tableNames) {
							System.err.println(tableName);
						}
					}
				} else if (driverClass.contains("postgres")) {
					Class.forName(driverClass).newInstance();
					Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
					Statement stmt = connection.createStatement();

					ResultSet results = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema = 'public' AND table_catalog = '" + connection.getCatalog() + "';");

					List<String> tableNames = new ArrayList<String>();
					while (results.next()) {
						tableNames.add(results.getString("table_name").toUpperCase());
					}
					fileCount = tableNames.size();

					// generated csv file if table exists.
					if (fileCount > 0) {
						for (String tableName : tableNames) {
							System.err.println(tableName);
						}
					}
				} else if (driverClass.contains("derby")) {
					String port = System.getProperty("wasup.derby.server.port", "1527");

					// Start Derby DB as network server mode
					server = new NetworkServerControl(InetAddress.getByName("0.0.0.0"), Integer.parseInt(port));
					server.start(new PrintWriter(System.out));

					Class.forName(driverClass).newInstance();
					Connection connection = DriverManager.getConnection(jdbcUrl);
					Statement stmt = connection.createStatement();
					ResultSet results = stmt.executeQuery("SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE = 'T'");

					List<String> tableNames = new ArrayList<String>();
					while (results.next()) {
						tableNames.add(results.getString("TABLENAME").toUpperCase());
					}
					fileCount = tableNames.size();

					// generated csv file if table exists.
					if (fileCount > 0) {
						for (String tableName : tableNames) {
							generatedCsvFiles(tableName);
						}
					}

					generatedCsvFiles("History");


				}

				logger.info("Be sure to check if " + fileCount +  " files have been downloaded.");
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while startDbDownload()", e);
				e.printStackTrace();
			}
		}
	}

	private static void generatedCsvFiles(String tableName) throws Exception {
		String home = System.getProperty("user.home");
		String csvFileName = tableName + ".csv";
		String downloadPath = home + getProperty("wasup.db.download.path", home) + csvFileName;

		CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(downloadPath),
				"EUC-KR"));
		String[] entries = "a,b,c".split(",");

		writer.writeNext(entries);

		writer.close();
	}

	/**
	 * Load properties.
	 */
	private static String getProperty(String key, String defaultValue) throws Exception {
		if (properties == null) {
			properties = new Properties();
			properties.load(DbDataGrepApplication.class.getClassLoader().getResourceAsStream("application.properties"));
		}

		String value = System.getProperty(key);

		if (value == null || value.equals("")) {
			value = properties.getProperty(key, defaultValue);
		}

		return value;
	}

}