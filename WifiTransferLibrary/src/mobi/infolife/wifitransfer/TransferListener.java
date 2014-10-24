package mobi.infolife.wifitransfer;

public interface TransferListener {

	public void onFileFinish(String fileName,boolean isSuccess);
	public void onFinish();
}
