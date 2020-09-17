/* ******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.jinyx.mqtt.paho;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	private ClientComms comms;
	private MqttService service;
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;
	private volatile boolean hasStarted = false;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.service = service;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		String action = MqttServiceConstants.PING_SENDER + comms.getClient().getClientId();
		try{
			service.registerReceiver(alarmReceiver, new IntentFilter(action));
		} catch(IllegalArgumentException e) {
			//Ignore unregister errors.
		}

		pendingIntent = PendingIntent.getBroadcast(
			service, 0, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT
		);

		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {
		if(hasStarted){
			if(pendingIntent != null){
				// Cancel Alarm.
				AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
				alarmManager.cancel(pendingIntent);
			}

			hasStarted = false;
			try{
				service.unregisterReceiver(alarmReceiver);
			}catch(IllegalArgumentException e){
				//Ignore unregister errors.			
			}
		}
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		AlarmManager alarmManager = (AlarmManager) service
				.getSystemService(Service.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT >= 23){
			// In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
			// the device to run this task whilst dosing.
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		} else if (Build.VERSION.SDK_INT >= 19) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		} else {
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class PingAsyncTask extends AsyncTask<ClientComms, Void, Boolean> {

		boolean success = false;

		protected Boolean doInBackground(ClientComms... comms) {
			IMqttToken token = comms[0].checkForActivity(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					success = true;
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					success = false;
				}
			});

			if (token != null) {
				try {
					token.waitForCompletion();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return success;
		}

		protected void onPostExecute(Boolean success) {
		}

		protected void onCancelled(Boolean success) {
		}

	}

	/*
	 * This class sends PingReq packet to MQTT broker
	 */
	class AlarmReceiver extends BroadcastReceiver {
		private PingAsyncTask pingRunner = null;
		private WakeLock wakelock;
		private final String wakeLockTag = MqttServiceConstants.PING_WAKELOCK
				+ that.comms.getClient().getClientId();

		@Override
        @SuppressLint("Wakelock")
		public void onReceive(Context context, Intent intent) {
			// According to the docs, "Alarm Manager holds a CPU wake lock as
			// long as the alarm receiver's onReceive() method is executing.
			// This guarantees that the phone will not sleep until you have
			// finished handling the broadcast.", but this class still get
			// a wake lock to wait for ping finished.

			PowerManager pm = (PowerManager) service
					.getSystemService(Service.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
			wakelock.acquire();

			if (pingRunner != null) {
				pingRunner.cancel(true);
			}

			pingRunner = new PingAsyncTask();
			pingRunner.execute(comms);

			if (wakelock.isHeld()) {
				wakelock.release();
			}
		}
	}
}
