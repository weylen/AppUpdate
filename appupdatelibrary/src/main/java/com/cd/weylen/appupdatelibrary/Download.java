package com.cd.weylen.appupdatelibrary;

import android.os.AsyncTask;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Created by weylen on 2016-09-07.
 */
public class Download extends AsyncTask<String, Integer, String>{

    private DownloadListener downloadListener;
    private ProgressBar progressBar;
    private TextView progressView;
    private String fileName;
    private String saveFile;
    private boolean isCancel;

    public Download(ProgressBar progressBar, TextView progressView, String saveFile){
        this.progressBar = progressBar;
        this.progressView = progressView;
        this.saveFile = saveFile;
    }

    public void cancel(){
        super.cancel(true);
        isCancel = true;
        try{
            File file = new File(saveFile, fileName);
            if (file.exists()){
                file.delete();
            }
        }catch (Exception e){}
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length > 0){
            String downloadUrl = params[0];
            if (URLUtil.isHttpsUrl(downloadUrl) || URLUtil.isHttpUrl(downloadUrl)){
                fileName = getFileName(downloadUrl);

                try {
                    URLConnection connection = new URL(downloadUrl).openConnection();
                    InputStream inputStream = connection.getInputStream();

                    File file = new File(saveFile, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    byte bytes[] = new byte[2048];
                    int i;
                    int maxLength = connection.getContentLength();

                    while((i = inputStream.read(bytes)) != -1 && !isCancel){
                        fos.write(bytes, 0, i);
                        fos.flush();
                        publishProgress(maxLength, i);
                    }

                    if (isCancel){
                        // 删除文件
                        file.delete();
                    }

                    fos.close();
                    inputStream.close();

                    return file.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getFileName(String url){
        try {
            url = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            if (url.contains("/")){
                url = url.substring(url.lastIndexOf("/")+1);
            }
        }
        return url;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (progressBar != null && values.length == 2){
            progressBar.setMax(values[0]);
            progressBar.incrementProgressBy(values[1]);
            progressView.setText(getRatio(progressBar.getMax(), progressBar.getProgress()));
        }
    }

    private String getRatio(Integer... values){
        return values[1] * 100/ values[0] +"%";
    }

    @Override
    protected void onPostExecute(String s) {
        if (downloadListener != null){
            downloadListener.onDownloadComplete(s);
        }
    }

    public interface DownloadListener{
        void onDownloadComplete(String path);
    }
}
