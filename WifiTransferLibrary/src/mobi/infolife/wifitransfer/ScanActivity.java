package mobi.infolife.wifitransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mobi.infolife.wifitransfer.ScanIP.ClientInfo;
import mobi.infolife.wifitransfer.ScanIP.ScanServerListener;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

public class ScanActivity extends SherlockActivity implements OnClickListener{

	private final static int CONFIRM_RESULT_ID = 111;
	private final static int DOWNLOAD_RESULT_ID = 222;
	private final static int TRANSFER_PROGRESS_ID = 555;

	private List<ClientInfo> clients;
	private Context mContext;
	private ClientInfo clientInfo;
	public static List<String> pathList = new ArrayList<String>();
	private AlertDialog dialog = null;
	private ListView deviceListView = null;
	public static String requestKey = "";
	private DeviceAdapter adapter = null;

	private TextView fileName, fileSize, fileFrom, fileTo, progressVaule;
	private ProgressBar progressBar;
	private long totalSize;
	private long finishedSize = 0;
	private int finishedCount = 0;
	private TransferServer nanoHttpd = null;
	private LinearLayout scanningLayout = null,sendLayout;
	private TextView scanningIP = null,noDeviceLayout;
	private TextView localIPTextView = null;
	private boolean isScanning = true;

	ScanServerListener scanServerListener = new ScanIP.ScanServerListener() {
		@Override
		public void OnFinish() {
			if (clients.size() == 0) {
				noDeviceLayout.setVisibility(View.VISIBLE);
			}
			isScanning = false;
			scanningLayout.setVisibility(View.GONE);
			localIPTextView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onFindServer(ClientInfo client) {
			boolean isContain = false;
			for (ClientInfo clientInfo : clients) {
				if (clientInfo.getIp().equals(client.getIp())) {
					isContain = true;
					break;
				}
			}
			if (!isContain) {
				clients.add(client);
				adapter.notifyDataSetChanged();
			}

		}

		@Override
		public void onScanning(String ip) {
			scanningIP.setText(getString(R.string.scanning) + ip);
		}

		@Override
		public void getLocalIp(String localIP) {
			localIPTextView.setText(getString(R.string.my_ip)+localIP);
		}
	};
	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case TRANSFER_PROGRESS_ID:
				if (progressBar != null) {
					long size = (Long) msg.obj;
					finishedSize += size;
					progressBar.setMax(100);
					int progress = (int) (finishedSize * 100 / totalSize);
					progressBar.setProgress(progress);
					progressVaule
							.setText((int) (finishedSize * 100 / totalSize)
									+ "%");
				}

				break;
			case CONFIRM_RESULT_ID:
				String result = (String) msg.obj;
				if (!Utils.CLIENT_RECEIVED_COTENT.equals(result)) {
					String message = mContext.getString(R.string.connect_failed);
					Toast.makeText(mContext,
							String.format(message, clientInfo.getIp()),
							Toast.LENGTH_SHORT).show();
					break;
				}

				String path = pathList.get(pathList.size() - 1);
				File file = new File(path);
				LayoutInflater inflater = LayoutInflater.from(mContext);
				View view = inflater.inflate(R.layout.transferring, null);
				fileName = (TextView) view.findViewById(R.id.file_name);
				fileSize = (TextView) view.findViewById(R.id.file_size);
				fileFrom = (TextView) view.findViewById(R.id.file_local);
				fileTo = (TextView) view.findViewById(R.id.file_to);
				progressBar = (ProgressBar) view
						.findViewById(R.id.progress_bar);
				progressVaule = (TextView) view
						.findViewById(R.id.progress_value);
				fileName.setText(file.getName());
				fileSize.setText(pathList.size() + " "+getString(R.string.item)+","
						+ Utils.formatSize(totalSize));
				fileFrom.setText(path);
				fileTo.setText(clientInfo.getIp());

				dialog = new AlertDialog.Builder(mContext)
						.setTitle(getString(R.string.transferring))
						.setView(view)
						.setNegativeButton(getString(R.string.cancel),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										new Thread(new Runnable() {
											public void run() {
												NetworkManager manager = new NetworkManager(
														mContext,
														"http://"
																+ clientInfo.ip
																+ ":"
																+ ServerManager.port
																+ Utils.CANCEL);
												List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
												nameValuePairs
														.add(new BasicNameValuePair(
																Utils.PARMS_REQUEST_KEY,
																requestKey));
												manager.excutePost(nameValuePairs);
											}
										}).start();
									}
								}).create();
				dialog.setCanceledOnTouchOutside(false);
				dialog.show();
				
				break;
			case DOWNLOAD_RESULT_ID:
				finishedCount++;
				if ((finishedCount == pathList.size()) && (dialog != null))
					dialog.dismiss();
				break;

			}
		};
	};

	private void getCheckedItems() {
		int position = deviceListView.getCheckedItemPosition();
		if (position >= 0) {
			clientInfo = clients.get(position);
			requestKey = clientInfo.getRequestKey();
		}
	}

	private Menu menu;
	private ScanIP mScanClient;
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;

		case 0:
			if (isScanning)
				break;
			if (!Utils.isWifiAvaliable(mContext)) {
				dialog = new AlertDialog.Builder(mContext)
						.setMessage(getString(R.string.alert))
						.setMessage(getString(R.string.no_wifi))
						.setPositiveButton(getString(R.string.ok),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								}).show();
				break;
			}
			isScanning = true;
			clients.clear();
			adapter.notifyDataSetChanged();
			if(mScanClient != null) {
				mScanClient.scan();
			}
			scanningLayout.setVisibility(View.VISIBLE);
			localIPTextView.setVisibility(View.GONE);
			noDeviceLayout.setVisibility(View.GONE);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		menu.add(0, 0, 1, "refresh").setIcon(R.drawable.ic_refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_ATMTheme);
		mContext = this;
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(mContext.getString(R.string.choose_device_title));
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.scan);
		if (!Utils.isWifiAvaliable(mContext)) {
			dialog = new AlertDialog.Builder(mContext)
					.setMessage(getString(R.string.alert))
					.setMessage(getString(R.string.no_wifi))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									finish();
								}
							}).show();
			return;
		}
		scanningLayout = (LinearLayout) findViewById(R.id.scanning_layout);
		scanningIP = (TextView) findViewById(R.id.scanning_ip);
		localIPTextView = (TextView) findViewById(R.id.local_ip);
		noDeviceLayout = (TextView) findViewById(R.id.no_device);
		sendLayout = (LinearLayout) findViewById(R.id.send_layout);
		sendLayout.setOnClickListener(this);
		
		pathList = getIntent().getStringArrayListExtra("files");

		deviceListView = (ListView) findViewById(R.id.device_list_view);
		ServerManager manager = ServerManager.getInstance(mContext);
		NanoHTTPD.setTransferObserver(new NanoHTTPD.TransferObserver() {

			@Override
			public void onTransferProgressUpdate(long delta) {
				Message msg = new Message();
				msg.what = TRANSFER_PROGRESS_ID;
				msg.obj = delta;
				handler.sendMessage(msg);
			}
		});
		nanoHttpd = manager.getNanoHttpd();
		if(nanoHttpd == null){
			manager.start();
			nanoHttpd = manager.getNanoHttpd();
		}
			
