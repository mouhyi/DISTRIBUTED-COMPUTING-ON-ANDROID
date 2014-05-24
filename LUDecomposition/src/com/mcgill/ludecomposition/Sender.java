package com.mcgill.ludecomposition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class Sender implements Runnable {

	private String email;
	private LinkedList<Job> toSend;
	private final int MAX_NUMBER_SENDS = Integer.MAX_VALUE;
	private volatile boolean killSelf;
	private DataOutputStream outToServer; 
	
	public Sender (String email, DataOutputStream outToServer){
		this.email = email;
		toSend = new LinkedList<Job>();
		this.outToServer = outToServer;
		this.killSelf = false;
	}
	
	@Override
	public void run() {
		
		//CONSUME!
		Job active = null;
		while (! killSelf) {
			active = getNextResponse();
			if (active != null) respond(convertToResponse(active));
		}
		System.out.println("Sender dead");
	}
	
	public boolean addResponse (Job job) {
		System.out.println("resonse added to queue");
		boolean result = false;
		synchronized (toSend){
			if (toSend.size() < MAX_NUMBER_SENDS){
				toSend.addFirst(job);
				result = true;
			}
		}
		return result;
	}
	
	//Blocks until a job arrives.
	public Job getNextResponse (){	
		Job result = null;
		while (result == null && !killSelf){
			if (toSend.size() > 0) {
				synchronized (toSend){
					result = toSend.removeFirst();
				}
			}
		}
		return result;
	}
	
	//Converts a job to a return response.
	//<COMMAND>,<EMAIL>,<ITERATIONNUM>,<SEQUENCENUM>,MATRIX(with csv rows, and space separate cols)
	public String convertToResponse (Job job){
		String response = "";
		int numRows, numCols;
		double [][] matrix;
		
		if (job.isDisconnect()) {
			response = "DISCONNECT," + email + ",1,2,3\n";
		}
		else if (job.isHeartbeat()){
			response = "HEARTBEAT," + email + "," + job.getIteration() + "," + job.getSequenceNumber() + ",123\n";
		}
		else {
			response = "WORK," + email + "," + job.getIteration() + "," + job.getSequenceNumber();
			matrix = job.getPayload();
			numRows = matrix.length;
			numCols = matrix[0].length;
			
			for (int i = 1; i < numRows ; i++){
				response = response + ",";
				for (int j = 0 ; j < numCols ; j++){
					if (j == 0) response = response + matrix[i][j];
					else response = response + " " + matrix[i][j];
				}
			}
			
			response = response + "\n";
		}
		return response;
	}
	
	public void respond (String response){
		
		try {
			if (outToServer != null) {
				outToServer.writeBytes(response);
				outToServer.flush();
//				System.out.print("Response sent: " + response);
			}
		} catch (IOException e){
			System.err.println("could not send message");
		}		
	}
	
	public void die (){
		killSelf = true;
	}
}
