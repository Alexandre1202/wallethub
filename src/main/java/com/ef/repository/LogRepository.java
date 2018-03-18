package com.ef.repository;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import com.ef.connectionfactory.WalletDB;
import com.ef.model.ParameterType;
import com.mysql.jdbc.util.ResultSetUtil;

public class LogRepository {
	
	private Connection connWallet;
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");

	public List<String> execute(Map<String, String> parserParameterMap) {
		List<String> queryAccessLogResultList = new ArrayList<>();
		List<String> logFileList = new ArrayList<>();
		logFileList = getLogFileList(parserParameterMap.get(ParameterType.ACCESSLOG.paramType()));
		if (logFileList != null && logFileList.size() > 0) {
			System.out.println("Insert started at " + new Date());
			try {
				Class.forName(WalletDB.DRIVER.connectionParam());
				connWallet = DriverManager.getConnection(
						WalletDB.JDBC_URL.connectionParam(), 
						WalletDB.USER.connectionParam(), 
						WalletDB.PASSWORD.connectionParam());
				System.out.println("Database connected at " + new Date());
				if (insertAccessLogList(logFileList) > 0) {
					System.out.println("Insert ended at " + new Date());					
					queryAccessLogResultList = queryLog(parserParameterMap);
					System.out.println("Query executed at " + new Date());					
				}				
				connWallet.close();
			} catch (ClassNotFoundException e) {
				System.out.println("Error! \n" + "Database connection is not available!\n" + e.toString());
			} catch (SQLException e) {
				System.out.println("WalletDB Database is not available!\n" + e.toString());
			}
		} 
		return queryAccessLogResultList;
	}

	private int insertAccessLogList(List<String> logFileList) throws SQLException {

		PreparedStatement pst; 
		int executedUpdateLines = 0;
		int qtdInsertLines = 0;
		StringBuilder sbuilderMultiInsert;
		StringTokenizer stzMultInsert;
		int splitInsert = 10000;
		int executeUpdateInt;
		
		String insertAccessLog = "insert into accesslog (dtlog, ipaddress, httpmethod, httpreturncode, logdescription) values";
		sbuilderMultiInsert = new StringBuilder(insertAccessLog);

		pst = connWallet.prepareStatement("truncate table accesslog");
		executeUpdateInt = pst.executeUpdate();

		System.out.println("Truncate successfully executed!");
		executeUpdateInt = 0;

		for (String accessLogLine : logFileList) {
			stzMultInsert = new StringTokenizer(accessLogLine, "|");
			
			//Insert date time
			sbuilderMultiInsert.append("('").append(stzMultInsert.nextToken()).append("','");

			//Insert IP Address
			sbuilderMultiInsert.append(stzMultInsert.nextToken()).append("',");

			//Insert http method
			sbuilderMultiInsert.append(stzMultInsert.nextToken()).append(",");					

			//Insert httpreturncode
			sbuilderMultiInsert.append(stzMultInsert.nextToken());
			sbuilderMultiInsert.append(",");

			//Insert Description
			sbuilderMultiInsert.append(stzMultInsert.nextToken());
			sbuilderMultiInsert.append(")");
			
			if (++qtdInsertLines == splitInsert || (qtdInsertLines + executedUpdateLines) >= logFileList.size() ) {
				qtdInsertLines = 0;
				//System.out.println("\n Insert ===> " + sbuilderMultiInsert.toString());
				pst = connWallet.prepareStatement(sbuilderMultiInsert.toString());
				executeUpdateInt = pst.executeUpdate();
				executedUpdateLines = executedUpdateLines + executeUpdateInt;
				if (executeUpdateInt > 0) {
					executeUpdateInt = 0;
					//System.out.println("[" + executedUpdateLines + "] lines inserted successfully! " + new Date());
				} else {
					System.out.println("Insert did not work properly!");
				}
				pst.close();
				sbuilderMultiInsert.delete(0, sbuilderMultiInsert.length()).append(insertAccessLog);
			} else {
				sbuilderMultiInsert.append(",");
			}
		}
		return executedUpdateLines;
	}
		
	private List<String> getLogFileList(String accessLog) {
		List<String> fileAccessList = new ArrayList<>();
		
		try (BufferedReader breader = Files.newBufferedReader(Paths.get(accessLog))) {
			fileAccessList = breader.lines().collect(Collectors.toList());
			System.out.println("Access log file read at " + new Date());
		} catch (Exception e) {
			System.out.println("Error! Check if the file exist and it permissions or maybe is corrupted! Error message = " + e.toString());
		}
		return fileAccessList;
	}	
	
	private List<String> queryLog(Map<String, String> parserParameterMap) throws SQLException {
		List<String> queryLogList = new ArrayList<>();
		ResultSet queryLogResultSet;

		StringBuilder sblQuery = new StringBuilder("SELECT * FROM accesslog A WHERE dtlog between cast(");
		String duration = parserParameterMap.get(ParameterType.DURATION.paramType());
		String startDate = parserParameterMap.get(ParameterType.STARTDATE.paramType());
		String strThreshold = parserParameterMap.get(ParameterType.THRESHOLD.paramType());
		int threshold = 0;
		LocalDateTime ldtStart = LocalDateTime.parse(startDate, dtf);
		String endDate;
	
		if ("hourly".equals(duration)) {
			endDate = ldtStart.plusHours(1).format(dtf);
		} else if ("daily".equals(duration)) {
			endDate = ldtStart.plusDays(1).format(dtf);
		} else {
			endDate = ldtStart.plusMonths(1).format(dtf);
			System.out.println("Duration invalid! Monthly will be used!");
		}
		
		if (strThreshold == null && strThreshold.length() > 0) {
			System.out.println("Threshold is invalid! strThreshold = " + strThreshold);
		} else {
			System.out.println("strThreshold = " + strThreshold);
			try {
				 threshold = Integer.parseInt(strThreshold);
			} catch (Exception e) {
				System.out.println("Threshold is invalid! ");
			}
		} 

		 sblQuery.append("'")
			.append(ldtStart.format(dtf))
			.append("' as datetime) and cast('")
			.append(endDate).append("' as datetime) and EXISTS  (SELECT 1 FROM accesslog B WHERE B.ipaddress = A.ipaddress GROUP BY B.ipaddress HAVING COUNT(*) > ")
			.append(String.valueOf(threshold))
			.append(")");

		//System.out.println(sblQuery.toString());
		queryLogResultSet = connWallet.prepareStatement(sblQuery.toString()).executeQuery();
		while(queryLogResultSet.next()) {
			queryLogList.add(queryLogResultSet.getDate("dtlog") + " | " 
					+ queryLogResultSet.getString("ipaddress") + " | "
					+ queryLogResultSet.getString("httpmethod") + " | "
					+ queryLogResultSet.getString("httpreturncode") + " | "
					+ queryLogResultSet.getString("logdescription"));
		}

		return queryLogList;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		connWallet = null;
	}
}
