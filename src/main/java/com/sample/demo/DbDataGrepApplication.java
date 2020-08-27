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

					ResultSet results = stmt
							.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + connection.getCatalog() + "';");

					List<String> tableNames = new ArrayList<>();
					while (results.next()) {
						tableNames.add(results.getString("table_name").toUpperCase());
					}
					fileCount = tableNames.size();

					// generated csv file if table exists.
					if (fileCount > 0) {
						for (String tableName : tableNames) {
							generatedCsvFiles(stmt, tableName);
						}
					}
				} else if (driverClass.contains("postgres")) {
					Class.forName(driverClass).newInstance();
					Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
					Statement stmt = connection.createStatement();

					ResultSet results = stmt
							.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema = 'public' AND table_catalog = '" + connection.getCatalog() + "';");

					List<String> tableNames = new ArrayList<String>();
					while (results.next()) {
						tableNames.add(results.getString("table_name").toUpperCase());
					}
					fileCount = tableNames.size();

					// generated csv file if table exists.
					if (fileCount > 0) {
						for (String tableName : tableNames) {
							generatedCsvFiles(stmt, tableName);
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
							generatedCsvFiles(stmt, tableName);
						}
					}
				}

				logger.info("Be sure to check if " + fileCount +  " files have been downloaded.");
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while startDbDownload()", e);
				e.printStackTrace();
			}
		}
	}

	private static void generatedCsvFiles(Statement stmt, String tableName) throws Exception {
		logger.info(":+:+:+:+:+:+:+:+: table Name : [{}]", tableName);
		String home = System.getProperty("user.home");
		String csvFileName = tableName + ".csv";
		String downloadPath = home + getProperty("wasup.db.download.path", home) + csvFileName;

		CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(downloadPath),
				"EUC-KR"));

		ResultSet rs = null;
		switch (tableName) {
			case "ACCESS_CONTROL":
				rs = stmt.executeQuery("select * from wasup.access_control");
				break;
			case "ALERT":
				rs = stmt.executeQuery("select * from wasup.alert");
				break;
			case "APPLICATION":
				rs = stmt.executeQuery("select * from wasup.application");
				break;
			case "ATLASSIAN_SERVER":
				rs = stmt.executeQuery("select * from wasup.atlassian_server");
				break;
			case "CLUSTER":
				rs = stmt.executeQuery("select * from wasup.cluster");
				break;
			case "CONFIG_FILE":
				rs = stmt.executeQuery("select * from wasup.config_file");
				break;
			case "DATASOURCE":
				rs = stmt.executeQuery("select * from wasup.datasource");
				break;
			case "DOMAIN":
				rs = stmt.executeQuery("select * from wasup.domain");
				break;
			case "ENGINE":
				rs = stmt.executeQuery("select * from wasup.engine");
				break;
			case "HISTORY":
				rs = stmt.executeQuery("select * from wasup.history");
				break;
			case "HOST":
				rs = stmt.executeQuery("select * from wasup.host");
				break;
			case "HOST_ALARM":
				rs = stmt.executeQuery("select * from wasup.host_alarm");
				break;
			case "HOST_DETAIL":
				rs = stmt.executeQuery("select * from wasup.host_detail");
				break;
			case "HOST_MONITOR":
				rs = stmt.executeQuery("select * from wasup.host_monitor");
				break;
			case "HOSTS_ENGINES":
				rs = stmt.executeQuery("select * from wasup.hosts_engines");
				break;
			case "JVM_MONITOR":
				rs = stmt.executeQuery("select * from wasup.jvm_monitor");
				break;
			case "MEMBER":
				rs = stmt.executeQuery("select * from wasup.member");
				break;
			case "MEMBERS_ROLES_DOMAINS":
				rs = stmt.executeQuery("select * from wasup.members_roles_domains");
				break;
			case "ROLE":
				rs = stmt.executeQuery("select * from wasup.role");
				break;
			case "SCOUTER_SERVER":
				rs = stmt.executeQuery("select * from wasup.scouter_server");
				break;
			case "SESSION_SERVER":
				rs = stmt.executeQuery("select * from wasup.session_server");
				break;
			case "SETTINGS":
				rs = stmt.executeQuery("select * from wasup.settings");
				break;
			case "SUBSCRIPTION":
				rs = stmt.executeQuery("select * from wasup.subscription");
				break;
			case "WEB_APP_SERVER":
				rs = stmt.executeQuery("select * from wasup.web_app_server");
				break;
			case "WEB_APP_SERVER_ALARM":
				rs = stmt.executeQuery("select * from wasup.web_app_server_alarm");
				break;
			case "WEB_APP_SERVERS_APPLICATIONS":
				rs = stmt.executeQuery("select * from wasup.web_app_servers_applications");
				break;
			case "WEB_APP_SERVERS_DATASOURCES":
				rs = stmt.executeQuery("select * from wasup.web_app_servers_datasources");
				break;
			case "WEB_SERVER":
				rs = stmt.executeQuery("select * from wasup.web_server");
				break;
			case "WEB_SERVERS_ACCESS_CONTROLS":
				rs = stmt.executeQuery("select * from wasup.web_servers_access_controls");
				break;
			case "WEB_SERVERS_WEB_APP_SERVERS":
				rs = stmt.executeQuery("select * from wasup.web_servers_web_app_servers");
				break;
			case "WIZARD":
				rs = stmt.executeQuery("select * from wasup.wizard");
				break;

			default:
				break;
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