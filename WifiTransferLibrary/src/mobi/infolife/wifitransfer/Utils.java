package mobi.infolife.wifitransfer;

import java.math.BigDecimal;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {

	public static final String ERROR_CONTENT = "error";
//	public static final int PORT = 8080;
	public static final String PARMS_FILE_NAME="file_name";
	public static final String PARMS_FILE_LENGTH = "file_length";
	public static final String PARMS_REQUEST_KEY = "request_key";
	public static final String PARMS_RESULT = "result";
	public static final String CLIENT_RECEIVED_COTENT = "received";
	public static final String PARMS_FILE_FLAG = "flag";
	public static final String PARMS_SUCCEED = "succeed";
	
	
	public static final String GET_HOST_NAME = "/host_name";
	public static final String REQUEST_TO_SEND_FILE = "/confirm";
	public static final String REJECT = "/reject";
	public static final String DOWNLOAD = "/download";
	public static final String RESULT = "/result";
	public static final String CANCEL = "/cancel";
	
	
	public static String formatSize(long size) {
		long g = 1024 * 1024 * 1024 ;
		long m = 1024 * 1024 * 800;
		long k = 1024 * 800;
		String ret = "";
		
		if (size >= m) {
			ret = formatDecimal(((double)size / g), 2) + "GB";
		} else if ((size >= k)&&(size < m)) {
			ret = formatDecimal(((double)size / (m/800)), 2) + "MB";
		} else if (size < k) {
			ret = formatDecimal(((double)size / (k/800)), 2) + "KB";
		}
		
		return ret;
	}
	
	public static String formatDecimal(double d, int scale) {
		BigDecimal bd = new BigDecimal(d);
		return bd.setScale(scale, BigDecimal.ROUND_HALF_DOWN).toString();
	}
	  public static String getLocDeviceName() {
	    return android.os.Build.MODEL.trim();
	  }
	  
	  public static String encodeRequestKey(String ip){
		  String result = "";
		  result += ip +getLocDeviceName()+System.currentTimeMillis();
		  int random = (int)(100000 * Math.random());
		  result += random;
		  
		  return Hash.md5(result);
	  }
	  
	  public static boolean isWifiAvaliable(Context context) {
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				NetworkInfo[] networks = cm.getAllNetworkInfo();
				if (networks != null) {
					for (int i = 0; i < networks.length; i++) {
						if (networks[i].getState() == NetworkInfo.State.CONNECTED) {
							if (networks[i].getTypeName().equals("WIFI"))
								return true;
						}

					}
				}
			}
			return false;
		}
}