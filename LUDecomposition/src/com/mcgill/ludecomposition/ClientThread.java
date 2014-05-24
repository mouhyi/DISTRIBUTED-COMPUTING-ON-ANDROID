package com.mcgill.ludecomposition;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class ClientThread implements Runnable {

	private static final int SERVER_PORT = 6780;
	private static final String SERVER_IP = "142.157.150.98";

	private Socket clientSocket;
	private InetAddress serverAddress;

	private Thread workerThread;
	private Worker worker;

	private Thread senderThread;
	private Sender sender;

	private String email;
	private volatile boolean connected;
	private Context context;
	private Handler handler;

	private BufferedReader inFromServer;
 	private DataOutputStream outToServer;
	
	public ClientThread(Context context, Handler handler) {
		this.handler = handler;
		this.context = context;
		this.inFromServer = null;
		this.outToServer = null;
		connected = false;
	}

	@Override
	public void run() {

		String rawInput;
		Job job = null;

		// Set the thread priority
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		connect();

		// Opens resources.
		if (connected) {
			try {

				// Input and output resources
				inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				outToServer = new DataOutputStream(clientSocket.getOutputStream());

				// A thread to take care of sending messages
				sender = new Sender(email, outToServer);
				senderThread = new Thread(sender);
				senderThread.start();

				// A thread to take care of the computation
				worker = new Worker(sender, handler);
				workerThread = new Thread(worker);
				workerThread.start();
				
			} catch (IOException e) {
				System.err.println("Unable to open stream");
				connected = false;
			}
		}

		// From this point on, only closeConnection() should be called to close
		// the connection,
		// it will take care of freeing resources.
		while (connected) {

			// listen for incoming message, then dispatch a thread to deal with
			// the information
			try {
				System.out.println("Waiting for input");
				rawInput = inFromServer.readLine();
				if (rawInput == null) break;
				
				job = new Job(rawInput);
				
				// Received a heartbeat
				if (job.isHeartbeat()) {
					sender.addResponse(job);
				}

				// Work received, give the job to our worker
				else if (job.isWork()) {
					worker.addJob(job);
				}

				// Unknown input or disconnect, disconnect immediately
				else {
					closeConnection();
				}

			} catch (IOException e) {}
		}
		
		//Die
		try {
			killWorker();
			killSender();
			
			//Free other resources
			if (inFromServer != null) inFromServer.close();
			if (outToServer != null) outToServer.close();
		} catch (IOException e){}
		System.out.println("ClientThread dead");
	}

	/**
	 * 
	 * Attempts to connect to the server, returns true if successful, false
	 * otherwise.
	 * 
	 **/
	private void connect() {

		// Retrieves the username of this user - for debugging purposes usually.
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType("com.google");
		this.email = accounts[0].name;
		if (email == null) email = "UNKNOWN";

		// Connect to the server
		try {
			serverAddress = InetAddress.getByName(SERVER_IP);
			clientSocket = new Socket(serverAddress, SERVER_PORT);
			connected = true;
			
			Message msg = handler.obtainMessage();
			msg.what = Constants.MSG_CONNECT;
			
			Date d = new Date();
//			System.out.println("CONNECTED : "+ d.toString());
			msg.obj = d.toString();
			handler.sendMessage(msg);

		} catch (UnknownHostException e1) {
			Message msg = handler.obtainMessage();
			msg.what = Constants.MSG_CONNECT_FAIL;
			handler.sendMessage(msg);
			
			System.out.println("UnknownHostException");
			connected = false;
		} catch (IOException e) {
			Message msg = handler.obtainMessage();
			msg.what = Constants.MSG_CONNECT_FAIL;
			handler.sendMessage(msg);
			
			System.out.println("IOException");
			connected = false;
		}
	}

	public void disconnect (){
		closeConnection();
	}
	
	public boolean isConnected() {
		return connected;
	}

	// Closes the connection and frees global resources
	public void closeConnection() {
		
		//Notify the GUI
		Message msg = handler.obtainMessage();
		msg.what = Constants.MSG_CONNECT;
		handler.sendMessage(msg);
		
		connected = false;
		try {
			if (clientSocket != null) {
				clientSocket.shutdownInput();
				clientSocket.shutdownOutput();
				clientSocket.close();
			}
		} catch (IOException e) {
			System.out.println("could not close socket");
		}

	}
	
	private void killWorker (){
		if (worker != null) worker.die();
		if (workerThread == null) return;
		while (workerThread.isAlive()){}
	}
	
	private void killSender (){
		if (sender != null) sender.die();
		if (senderThread == null) return;
		while (senderThread.isAlive()){}
	}
}
