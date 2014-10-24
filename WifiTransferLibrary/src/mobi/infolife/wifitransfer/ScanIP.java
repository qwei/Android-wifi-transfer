package mobi.infolife.wifitransfer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ScanIP {
	
	private final static int SCAN_ONE_ID = 111;
	private final static int FIND_SERVER_ID = 222;
	
	private int scanCount = 0;
	private String locAddress;//  local ip:192.168.1.
	private Runtime run = Runtime.getRuntime();
	private String ping = "ping -c 1 -w 0.5 ";// 
	private int localIP;

	private Context ctx;
	private ScanServerListener scanServerListener;
	private ExecutorService mPool = null;
	private int startIP = 1,endIP = 254;
	
	public ScanIP(Context ctx, ScanServerListener scanServerListener) {
		this.ctx = ctx;
		this.scanServerListener = scanServerListener;
		scanCount = 0;
	}

	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case SCAN_ONE_ID:
				scanCount++;
				String currentIP = (String)msg.obj;
				scanServerListener.onScanning(currentIP);
				if (scanCount == (endIP - startIP)){
					scanServerListener.OnFinish();
					try{
						mPool.shutdown();
					} catch(Exception e){}
					mPool = null;
				}
				break;
			case FIND_SERVER_ID:
				ClientInfo client = (ClientInfo) msg.obj;
				if (client != null){
					scanServerListener.onFindServer(client);
				}

				break;
			}
		};
	};

	public void scan() {
		if(mPool != null)return;
		scanCount = 0;
		mPool = Executors.newFixedThreadPool(50);
		locAddress = getLocAddrIndex();
		if (locAddress == null || "".equals(locAddress)) {
			Toast.makeText(ctx, "Failure", Toast.LENGTH_LONG).show();
			return;
		}

		for (int i = startIP; i <= endIP; i++) {
			final int j = i;
			if (i == localIP){
//				Log.d("test", "++++++++++++++++++++localIP="+localIP);
				continue;
			}
			mPool.execute(new Runnable() {

				public void run() {
					Process proc = null;
					String p = ScanIP.this.ping + locAddress
							+ j;

					String current_ip = locAddress + j;

					try {
						proc = run.exec(p);
						int result = proc.waitFor();
//						Log.d("test", result+"++++++++++++++"+current_ip);
						if (result == 0) {
//							Log.d("test", "connected success::" + current_ip);
							NetworkManager netManager = new NetworkManager(ctx,
									"http://" + current_ip + ":" + ServerManager.port
											+ Utils.GET_HOST_NAME);
							String deviceName = netManager.excutePost(null);
							ClientInfo client = null;
//							Log.d("test", "scan::" + deviceName + "++++" + j);
							if (!Utils.ERROR_CONTENT.equals(deviceName)) {
								client = new ClientInfo();
								client.deviceName = deviceName;
								client.ip = current_ip;
								client.requestKey = Utils.encodeRequestKey(current_ip);
								Message msg = new Message();
								msg.obj = client;
								msg.what = FIND_SERVER_ID;
								handler.sendMessage(msg);
							}
						} else {
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					} finally {
						if(proc != null)
							proc.destroy();
						Message msg = new Message();
						msg.obj = current_ip;
						msg.what = 111;
						handler.sendMessage(msg);
					}
				}
			});


		}

	}

	public String getLocAddress() {

		String ipaddress = "";

		try {
			Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface networks = en.nextElement();
				Enumeration<InetAddress> address = networks.getInetAddresses();
				while (address.hasMoreElements()) {
					InetAddress ip = address.nextElement();
					if (!ip.isLoopbackAddress()
							&& InetAddressUtils.isIPv4Address(ip
									.getHostAddress())) {
						ipaddress = ip.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return ipaddress;

	}

	public String getLocAddrIndex() {

		String str = getLocAddress();
//		Log.d("test", str);
		localIP = Integer.parseInt(str.substring(str.lastIndexOf(".") + 1));
		scanServerListener.getLocalIp(str);
		if (!str.equals("")) {
			return str.substring(0, str.lastIndexOf(".") + 1);
		}
		return null;
	}

	class ClientInfo {
		String ip;
		String deviceName;
		String requestKey;

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public void setDeviceName(String deviceName) {
			this.deviceName = deviceName;
		}

		public String getRequestKey() {
			return requestKey;
		}

		public void setRequestKey(String requestKey) {
			this.requestKey = requestKey;
		}
		
		
	}
	public void onDestroy(){
		if(mPool != null){
			try{
				mPool.shutdown();
			} catch(Exception e){}
			
		}
	}

	interface ScanServerListener {
		void OnFinish();
		void onFindServer(ClientInfo client);
		void onScanning(String ip);
		void getLocalIp(String localIP);
	}
}
