package com.mcgill.ludecomposition;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	public boolean isConnected = false;
	private ClientThread ct = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg != null) {
				
				if (msg.what == Constants.MSG_CONNECT_FAIL) {
					System.out.println("CONNECTION FAILED");
					((TextView) findViewById(R.id.connect_message)).setText("");
					((TextView) findViewById(R.id.system_message)).setTextColor(Color.RED);
					((TextView) findViewById(R.id.system_message)).setText("Connection failed...");
					
				} else if (msg.what == Constants.MSG_CONNECT) {
					findViewById(R.id.system_message).setVisibility(View.VISIBLE);
					isConnected = !isConnected;
					if (isConnected) {
						System.out.println("CONNECTION SUCCESSFUL");
						((TextView) findViewById(R.id.system_message)).setText("Connected");
						((TextView) findViewById(R.id.system_message)).setTextColor(Color.parseColor("#00A125"));
						
						((TextView) findViewById(R.id.connect_message)).setText("Connected since: "+ msg.obj.toString());
						
						findViewById(R.id.connect).setVisibility(View.GONE);
						findViewById(R.id.disconnect).setVisibility(View.VISIBLE);
						
					} else {
						((TextView) findViewById(R.id.system_message)).setText("Disconnected");
						((TextView) findViewById(R.id.system_message)).setTextColor(Color.parseColor("#00A125"));
						
						((TextView) findViewById(R.id.connect_message)).setText("");
						((TextView) findViewById(R.id.worker_message)).setText("");
						
						findViewById(R.id.disconnect).setVisibility(View.GONE);
						findViewById(R.id.connect).setVisibility(View.VISIBLE);						
					}

				} else if (msg.what == Constants.MSG_WORKER) {
					if(isConnected){
						findViewById(R.id.system_message).setVisibility(View.GONE);
						((TextView) findViewById(R.id.worker_message)).setText(msg.obj.toString());
					}
				}
				super.handleMessage(msg);
			}
		}
	};

	/**
	 * Called when the user clicks the Connect button Tries to connect with the
	 * host.
	 * 
	 * */
	public void connect(View view) {

		// Start the new thread
		ct = new ClientThread(this, handler);
		Thread t = new Thread(ct);
		t.start();
	}
	
	/**
	 * Called when the user selects the Disconnect button
	 * 
	 * 
	 * */
	public void disconnect(View view) {
		System.out.println("Disconnect was called");
		if (ct != null){
			ct.disconnect();
		}
		
		findViewById(R.id.disconnect).setVisibility(View.GONE);
		findViewById(R.id.connect).setVisibility(View.VISIBLE);	
		//
	}

}
