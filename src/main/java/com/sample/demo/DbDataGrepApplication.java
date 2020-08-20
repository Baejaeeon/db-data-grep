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
						tableNames.add(results.getString("table_name").toUpperCase());
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
							//generatedCsvFiles(tableName);
						}
					}

					generatedCsvFiles(stmt, "HISTORY");
				}

				logger.info("Be sure to check if " + fileCount +  " files have been downloaded.");
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while startDbDownload()", e);
				e.printStackTrace();
			}
		}
	}

	private static void generatedCsvFiles(Statement stmt, String tableName) throws Exception {
		String home = System.getProperty("user.home");
		String csvFileName = tableName + ".csv";
		String downloadPath = home + getProperty("wasup.db.download.path", home) + csvFileName;

		CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(downloadPath),
				"EUC-KR"));

		ResultSet rs = null;
		if (tableName.equals("ACCESS_CONTROL")) {
			rs = stmt.executeQuery("select * from access_control;");
		} else if (tableName.equals("ALERT")) {
			rs = stmt.executeQuery("select * from alert;");
		} else if (tableName.equals("APPLICATION")) {
			rs = stmt.executeQuery("select * from application;");
		} else if (tableName.equals("ATLASSIAN_SERVER")) {
			rs = stmt.executeQuery("select * from atlassian_server;");
		} else if (tableName.equals("CLUSTER")) {
			rs = stmt.executeQuery("select * from cluster;");
		} else if (tableName.equals("CONFIG_FILE")) {
			rs = stmt.executeQuery("select * from config_file;");
		} else if (tableName.equals("DATASOURCE")) {
			rs = stmt.executeQuery("select * from datasource;");
		} else if (tableName.equals("DOMAIN")) {
			rs = stmt.executeQuery("select * from domain;");
		} else if (tableName.equals("ENGINE")) {
			rs = stmt.executeQuery("select * from engine;");
		} else if (tableName.equals("HISTORY")) {
			rs = stmt.executeQuery("select * from history;");
		} else if (tableName.equals("HOST")) {
			rs = stmt.executeQuery("select * from host;");
		} else if (tableName.equals("HOST_ALARM")) {
			rs = stmt.executeQuery("select * from host_alarm;");
		} else if (tableName.equals("HOST_DETAIL")) {
			rs = stmt.executeQuery("select * from host_detail;");
		} else if (tableName.equals("HOST_MONITOR")) {
			rs = stmt.executeQuery("select * from host_monitor;");
		} else if (tableName.equals("HOST_ENGINES")) {
			rs = stmt.executeQuery("select * from host_engines;");
		} else if (tableName.equals("JVM_MONITOR")) {
			rs = stmt.executeQuery("select * from jvm_monitor;");
		} else if (tableName.equals("MEMBER")) {
			rs = stmt.executeQuery("select * from member;");
		} else if (tableName.equals("MEMBER_ROLES_DOMAIN")) {
			rs = stmt.executeQuery("select * from member_roles_domain;");
		} else if (tableName.equals("ROLE")) {
			rs = stmt.executeQuery("select * from role;");
		} else if (tableName.equals("SCOUTER_SERVER")) {
			rs = stmt.executeQuery("select * from scouter_server;");
		} else if (tableName.equals("SESSION_SERVER")) {
			rs = stmt.executeQuery("select * from session_server;");
		} else if (tableName.equals("SETTINGS")) {
			rs = stmt.executeQuery("select * from settings;");
		} else if (tableName.equals("SUBSCRIPTION")) {
			rs = stmt.executeQuery("select * from subscription;");
		} else if (tableName.equals("WEB_APP_SERVER")) {
			rs = stmt.executeQuery("select * from web_app_server;");
		} else if (tableName.equals("WEB_APP_SERVER_ALARM")) {
			rs = stmt.executeQuery("select * from web_app_server_alarm;");
		} else if (tableName.equals("WEB_APP_SERVERS_APPLICATION")) {
			rs = stmt.executeQuery("select * from web_app_servers_application;");
		} else if (tableName.equals("WEB_APP_SERVERS_DATASOURCE")) {
			rs = stmt.executeQuery("select * from web_app_servers_datasource;");
		} else if (tableName.equals("WEB_SERVER")) {
			rs = stmt.executeQuery("select * from web_server;");
		} else if (tableName.equals("WEB_SERVERS_ACCESS_CONTROL")) {
			rs = stmt.executeQuery("select * from web_servers_access_control;");
		} else if (tableName.equals("WEB_SERVERS_WEB_APP_SERVERS")) {
			rs = stmt.executeQuery("select * from web_servers_web_app_servers;");
		} else if (tableName.equals("WIZARD")) {
			rs = stmt.executeQuery("select * from wizard;");
		}

		//writer.writeNext(entries);
		writer.writeAll(rs, true);

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