//		Log.d("server", "server is null :"+(nanoHttpd == null));
		nanoHttpd.setDownloadListener(new TransferServer.DownloadListener() {

			@Override
			public void onDownloadFinish(String fileName, boolean succeed) {
				Message msg = new Message();
				msg.what = DOWNLOAD_RESULT_ID;
				handler.sendMessage(msg);
			}

			@Override
			public Context getContext() {
				return mContext;
			}

			@Override
			public void onDownloadCancel() {
				
			}
		});
		clients = new ArrayList<ScanIP.ClientInfo>();
		adapter = new DeviceAdapter(mContext);
		deviceListView.setAdapter(adapter);
		mScanClient= new ScanIP(ScanActivity.this, scanServerListener);
		mScanClient.scan();
		localIPTextView.setVisibility(View.GONE);
		scanningLayout.setVisibility(View.VISIBLE);
	}

	class DeviceAdapter extends BaseAdapter {
		private Context mContext;
		private LayoutInflater inflater;
		private TextView textView;

		public DeviceAdapter(Context context) {
			this.mContext = context;
			inflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return clients.size();
		}

		@Override
		public Object getItem(int position) {
			return clients.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.device_item, null);
				textView = (TextView) convertView
						.findViewById(R.id.device_name);
			}
			ClientInfo info = (ClientInfo) getItem(position);
			textView.setText(info.getDeviceName() + " ("+info.getIp() + ")" );
			return convertView;
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		dialog = null;
		if (nanoHttpd != null) {
			nanoHttpd.setDownloadListener(null);
		}
		if(mScanClient != null) {
			mScanClient.onDestroy();
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send_layout){
			if (adapter == null)
				return;
			getCheckedItems();
			if (clientInfo == null)
				return;
			finishedSize = 0;
			finishedCount = 0;
			totalSize = 0;
//			Log.d("test", clientInfo.deviceName + "+++" + clientInfo.ip);
			new Thread(new Runnable() {
				@Override
				public void run() {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					File file = null;
					for (int i = 0; i < pathList.size(); i++) {
						file = new File(pathList.get(i));
						totalSize += file.length();
						nameValuePairs.add(new BasicNameValuePair(
								Utils.PARMS_FILE_NAME + i, file.getName()));
					}
					nameValuePairs.add(new BasicNameValuePair(
							Utils.PARMS_FILE_LENGTH, totalSize + ""));
					nameValuePairs.add(new BasicNameValuePair(
							Utils.PARMS_REQUEST_KEY, requestKey));
					String url = "http://" + clientInfo.getIp() + ":"
							+ ServerManager.port + Utils.REQUEST_TO_SEND_FILE;
					NetworkManager networkManager = new NetworkManager(
							mContext, url);
					String result = networkManager.excutePost(nameValuePairs);
					Message msg = new Message();
					msg.obj = result;
					msg.what = CONFIRM_RESULT_ID;
					handler.sendMessage(msg);
				}
			}).start();
		}
	}
}
