package com.test.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.multi.thread.download.DownLoadConfig;
import com.multi.thread.download.DownLoader;
import com.multi.thread.download.FileDownLoadManager;

import java.io.File;
import java.math.BigDecimal;

public class MainActivity extends Activity {
    private String url1 = "http://dl.360safe.com/pclianmeng/n/1__6000319__3f7372633d6c6d266c733d6e36396534313835353965__68616f2e3336302e636e__0c4b.exe";
    private String url2 = "https://downc.xiyouence.com/xiyou/package/wqry/2333_34724/mssj.apk";
    private String url3 = "http://static.ilongyuan.cn/imp/implosion_ly_1.2.4.2.apk";
    private ProgressBar pb1,pb2,pb3;
    private DownLoader downLoader1,downLoader2,downLoader3;
    private TextView tv1,tv2,tv3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pb1 = (ProgressBar) findViewById(R.id.pb_progressbar1);
        pb2 = (ProgressBar) findViewById(R.id.pb_progressbar2);
        pb3 = (ProgressBar) findViewById(R.id.pb_progressbar3);
        tv1 = (TextView) findViewById(R.id.total1_tv);
        tv2 = (TextView) findViewById(R.id.total2_tv);
        tv3 = (TextView) findViewById(R.id.total3_tv);
        FileDownLoadManager.getInstance().init(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileDownLoadManager.getInstance().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FileDownLoadManager.getInstance().onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownLoadManager.getInstance().onDestroy();
    }
    public void onClick(View view){
        int viewID = view.getId();
        if(viewID == R.id.start_bt1){
            if(downLoader1!=null) {
                downLoader1.start();
                return;
            }
            downLoader1 = start(url1, new DownLoader.DownLoadCallback() {
                @Override
                public void onStart() {
                    System.out.println("pg1call onStart");
                }

                @Override
                public void onStop() {
                    System.out.println("pg1call onStop");
                }

                @Override
                public void onSuccess(String savePath) {
                    System.out.println("pg1savePath:"+savePath);
                }

                @Override
                public void onFail() {
                    System.out.println("pg1call onFail");
                }

                @Override
                public void onProgress(final float progress, final long downloadLen) {
                    System.out.println("pg1进度："+progress);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pb1.setProgress((int)(1000*progress));
                            tv1.setText(getFormatSize(downloadLen));

                        }
                    });
                }
            },3,(new File(getBasePath(),filterUrlGetFileName(url1))).getAbsolutePath(),null);
        }else if(viewID == R.id.stop_bt1){
            if(downLoader1!=null) downLoader1.stop();
        }else if(viewID == R.id.start_bt2){
            if(downLoader2!=null) {
                downLoader2.start();
                return;
            }
            downLoader2 = start(url2, new DownLoader.DownLoadCallback() {
                @Override
                public void onStart() {

                }

                @Override
                public void onStop() {

                }

                @Override
                public void onSuccess(String savePath) {

                }

                @Override
                public void onFail() {

                }

                @Override
                public void onProgress(final float progress, final long downloadLen) {
                    System.out.println("pg2进度："+progress);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pb2.setProgress((int)(1000*progress));
                            tv2.setText(getFormatSize(downloadLen));
                        }
                    });
                }
            },5,(new File(getBasePath(),filterUrlGetFileName(url2))).getAbsolutePath(),null);
        }else if(viewID == R.id.stop_bt2){
            if(downLoader2!=null) downLoader2.stop();
        }else if(viewID == R.id.start_bt3){
            if(downLoader3!=null) {
                downLoader3.start();
                return;
            }
            downLoader3 = start(url3, new DownLoader.DownLoadCallback() {
                @Override
                public void onStart() {

                }

                @Override
                public void onStop() {

                }

                @Override
                public void onSuccess(String savePath) {

                }

                @Override
                public void onFail() {

                }

                @Override
                public void onProgress(final float progress, final long downloadLen) {
                    System.out.println("pg3进度："+progress);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pb3.setProgress((int)(1000*progress));
                            tv3.setText(getFormatSize(downloadLen));
                        }
                    });
                }
            },10,(new File(getBasePath(),filterUrlGetFileName(url3))).getAbsolutePath(),null);
        }else if(viewID == R.id.stop_bt3){
            if(downLoader3!=null) downLoader3.stop();
        }
    }

    private DownLoader start(String url, DownLoader.DownLoadCallback callback,int burstCount,String savePath,String md5) {
        DownLoadConfig config = new DownLoadConfig();
        config.setCallback(callback);
        config.setBurstCount(burstCount);
        config.setDownloadUrl(url);
        config.setMd5(md5);
        config.setSavePath(savePath);
        DownLoader downLoader = FileDownLoadManager.getInstance().startDownload(config);
        return downLoader;
    }

    public String getBasePath(){
        //判断是否有存储卡
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String pathBase = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"download";
            File file = new File(pathBase);
            if(! file.exists()) file.mkdirs();
            return pathBase;
        }else{
            String pathBase = getFilesDir().getAbsolutePath()+ File.separator+"download";
            File file = new File(pathBase);
            if(!file.exists()){
                file.mkdirs();
            }
            return pathBase;
        }
    }

    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if(kiloByte < 1) {
            return size + "Byte(s)";
        }

        double megaByte = kiloByte/1024;
        if(megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "k/s";
        }

        double gigaByte = megaByte/1024;
        if(gigaByte < 1) {
            BigDecimal result2  = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "m/s";
        }

        double teraBytes = gigaByte/1024;
        if(teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "g/s";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "t/s";
    }

    public static String filterUrlGetFileName(String strUrl){
        return strUrl.substring(strUrl.lastIndexOf("/") + 1);
    }
}
