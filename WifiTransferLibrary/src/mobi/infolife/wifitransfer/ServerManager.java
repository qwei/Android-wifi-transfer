package mobi.infolife.wifitransfer;

import java.io.IOException;

import mobi.infolife.wifitransfer.NanoHTTPD.RequestListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

public class ServerManager {

	private TransferServer nanoHttpd = null;
	public TransferServer getNanoHttpd() {
		return nanoHttpd;
	}
	private static ServerManager manager = null;
	public static int port;
	public static String savePath = Environment.getExternalStorageDirectory()+"";
	private TransferListener transferListener = null;
	private Context mContext = null;
//	private boolean isServerStarted = false;
//	public boolean isServerStarted() {
//		return isServerStarted;
//	}
//	public void setServerStarted(boolean isServerStarted) {
//		this.isServerStarted = isServerStarted;
//	}
	public static ServerManager getInstance(Context context){
		if(manager == null){
			manager = new ServerManager(context);
		}
		return manager;
	}
	private ServerManager(Context context){
		this.mContext = context;
	}
	public static void setPort(int port) {
		ServerManager.port = port;
	}
	public static void setSavePath(String savePath) {
		ServerManager.savePath = savePath;
	}
	public void setListener(TransferListener transferListener) {
		this.transferListener = transferListener;
	}
	public void start(){
		boolean isWifiAvaliable = Utils.isWifiAvaliable(mContext);
		if(!isWifiAvaliable) {
			return;
		}
			
		if(nanoHttpd == null)
			nanoHttpd = new TransferServer(port,mContext,transferListener);
		try {
			nanoHttpd.start();
//			Log.d("server", "start server");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void stop(){
		if(nanoHttpd != null){
			nanoHttpd.stop();
			nanoHttpd = null;
			manager = null;
		}
		
	}
}
