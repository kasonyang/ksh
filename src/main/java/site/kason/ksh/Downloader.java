package site.kason.ksh;

import kalang.type.Function1;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {

    public static class Progress {
        private long totalSize;
        private long downloadedSize;
        private long speed;
        public Progress(long totalSize, long downloadedSize,long speed) {
            this.totalSize = totalSize;
            this.downloadedSize = downloadedSize;
            this.speed = speed;
        }
        public long getTotalSize() {
            return totalSize;
        }
        public long getDownloadedSize() {
            return downloadedSize;
        }
        public long getSpeed() {
            return speed;
        }
    }

    public void download(String url, File localFile,@Nullable Function1<Void,Progress> callback) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        long contentLen = conn.getContentLengthLong();
        long downloadedSize = 0;
        int bufferSize = 4096 * 10;
        try (InputStream is = conn.getInputStream();FileOutputStream os = new FileOutputStream(localFile)) {
            byte[] buffer = new byte[bufferSize];
            long startTime = System.currentTimeMillis();
            while(true) {
                long loopStartTime = System.currentTimeMillis();
                int rlen = is.read(buffer);
                if (rlen<=-1) {
                    break;
                }
                long loopEndTime = System.currentTimeMillis();
                downloadedSize += rlen;
                if (rlen>0) {
                    os.write(buffer,0,rlen);
                    if (callback!=null) {
                        long speed;
                        if (loopEndTime>loopStartTime) {
                            speed = rlen * 1000 / (loopEndTime-loopStartTime);
                        } else if (loopEndTime>startTime) {
                            speed = downloadedSize * 1000 / (loopEndTime - startTime);
                        } else {
                            speed = Long.MAX_VALUE;
                        }
                        callback.call(new Progress(contentLen,downloadedSize,speed));
                    }
                }
            }
        }
    }

    public void download(String url,File localFile) throws IOException {
        download(url,localFile,null);
    }

}
