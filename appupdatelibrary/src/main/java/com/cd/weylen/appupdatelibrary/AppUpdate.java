package com.cd.weylen.appupdatelibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

/**
 * Created by weylen on 2016-09-07.
 */
public class AppUpdate {

    private String downloadUrl;
    private Context context;
    private String saveFile;
    private String message;
    private boolean isMust;
    private OnUpdateCallbackListener onUpdateCallbackListener;

    public AppUpdate(Context context){
        this.context = context;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(String saveFile) {
        this.saveFile = saveFile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isMust() {
        return isMust;
    }

    public void setMust(boolean must) {
        isMust = must;
    }

    public void setOnUpdateCallbackListener(OnUpdateCallbackListener onUpdateCallbackListener) {
        this.onUpdateCallbackListener = onUpdateCallbackListener;
    }

    public void show(){
        String text = TextUtils.isEmpty(message) ? "发现新版本，请立即升级" : message;
        if (isMust){
            text += "<br/><br/><font color='red'>提示：您必须更新之后才能继续使用！</font>";
        }
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("发现新版本")
                .setMessage(Html.fromHtml(text).toString())
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onUpdateCallbackListener != null){
                            onUpdateCallbackListener.onCancel(isMust);
                        }
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("立即升级", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        download();
                    }
                }).create();
        if (isMust){
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }
        dialog.show();
    }

    private void download(){
        // 检查储存卡是否可用
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            if (onUpdateCallbackListener != null){
                onUpdateCallbackListener.disableMemory(isMust);
            }
            return;
        }

        View contentView = LayoutInflater.from(context).inflate(R.layout.download_view, null);
        ProgressBar progressBar = (ProgressBar) contentView.findViewById(R.id.progressBar);
        TextView percent = (TextView) contentView.findViewById(R.id.percent);

        final Download download = new Download(progressBar, percent, saveFile);

        final AlertDialog downloadDialog = new AlertDialog.Builder(context)
                .setTitle("下载中")
                .setView(contentView)
                .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onUpdateCallbackListener != null){
                            onUpdateCallbackListener.onCancel(isMust);
                        }
                        download.cancel();
                        dialog.dismiss();
                    }
                })
                .create();

        downloadDialog.setCanceledOnTouchOutside(false);
        downloadDialog.setCancelable(false);
        downloadDialog.show();

        Download.DownloadListener listener = new Download.DownloadListener() {
            @Override
            public void onDownloadComplete(String path) {
                if (TextUtils.isEmpty(path)){
                    downloadDialog.setTitle("下载失败");
                    if (onUpdateCallbackListener != null){
                        onUpdateCallbackListener.downloadFailure(isMust);
                    }
                    return;
                }

                downloadDialog.dismiss();

                // 安装apk
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
                context.startActivity(intent);
            }
        };
        download.setDownloadListener(listener);
        download.execute(downloadUrl);
    }

    public interface OnUpdateCallbackListener{
        /**
         * 取消更新
         * @param isMust 是否强制更新
         */
        void onCancel(boolean isMust);

        /**
         * 储存卡不可用
         * @param isMust
         */
        void disableMemory(boolean isMust);

        /**
         * 下载失败
         */
        void downloadFailure(boolean isMust);
    }

    public static class Builder{
        private String downloadUrl;
        private Context context;
        private String saveFile;
        private String message;
        private boolean isMust;
        private OnUpdateCallbackListener onUpdateCallbackListener;

        public Builder(Context context){
            this.context = context;
        }

        public Builder downloadUrl(String downloadUrl){
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder message(String message){
            this.message = message;
            return this;
        }

        public Builder isMust(boolean isMust){
            this.isMust = isMust;
            return this;
        }

        public Builder callback(OnUpdateCallbackListener onUpdateCallbackListener){
            this.onUpdateCallbackListener = onUpdateCallbackListener;
            return this;
        }

        public AppUpdate create(){
            AppUpdate appUpdate = new AppUpdate(context);
            appUpdate.setDownloadUrl(downloadUrl);
            appUpdate.setMessage(message);
            appUpdate.setMust(isMust);
            appUpdate.setSaveFile(saveFile);
            appUpdate.setOnUpdateCallbackListener(onUpdateCallbackListener);
            return appUpdate;
        }
    }
}
