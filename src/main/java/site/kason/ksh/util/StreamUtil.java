package site.kason.ksh.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author KasonYang
 */
public class StreamUtil {

    public static String readToString(InputStream is, String charset) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int rlen;
        while ((rlen = is.read(buffer)) > 0) {
            bos.write(buffer, 0, rlen);
        }
        return bos.toString(charset);
    }

}
