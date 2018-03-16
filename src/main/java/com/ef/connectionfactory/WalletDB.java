package com.ef.connectionfactory;

public enum WalletDB {
	DRIVER("com.mysql.jdbc.Driver"),
	JDBC_URL("jdbc:mysql://localhost:3306/wallethub"),
	USER("root"),
	PASSWORD("Langdon#1618");
	
	private String connectionParam;
	
	WalletDB(String connectionParam) {
		this.connectionParam = connectionParam;
	}
	
	public String connectionParam() {
		return this.connectionParam;
	}
}
