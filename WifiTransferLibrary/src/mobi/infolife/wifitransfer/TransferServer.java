package mobi.infolife.wifitransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mobi.infolife.wifitransfer.NanoHTTPD.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TransferServer extends NanoHTTPD {
	private static final int REQUEST_FOR_CONFIRM_ID = 111;
	private static final int DOWNLOAD_ONE_FILE_ID = 222;

	public static final int DOWNLOAD_BUFFER_SIZE = 4 * 1024;

	private Context mContext;
	private DownloadListener downloadListener;
	private ProgressDialog mProgressDialog;
	private String requestKey;
	private long totalSize;
	private List<String> downloadFileName = new ArrayList<String>();
	private DownloadTask downloadTask = null;
	private AlertDialog confirmDialog = null;
	private TransferListener transferListener;
	
    public interface DownloadListener{
    	void onDownloadFinish(String fileName, boolean succeed);
    	void onDownloadCancel();
    	Context getContext();
    }
	
	public void setDownloadListener(DownloadListener downloadListener) {
		this.downloadListener = downloadListener;
	}
	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case REQUEST_FOR_CONFIRM_ID:
				Context context = null;
				if(downloadListener != null && downloadListener.getContext() != null) {
					context = downloadListener.getContext();
				} else {
					context = mContext;
				}
				mProgressDialog = new ProgressDialog(context);
				mProgressDialog.setMessage("Transferring");
				mProgressDialog.setIndeterminate(true);
				mProgressDialog
						.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.setCanceledOnTouchOutside(false);
				mProgressDialog.setCancelable(true);

				int downloadFileCount = downloadFileName.size();
				confirmDialog = new AlertDialog.Builder(context)
						.setTitle(mContext.getString(R.string.confirm))
						.setMessage(
								clientIp + " " + mContext.getString(R.string.will_send) + " "
										+ downloadFileName.get(downloadFileCount - 1)
										+(downloadFileCount > 1 ? "...\n"+downloadFileCount+mContext.getString(R.string.item):"")
										+ "\n"+mContext.getString(R.string.size)
										+ Utils.formatSize(totalSize))
						.setPositiveButton(mContext.getString(R.string.ok),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										downloadTask = new DownloadTask(
												requestKey);
										downloadTask.execute("");
										mProgressDialog
												.setOnCancelListener(new DialogInterface.OnCancelListener() {

													@Override
													public void onCancel(
															DialogInterface dialog) {
														downloadTask
																.cancel(true);
													}
												});
									}
								})
						.setNegativeButton(mContext.getString(R.string.cancel),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										new Thread(new Runnable() {

											@Override
											public void run() {
												NetworkManager manager = new NetworkManager(
														mContext,
														"http://"
																+ TransferServer.super.clientIp
																+ ":"
																+ ServerManager.port
																+ Utils.REJECT);
												manager.excutePost(null);
											}
										}).start();
									}
								}).show();
				confirmDialog.setCanceledOnTouchOutside(false);
				break;
			case DOWNLOAD_ONE_FILE_ID:
				Bundle data = msg.getData();
				String fileName = data.getString(Utils.PARMS_FILE_NAME);
				boolean succeed = data.getBoolean(Utils.PARMS_SUCCEED);
				if (transferListener != null)
					transferListener.onFileFinish(fileName,succeed);
				break;
			}

		};
	};

	public class DownloadTask extends AsyncTask<String, Integer, String> {

		private String requestKey;

		public DownloadTask(String requestKey) {
			this.requestKey = requestKey;
		}

		@Override
		protected String doInBackground(String... params) {
			long total = 0;
			for (int i = 0; i < downloadFileName.size(); i++) {
				String fileName = downloadFileName.get(i);
				String path = "http://" + clientIp + ":" + ServerManager.port
						+ Utils.DOWNLOAD;
				List<NameValuePair> nameValuePairs1 = new ArrayList<NameValuePair>();
				nameValuePairs1.add(new BasicNameValuePair(
						Utils.PARMS_FILE_FLAG, i + ""));
				nameValuePairs1.add(new BasicNameValuePair(
						Utils.PARMS_REQUEST_KEY, requestKey));
				String savePath = ServerManager.savePath + "/" + fileName;
				InputStream inputStream = null;
				OutputStream outputStream = null;
				boolean succeed = false;
				AndroidHttpClient httpclient = AndroidHttpClient
						.newInstance(NetworkManager.getUserAgent());
				httpclient.getParams().setParameter("http.socket.timeout", new Integer(60000));
				HttpPost httppost = new HttpPost(path);
				try {
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs1,"utf-8"));
					HttpResponse response = httpclient.execute(httppost);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						inputStream = response.getEntity().getContent();
						
						File file = new File(savePath);
						if (!file.exists())
							file.createNewFile();
						outputStream = new FileOutputStream(file);
						byte buffer[] = new byte[DOWNLOAD_BUFFER_SIZE];
						int temp = -1;
						temp = inputStream
								.read(buffer, 0, DOWNLOAD_BUFFER_SIZE);
						if(temp == -1){
							inputStream.close();
							return null;
						}

						while (temp != -1) {
//							Log.d("test", "downloading..." + fileName + "+++"
//									+ isCancelled());
							if (isCancelled()) {
								inputStream.close();
								return null;
							}
							total += temp;
							publishProgress((int) (total * 100 / totalSize));
							outputStream.write(buffer, 0, temp);
							temp = inputStream.read(buffer, 0,
									DOWNLOAD_BUFFER_SIZE);
						}
						outputStream.flush();
						succeed = true;
					}
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(httpclient != null){
						httpclient.close();
						httpclient = null;
					}
					try {
						if (outputStream != null)
							outputStream.close();
						if (inputStream != null)
							inputStream.close();
					} catch (IOException ignored) {
					}
					NetworkManager manager;
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair(
							Utils.PARMS_RESULT, succeed + ""));
					nameValuePairs.add(new BasicNameValuePair(
							Utils.PARMS_REQUEST_KEY, requestKey));
					nameValuePairs.add(new BasicNameValuePair(
							Utils.PARMS_FILE_NAME, fileName));

					String url1 = "http://" + clientIp + ":"
							+ ServerManager.port + Utils.RESULT;
					manager = new NetworkManager(mContext, url1);
					manager.excutePost(nameValuePairs);

					Message msg = new Message();
					msg.what = DOWNLOAD_ONE_FILE_ID;
					Bundle bundle = new Bundle();
					bundle.putBoolean(Utils.PARMS_SUCCEED, succeed);
					bundle.putString(Utils.PARMS_FILE_NAME, fileName);
					msg.setData(bundle);
					handler.sendMessage(msg);
				}

			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgress(progress[0]);
			if(!Utils.isWifiAvaliable(mContext)){
				this.cancel(true);
			}
				
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mProgressDialog.dismiss();
//			Toast.makeText(mContext, "succeed!", Toast.LENGTH_SHORT).show();
			if (transferListener != null) {
				transferListener.onFinish();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.show();
		}
	}

	public TransferServer(int port) {
		super(port);
		registerReceiver();
	}

	public TransferServer(String hostName, int port) {
		super(hostName, port);
		registerReceiver();
	}

	public TransferServer(int port, Context context) {
		super(port, context);
		this.mContext = context;
		registerReceiver();
	}
	
	public TransferServer(int port, Context context, TransferListener listener) {
		super(port, context);
		this.mContext = context;
		this.transferListener = listener;
		registerReceiver();
	}

	private Response responseForDeviceName() {
		String msg = Utils.getLocDeviceName();
		return new NanoHTTPD.Response(msg);
	}

	private Response responseForConfirm(IHTTPSession session) {
		Map<String, String> parms = session.getParms();
		downloadFileName.clear();
		if (parms.get(Utils.PARMS_REQUEST_KEY) == null
				|| parms.get(Utils.PARMS_FILE_LENGTH) == null
				|| parms.get(Utils.PARMS_FILE_NAME + "0") == null)
			return new NanoHTTPD.Response(Utils.ERROR_CONTENT);
		totalSize = Long.parseLong(parms.get(Utils.PARMS_FILE_LENGTH));
		requestKey = parms.get(Utils.PARMS_REQUEST_KEY);
		int downloadFileCount = parms.size() - 3;
		for (int i = 0; i < downloadFileCount; i++) {
//			Log.d("test", parms.get(Utils.PARMS_FILE_NAME + i));
			downloadFileName.add(parms.get(Utils.PARMS_FILE_NAME + i));
		}
		Message msg = new Message();
		msg.what = REQUEST_FOR_CONFIRM_ID;
		handler.sendMessage(msg);
		return new NanoHTTPD.Response(Utils.CLIENT_RECEIVED_COTENT);
	}

	private Response responseForReject() {
		return null;
	}

	private Response responseForDownload(IHTTPSession session) {
		Map<String, String> parms = session.getParms();
		if (parms.get(Utils.PARMS_FILE_FLAG) == null
				|| !ScanActivity.requestKey.equals(parms
						.get(Utils.PARMS_REQUEST_KEY)))
			return new NanoHTTPD.Response(Utils.ERROR_CONTENT);
		InputStream data = null;
		int downloadId = Integer.parseInt(parms.get(Utils.PARMS_FILE_FLAG));
		try {
			String path = ScanActivity.pathList.get(downloadId);
			data = new FileInputStream(new File(path));
			Response res = new Response(Status.OK, "multipart/form-data", data);
			return res;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return new NanoHTTPD.Response(Utils.ERROR_CONTENT);
	}

	private Response responseForResult(IHTTPSession session) {
		Map<String, String> parms = session.getParms();
		if (parms.get(Utils.PARMS_FILE_NAME) == null
				|| !ScanActivity.requestKey.equals(parms
						.get(Utils.PARMS_REQUEST_KEY))
				|| parms.get(Utils.PARMS_RESULT) == null)
			return new NanoHTTPD.Response(Utils.ERROR_CONTENT);
		if(downloadListener != null){
			downloadListener.onDownloadFinish(parms.get(Utils.PARMS_FILE_NAME),
					"true".equals(parms.get(Utils.PARMS_RESULT)));
		}
		return super.serve(session);
	}

	private Response responseForCancel(IHTTPSession session) {
		Map<String, String> parms = session.getParms();
		if (!requestKey.equals(parms.get(Utils.PARMS_REQUEST_KEY))) {
			return new NanoHTTPD.Response(Utils.ERROR_CONTENT);
		}
		if (downloadTask != null)
			downloadTask.cancel(true);

		if (mProgressDialog != null && mProgressDialog.isShowing())
			mProgressDialog.dismiss();

		if (confirmDialog != null && confirmDialog.isShowing()) {
			confirmDialog.dismiss();
			confirmDialog = null;
		}

		return super.serve(session);
	}

	public Response serve(IHTTPSession session) {
		super.serve(session);
//		Map<String, String> parms = session.getParms();
		String uri = session.getUri();
//		Log.d("httptest", uri + "+++++" + parms.size());
		if (Utils.GET_HOST_NAME.equals(uri)) {
			return responseForDeviceName();
		} else if (Utils.REQUEST_TO_SEND_FILE.equals(uri)) {
			return responseForConfirm(session);
		} else if (Utils.REJECT.equals(uri)) {
			return responseForReject();
		} else if (Utils.DOWNLOAD.equals(uri)) {
			return responseForDownload(session);
		} else if (Utils.RESULT.equals(uri)) {
			return responseForResult(session);
		} else if (Utils.CANCEL.equals(uri)) {
			responseForCancel(session);
		}
		return new NanoHTTPD.Response(Utils.ERROR_CONTENT);

	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
		    if (action.equals("android.net.wifi.STATE_CHANGE")) {
		    	if(!Utils.isWifiAvaliable(mContext)){
		    		stop();
		    	}
		    }
		}
	};

	public void stop() {
		try{
			mContext.unregisterReceiver(receiver);
		} catch(Exception e){
			
		}
		
		super.stop();
	};
	private void registerReceiver(){
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intentFilter.addAction("android.net.wifi.STATE_CHANGE");
		mContext.registerReceiver(receiver, intentFilter);
	}
}
