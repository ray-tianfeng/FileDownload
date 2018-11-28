package com.multi.thread.download;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.multi.thread.download.util.DataFormatUtils;
import com.multi.thread.download.util.FileUtils;
import com.multi.thread.download.util.IOUtils;
import com.multi.thread.download.util.MD5Utils;
import com.multi.thread.download.util.SharedPUtils;
import com.multi.thread.download.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 下载对象
 * @author zl
 * @time 2018/11/16 0016.
 */

public class DownLoader {
    private DownLoadConfig config;//配置
    private CopyOnWriteArrayList<Stem> stems;//下载线程句柄
    private Context mContext;
    public DownLoader(DownLoadConfig config,Context mContext) {
        this.config = config;
        this.mContext = mContext;
        stems = new CopyOnWriteArrayList<>();
    }

    public void start(){//开始下载
        HttpUtil.getInstance().httpGet(config.getDownloadUrl(), new HashMap(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                config.getCallback().onFail();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.code() != 200) {
                        config.getCallback().onFail();
                        IOUtils.close(response.body());
                        return;
                    }
                    long fileLength = response.body().contentLength();
                    if(fileLength < 1){
                        config.getCallback().onFail();
                       return;
                    }
                    config.setFileLength(fileLength);
                    loadLocalConfig();//未获到本地配置
                    config.burst();//分片
                    config.getCallback().onStart();//通知开始下载
                    for(int i=0;i<config.getBurstCount();i++){
                        DownLoadConfig.Burst bt = config.getBursts().get(i);
                        downloadFileByRange(bt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getCallback().onFail();
                }
            }
        });
    }

    private boolean loadLocalConfig() {//本地缓存
        String burstsStr = SharedPUtils.getString(mContext, config.makeFile(),"");
        SharedPUtils.getString(mContext, MD5Utils.md5(config.getDownloadUrl()),"");
        if(StringUtils.isEmpty(burstsStr) || !FileUtils.isExits(config.getSavePath())) return false;
        ArrayList<DownLoadConfig.Burst> bursts = (ArrayList<DownLoadConfig.Burst>) JSON.parseArray(burstsStr,DownLoadConfig.Burst.class);
        config.getBursts().clear();
        config.getBursts().addAll(bursts);
        return true;
    }

    /**
     * 下载分片
     * @param bt 分片配置
     * @throws FileNotFoundException
     */
    private void downloadFileByRange(DownLoadConfig.Burst bt) throws FileNotFoundException {
        Call call = HttpUtil.getInstance().downloadFileByRange(config.getDownloadUrl(), bt.getStartIndex()+bt.getDownloadIndex(), bt.getEndIndex(), new FileDownloadRequestCallback(bt,config.getSavePath()));
        Stem stem = new Stem();
        stem.setCall(call);
        stem.setDownloadLength(0L);
        stem.setBurst(bt);
        stems.add(stem);
    }

    /**
     * 同步当前下载进度
     * @return boolean true同步成功 false同步失败，下载完成后会同步失败
     */
    public boolean syncDownloadProgress(){
        if(stems==null || stems.size()==0) return true;
        long downloadTotal = 0L;
        for(DownLoadConfig.Burst burst:config.getBursts()){
            downloadTotal += burst.getDownloadIndex();
        }
        float progress = DataFormatUtils.formatFloat(downloadTotal,0F)/ DataFormatUtils.formatFloat(config.getFileLength(),1F);
        progress = new BigDecimal(progress).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        config.getCallback().onProgress(progress);
        if(progress == 1){
            if(StringUtils.isEmpty(config.getMd5()) && !MD5Utils.md5(config.getSavePath()).equals(config.getMd5())){//验证md5值
                config.getCallback().onFail();
                return false;
            }
            config.getCallback().onSuccess(config.getSavePath());
            SharedPUtils.putString(mContext,config.makeFile(),"");
            return false;
        }
        return true;
    }

    /**
     * 通过句柄检查线程下载是否暂停，并对暂停线程重启
     * @throws FileNotFoundException
     */
    public void checkOrRestartStem() throws FileNotFoundException {
        for(Stem stem:stems){
            if(stem.getDownloadLength() == stem.getBurst().getDownloadIndex()){
                if(!stem.getCall().isCanceled() && stem.getCall().isExecuted()){
                    stem.getCall().cancel();
                }
                if(stem.getBurst().getDownloadIndex()<stem.getBurst().getEndIndex())
                    downloadFileByRange(stem.getBurst());
                stems.remove(stem);
            }else{
                stem.setDownloadLength(stem.getBurst().getDownloadIndex());
            }
        }
    }

    /**
     * 停止下载
     */
    public void stop(){
        if(stems == null) return;
        for(Stem stem:stems){
            if(!stem.getCall().isCanceled() && stem.getCall().isExecuted()){
                stem.getCall().cancel();
            }
        }
        stems.clear();
        config.getCallback().onStop();
    }

    /**
     * 保存下载状态
     */
    public void saveConfig(){
        if(config==null || StringUtils.isEmpty(config.getDownloadUrl()) || config.getBursts()==null || config.getBursts().size()<1) return;
        SharedPUtils.putString(mContext,config.makeFile(), JSON.toJSONString(config.getBursts()));
    }

    public DownLoadConfig getConfig() {
        return config;
    }

    public class Stem{//下载线程句柄
        private Call call;//请求对象Call
        private DownLoadConfig.Burst burst;//当前现在进度
        private long downloadLength;//上次同步下载进度

        public Call getCall() {
            return call;
        }

        public void setCall(Call call) {
            this.call = call;
        }

        public long getDownloadLength() {
            return downloadLength;
        }

        public void setDownloadLength(long downloadLength) {
            this.downloadLength = downloadLength;
        }

        public DownLoadConfig.Burst getBurst() {
            return burst;
        }

        public void setBurst(DownLoadConfig.Burst burst) {
            this.burst = burst;
        }
    }

    public interface DownLoadCallback{
        void onStart();
        void onStop();
        void onSuccess(String savePath);
        void onFail();
        void onProgress(float progress);
    }
}
