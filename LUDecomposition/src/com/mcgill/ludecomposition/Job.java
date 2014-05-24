package com.mcgill.ludecomposition;

/**
 *  Encases the information a job
 * */

public class Job {

	private final int COMMAND = 0, ITERATION = 1, SEQUENCE_NUMBER = 2, PAYLOAD = 3;
	
	private int iteration;
	private int sequenceNumber;
	private String command;
	private double [][] payload;
	
	public Job (String input){
		
		String [] structInput = input.split(",");
		
		//Parse command
		if (structInput.length < 4) {
			command = "DISCONNECT";
			return;
		}
		else if (structInput[COMMAND].equals("HEARTBEAT") || structInput[COMMAND].equals("WORK")) {
			command = structInput[COMMAND];
		}
		else {
			command = "DISCONNECT";
			return;
		}
		
		//Parse the iteration, if necessary.
		if (iterationRequired()){
			try {
				iteration = Integer.parseInt(structInput[ITERATION]);
			} catch (NumberFormatException e) {
				command = "DISCONNECT";
				return;
			}
		}
		
		//Parses the sequence number
		if (sequenceNumberRequired()){
			try {
				sequenceNumber = Integer.parseInt(structInput[SEQUENCE_NUMBER]);
			} catch (NumberFormatException e) {
				command = "DISCONNECT";
				return;
			}
		}
		
		//Parses the payload
		if (payloadRequired()){
			
			int len = 0;
			String row [];
			int i;
			for (i = PAYLOAD ; i < structInput.length ; i ++){
				
				//Each row has the values space separated
				row = structInput[i].split(" ");
				
				//On the first iteration, we now have enough information to allocate memory for the payload matrix
				if (i == PAYLOAD){
					len = row.length;
					payload = new double [structInput.length - PAYLOAD][len];
				}
				
				//For future iterations, if the row length is not the same, the matrix is uneven, disconnect. Error
				//or malicious access has occurred.
				else if (row.length != len) {
					command = "DISCONNECT";
					return;
				}
				
				//Go through each row of the payload matrix and convert it to what we want.
				for (int j = 0 ; j < len ; j ++){
					try {
						payload[i - PAYLOAD][j] = Double.parseDouble(row[j]);
					} catch (NumberFormatException e){
						command = "DISCONNECT";
						return;
					}
				}	
			}
		}
	}
	
	
	//Helper methods
	private boolean payloadRequired (){
		if (command == null || command.equals("DISCONNECT") || command.equals("HEARTBEAT")) return false;
		else return true;
	}
	
	private boolean iterationRequired(){
		if (command == null || command.equals("DISCONNECT")) return false;
		else return true;
	}
	
	private boolean sequenceNumberRequired (){
		return payloadRequired();
	}
	
	//Standard getters
	public int getIteration () {
		return iteration;
	}
	
	public int getSequenceNumber () {
		return sequenceNumber;
	}
	
	public double [][] getPayload (){
		return payload;
	}
	
	public boolean isWork (){
		return command.equals("WORK");
	}
	
	public boolean isHeartbeat (){
		return command.equals("HEARTBEAT");
	}
	
	public boolean isDisconnect (){
		return !(isWork() || isHeartbeat());
	}
	
	public static void printMatrix(double[][] matrix) {
		int dimension = matrix.length;
		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				System.out.print(" " + matrix[i][j]);
			}
			System.out.println("");
		}
	}
	
}
