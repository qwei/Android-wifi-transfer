package mobi.infolife.wifitransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.Build;
import android.util.Log;


public class NetworkManager {

	private String url;
	private boolean isNetWorkAvailable;
	private AndroidHttpClient httpClient;
	private Context mContext;
	
	public NetworkManager(Context context,String url){
		this.url = url;
		this.mContext = context;
//		this.isNetWorkAvailable = CommonTaskUtils.isNetworkAvaliable(context);
	}
	
//	public String  excuteGet(){
//		String data = Utils.ERROR_CONTENT;
//		httpClient = AndroidHttpClient.newInstance(getUserAgent());
//		HttpGet get = new HttpGet(url) ;
//		
//		HttpResponse response;
//		try {
//			response = httpClient.execute(get);
//			Log.d("test", url+"+++"+response.getStatusLine().getStatusCode());
//			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//				data = EntityUtils.toString(response.getEntity(), "utf-8");
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
////			Log.d("test", "request exception::url="+url);
//		} finally{
//			if(httpClient != null)
//				httpClient.close();
//		}
//		return data;
//	}
	
	public String excutePost(List<NameValuePair> nameValuePairs){
		String data = Utils.ERROR_CONTENT;
		AndroidHttpClient httpclient = AndroidHttpClient.newInstance(getUserAgent());
	    HttpPost httppost = new HttpPost(url);
	    HttpConnectionParams.setSoTimeout(httpclient.getParams(), 2000);
	    try {
	    	if(nameValuePairs != null){
	    		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,"utf-8"));
	    	}
			HttpResponse response = httpclient.execute(httppost);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				data = EntityUtils.toString(response.getEntity(), "utf-8");
			}
		} catch (UnsupportedEncodingException e) {
//			Log.d("httptest", e.toString()+"++++1");
//			e.printStackTrace();
		} catch (ClientProtocolException e) {
//			Log.d("httptest", e.toString()+"++++2");
//			e.printStackTrace();
		} catch (IOException e) {
//			Log.d("httptest", e.toString()+"++++3++"+url);
//			e.printStackTrace();
		} finally{
			if(httpclient != null){
				httpclient.close();
				httpclient = null;
			}
				
		}
	    return data;
	}
	
	public static String getUserAgent(){
		StringBuilder sb = new StringBuilder();
		sb.append("Mozilla/5.0 (Linux; U; Android ");
		sb.append(Build.VERSION.RELEASE);
		sb.append("; ");
		sb.append(Locale.getDefault().toString());
		sb.append("; ");
		sb.append(Build.MODEL);
		sb.append(" Build/");
		sb.append(Build.VERSION.SDK);
		sb.append(")");
		sb.append(" AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
		return sb.toString();
	}
}
