package com.ef;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ef.model.ParameterType;
import com.ef.repository.LogRepository;

public class Parser {

	private final static Map<String, String> parserParameterMap = new HashMap<>();
	private final static String USAGE = "The Parser usage is: java -cp \"parser-1.jar\" com.ef.Parser --accesslog=/path/to/file --startDate=2017-01-01.13:00:00 --duration=hourly --threshold=100";
	
	public static void main(String[] args) {
		if (areValidParams(args)) {
			System.out.println("Parser started at " + new Date() );
			execute();
		}		
	}	
	
	static private void execute() {
		LogRepository logRepository = new LogRepository();
		List<String> queryLogFileList = logRepository.execute(parserParameterMap);
		if (queryLogFileList != null && queryLogFileList.size() > 0) {
			printResult(queryLogFileList);
		}
	}
	
	static private void printResult(List<String> queryLogFileList) {

		System.out.println("+-------------------------- Query result - Begin ----------------------------------------------+");
		for (String string : queryLogFileList) {
			System.out.println(string);
		}
		System.out.println("+-------------------------- Query result - End -----------------------------------------------+");
	}
	
	static private boolean areValidParams(String[] args) {

		boolean validParams = false;
		if (args == null || args.length == 0) {
			System.out.println(USAGE);
		} else {
			String parameterName;
			String parameterValue;
			int equalPosition = 0;
			byte qtdParams = 0;
			
			for (String param : args) {
				
				if (param == null || param.length() <= 13) {
					System.out.println(USAGE);
				} else {
					equalPosition = param.indexOf("=");
					if (equalPosition <= 8) {
						System.out.println(USAGE);
					} else {
						parameterName = param.substring(0, equalPosition);
						parameterValue = param.substring(equalPosition+1, param.length());
						
						if (ParameterType.ACCESSLOG.paramType().equals(parameterName)) {
							parserParameterMap.put(parameterName, parameterValue);
							qtdParams++;
							
						} else if (ParameterType.STARTDATE.paramType().equals(parameterName)) {
							parserParameterMap.put(parameterName, parameterValue);
							qtdParams++;
	
						} else if (ParameterType.DURATION.paramType().equals(parameterName)) {
							parserParameterMap.put(parameterName, parameterValue);
							qtdParams++;
							
						} else if (ParameterType.THRESHOLD.paramType().equals(parameterName)) {
							parserParameterMap.put(parameterName, parameterValue);	
							qtdParams++;
						} 
						//System.out.println("[" + parameterName + "]:[" + parserParameterMap.get(parameterName) + "]");
					}
				}
				
			}
			
			validParams = qtdParams == 4;
		}
		
		return validParams;
	}
}



