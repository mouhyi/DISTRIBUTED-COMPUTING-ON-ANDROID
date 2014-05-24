package com.mcgill.ludecomposition;

import java.util.LinkedList;

import android.os.Handler;
import android.os.Message;

public class Worker implements Runnable {

	private LinkedList<Job> toDo;
	private int currentIterationNumber;
	public final int MAX_NUMBER_JOBS = Integer.MAX_VALUE; 
	private Sender sender;
	private Object currentIterationLock;
	private Handler handler;
	private long numComputations;
	private volatile boolean killSelf;
	
	public Worker (Sender sender, Handler handler){
		toDo = new LinkedList<Job>();
		currentIterationNumber = Integer.MIN_VALUE;
		this.sender = sender;
		currentIterationLock = new Object ();
		this.handler = handler;
		numComputations = 0;
		killSelf = false;
	}
	
	@Override
	public void run() {
		Job active;
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		
		//CONSUME!
		while (! killSelf) {
			active = getNextJob();
			decompose(active);
		}
		System.out.println("Worker dead");
	}
	
	//The actual LU decomposition. Note, all computations are done directly on job
	public void decompose (Job job){
		/**
		 * 
		 * IMITATE A DECOMPOSITION
		 * 
		 * */
		if (job == null) return;
		
		
		
		double [][] matrix = job.getPayload();
//		System.out.println("RECEIVED:");
//		Job.printMatrix(matrix);
		
		int iteration = job.getIteration();
		
		int n = matrix.length;
		int m = matrix[0].length;
		
		for (int i = 1; i < n; i++) {
			// divide the pivot element
			matrix[i][iteration] /= matrix[0][iteration];

			// reduce remaining submatrix
			for (int k = iteration + 1; k < m; ++k) {
				matrix[i][k] -= matrix[i][iteration] * matrix[0][k];
			}
		}
		
		
		
		Message msg = handler.obtainMessage();
		
		msg.what = Constants.MSG_WORKER;
		
		numComputations += matrix.length* (matrix[0].length-iteration);
		msg.obj=getMessage(job.getIteration(),job.getSequenceNumber());
		
		handler.sendMessage(msg);
		
		
//		System.out.println("JOB MATRIX:");
//		Job.printMatrix(job.getPayload());
		if (job.getIteration() >= currentIterationNumber) sender.addResponse(job);
	}
	
	private String getMessage(int iteration, int seqNumber) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("Job iteration: "+iteration+"\n");
		sb.append("Job sequence number: "+seqNumber+"\n");
		sb.append("Total computations: "+numComputations+"\n");
		
		return sb.toString();
	}

	//Adds a job to be complete to the queue.
	public boolean addJob (Job job){
		
		boolean result = false;
		
		//If the iteration has been updated, remove all jobs in the queue which are out of date
		if (updateIteration(job.getIteration())){
			purgeToDo();
		}
		
		synchronized(toDo){
			if (toDo.size() < MAX_NUMBER_JOBS)	{
				toDo.addLast(job);
				result = true;
			}
		}
		return result;
	}
	
	//Blocks until a job arrives.
	public Job getNextJob (){
		
		Job job = null;
		while (! killSelf) {
			job = getNextJobInQueue();
			
			if (job != null){
				return job;
			}
		}
		return null;
		
	}
	
	//Helper method for getNextJob
	private Job getNextJobInQueue () {
		
		Job result = null;
		synchronized (toDo){
			if (toDo.size() > 0) {
				result = toDo.removeFirst();
			}
		}
		return result;
		
	}
	
	//Removes all Jobs in toDo which have an out of date iteration number according to currentIterationNumber
	public void purgeToDo (){
		LinkedList<Job> toBeRemoved = new LinkedList<Job>();
		int currentIterationNumber;
		
		//Get the currentIterationNumber, we need this number to be persistent
		synchronized (currentIterationLock){
			currentIterationNumber = this.currentIterationNumber;
		}
		
		synchronized (toDo){
			for (Job j : toDo) {
				if (j.getIteration() < currentIterationNumber){
					toBeRemoved.add(j);
				}
			}
			for (Job j : toBeRemoved){
				toDo.remove(j);
			}
		}
	}
	
	//Returns true if the iteration has been updated
	public boolean updateIteration (int newIterationNumber) {
		synchronized (currentIterationLock){
			if (newIterationNumber > currentIterationNumber){
				currentIterationNumber = newIterationNumber;
				return true;
			}
			return false;
		}
	}

	public void die (){
		killSelf = true;
	}
}
