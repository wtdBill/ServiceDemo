package com.example.ypp0623.servicedemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    public static int threadCount = 10;

    public static int runningThreadCount;
    private String path = "http://s1.music.126.net/download/android/CloudMusic_3.4.1.133604_official.apk";
    private volatile boolean isDownload = true;
    private MyBinder myBinder = new MyBinder();

    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //下载
    public void download() {
        new Thread() {
            public void run() {
                try {
                    //1.获取服务器上的目标文件的大小
                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        int length = conn.getContentLength();
                        System.out.println("服务器文件的长度为:" + length);
                        //2.在本地创建一个跟原始文件同样大小的文件
                        RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getFileName(path), "rw");
                        raf.setLength(length);
                        raf.close();
                        //3.计算每个线程下载的起始位置和结束位置
                        int blocksize = length / threadCount;
                        runningThreadCount = threadCount;
                        for (int threadId = 0; threadId < threadCount; threadId++) {
                            int startIndex = threadId * blocksize;
                            int endIndex = (threadId + 1) * blocksize - 1;
                            if (threadId == (threadCount - 1)) {
                                endIndex = length - 1;
                            }
                            //4.开启多个子线程开始下载
                            new DownloadThread(threadId, startIndex, endIndex).start();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private class DownloadThread extends Thread {
        /**
         * 线程id
         */
        private int threadId;
        /**
         * 线程下载的理论开始位置
         */
        private int startIndex;
        /**
         * 线程下载的结束位置
         */
        private int endIndex;
        /**
         * 当前线程下载到文件的那一个位置了.
         */
        private int currentPosition;

        public DownloadThread(int threadId, int startIndex, int endIndex) {
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            System.out.println(threadId + "号线程下载的范围为:" + startIndex
                    + "   ~~   " + endIndex);
            currentPosition = startIndex;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //检查当前线程是否已经下载过一部分的数据了
                File info = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + threadId + ".position");
                RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getFileName(path), "rw");
                if (info.exists() && info.length() > 0) {
                    FileInputStream fis = new FileInputStream(info);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    currentPosition = Integer.valueOf(br.readLine());
                    conn.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endIndex);
                    System.out.println("原来有下载进度,从上一次终止的位置继续下载" + "bytes=" + currentPosition + "-" + endIndex);
                    fis.close();
                    raf.seek(currentPosition);//每个线程写文件的开始位置都是不一样的.
                } else {
                    //告诉服务器 只想下载资源的一部分
                    conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
                    System.out.println("原来没有有下载进度,新的下载" + "bytes=" + startIndex + "-" + endIndex);
                    raf.seek(startIndex);//每个线程写文件的开始位置都是不一样的.
                }
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    //把每个线程下载的数据放在自己的空间里面.
//                    System.out.println("线程:"+threadId+"正在下载:"+new String(buffer));
                    raf.write(buffer, 0, len);
                    //5.记录下载进度
                    currentPosition += len;
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + threadId + ".position");
                    RandomAccessFile fos = new RandomAccessFile(file, "rwd");
                    //System.out.println("线程:"+threadId+"写到了"+currentPosition);
                    fos.write(String.valueOf(currentPosition).getBytes());
                    fos.close();//fileoutstream数据是不一定被写入到底层设备里面的,有可能是存储在缓存里面.
                    //raf 的 rwd模式,数据是立刻被存储到底层硬盘设备里面.

                    //更新进度条的显示
                    int max = endIndex - startIndex;
                    int progress = currentPosition - startIndex;
                    if (threadId == 0) {
//                        pb0.setMax(max);
//                        pb0.setProgress(progress);
                        Log.d("down", progress + "");
                    } else if (threadId == 1) {
//                        pb1.setMax(max);
//                        pb1.setProgress(progress);
                        Log.d("down", progress + "");
                    } else if (threadId == 2) {
//                        pb2.setMax(max);
//                        pb2.setProgress(progress);
                        Log.d("down", progress + "");
                    }
                }
                raf.close();
                is.close();
                System.out.println("线程:" + threadId + "下载完毕了...");
                File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + threadId + ".position");
                f.renameTo(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + threadId + ".position.finish"));
                synchronized (MainActivity.class) {
                    runningThreadCount--;
                    //6.删除临时文件
                    if (runningThreadCount <= 0) {
                        for (int i = 0; i < threadCount; i++) {
                            File ft = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + i + ".position.finish");
                            ft.delete();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取一个文件名称
     *
     * @param path
     * @return
     */
    public String getFileName(String path) {
        int start = path.lastIndexOf("/") + 1;
        return path.substring(start);
    }

    class MyBinder extends Binder {
        public void cancelDownload() {
            isDownload = false;
            DownloadThread.interrupted();
        }

        public void startDownload() {
            isDownload = true;
            download();
        }
    }
}
