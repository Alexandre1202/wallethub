package com.ef.model;

public enum ParameterType {
	ACCESSLOG("--accesslog"),
	STARTDATE("--startDate"),
	DURATION("--duration"),
	THRESHOLD("--threshold");
	
	private String paramType;
	
	ParameterType(String paramType) {
		this.paramType = paramType;				
	}
	
	public String paramType() {
		return this.paramType;
	}

}