package com.example.wifitransfertest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import mobi.infolife.wifitransfer.ScanActivity;
import mobi.infolife.wifitransfer.ServerManager;
import mobi.infolife.wifitransfer.TransferListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	private int port = 8089;
	Button btn;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btn = (Button) findViewById(R.id.send);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ArrayList<String> pathList = new ArrayList<String>();
				pathList.add("//sdcard/2.apk");
				pathList.add("//sdcard/fqrouter-latest.apk");
				pathList.add("//sdcard/Battery Defender-mobi.infolife.batterysaver-104-v1.1.7.apk");
				Intent intent = new Intent(MainActivity.this,ScanActivity.class);
				intent.putExtra("files", pathList);
//				intent.putExtra("port", port);
				startActivity(intent);
			}
		});
		ServerManager manager = ServerManager.getInstance(this);
		ServerManager.setPort(port);
		ServerManager.setSavePath(Environment.getExternalStorageDirectory()+"");
		manager.setListener(new TransferListener() {
			
			@Override
			public void onFileFinish(String fileName,boolean isSuccess) {
				
			}
			@Override
			public void onFinish() {
				
			}
		});
		manager.start();
		String str = "a+b";
		try {  
            String decoded = URLDecoder.decode(str, "UTF8");
            Log.d("test", decoded);
        } catch (UnsupportedEncodingException ignored) {  
        } 
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ServerManager.getInstance(this).stop();
	}
}
