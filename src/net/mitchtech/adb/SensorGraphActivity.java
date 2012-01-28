package net.mitchtech.adb;

import java.io.IOException;

import net.mitchtech.adb.sensorgraph.R;

import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SensorGraphActivity extends Activity {

	private static final String TAG = SensorGraphActivity.class.getSimpleName();
	private GraphView mGraph;
	private int mSensorValue = 10;

	// Create TCP server (based on MicroBridge LightWeight Server).
	// Note: This Server runs in a separate thread.
	Server mServer = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mGraph = (GraphView) findViewById(R.id.graph);
		mGraph.setMaxValue(1024);

		// Create TCP server (based on MicroBridge LightWeight Server)
		try {
			mServer = new Server(4568); // Same port number used in ADK firmware
			mServer.start();
		} catch (IOException e) {
			Log.e(TAG, "Unable to start TCP server", e);
			System.exit(-1);
		}

		mServer.addListener(new AbstractServerListener() {
			@Override
			public void onReceive(org.microbridge.server.Client client, byte[] data) {
				if (data.length < 2)
					return;
				mSensorValue = (data[0] & 0xff) | ((data[1] & 0xff) << 8);
				// Any update to UI can not be carried out in a non UI thread
				// like the one used for Server. Hence runOnUIThread is used.
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						new UpdateData().execute(mSensorValue);
					}
				});
			}
		});
	}

	// UpdateData Asynchronously sends the value received from ADK Main Board.
	// This is triggered by onReceive()
	class UpdateData extends AsyncTask<Integer, Integer, Integer> {
		@Override
		protected Integer doInBackground(Integer... sensorValue) {
			return (sensorValue[0]);  // This goes to result
		}

		// Called when there's a status to be updated
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			// Not used in this case
		}

		// Called once the background activity has completed
		@Override
		protected void onPostExecute(Integer result) {
			// Init TextView Widget to display ADC sensor value in numeric.
			GraphView mGraphView = (GraphView) findViewById(R.id.graph);
			mGraphView.addDataPoint(result);
			TextView mTextView = (TextView) findViewById(R.id.value);
			mTextView.setText(String.valueOf(result));
		}
	}
}
