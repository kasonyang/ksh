package site.kason.ksh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractor {

    public File[] extract(File zipFile,File destDir) throws IOException {
        return extract(zipFile,destDir,false);
    }

    public File[] extract(File zipFile,File destDir,boolean allowAutoMkDir) throws IOException {
        List<File> result = new LinkedList<>();
        ZipFile zf = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> ens = zf.entries();
        if (!destDir.exists() && !allowAutoMkDir) {
            throw new IOException("destination not found");
        }
        if(!destDir.exists() && !destDir.mkdirs()){
            throw new IOException("unable to create directory:"+destDir);
        }
        byte[] data = new byte[4096];
        while(ens.hasMoreElements()){
            ZipEntry en = ens.nextElement();
            File enFile = new File(destDir, en.getName());
            if (enFile.getParentFile().equals(destDir)) {
                result.add(enFile);
            }
            if(en.isDirectory()){
                if(!enFile.exists() && !enFile.mkdirs()){
                    throw new IOException("failed to create directory");
                }
            }else{
                File outDir = enFile.getParentFile();
                if( !outDir.exists() && !outDir.mkdirs() ){
                    throw new IOException("unable to create directory:"+outDir.getAbsolutePath());
                }
                InputStream is = zf.getInputStream(en);
                FileOutputStream os = new FileOutputStream(enFile);
                int len;
                while((len = is.read(data))>0){
                    os.write(data,0,len);
                }
                is.close();
                os.close();
            }
        }
        zf.close();
        return result.toArray(new File[0]);
    }


}
