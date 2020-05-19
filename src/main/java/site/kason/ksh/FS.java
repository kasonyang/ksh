package site.kason.ksh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class FS {

    public static void mkdir(File directory) throws IOException {
        FileUtils.forceMkdir(directory);
    }

    public static void copyTo(File src, File destDir) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("file not found:" + src);
        }
        if (src.isFile()) {
            FileUtils.copyFileToDirectory(src, destDir, true);
        } else if (src.isDirectory()) {
            FileUtils.copyDirectory(src, destDir);
        } else {
            throw new IllegalArgumentException("unknown file type:" + src);
        }
    }

    public static void copy(File src, File dest) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("file not found:" + src);
        }
        if (src.isFile()) {
            FileUtils.copyFile(src, dest, true);
        } else if (src.isDirectory()) {
            FileUtils.copyDirectory(src, dest);
        } else {
            throw new IllegalArgumentException("unkonwn file type:" + src);
        }
    }

    /**
     * move file/directory to directory
     *
     * @param src            the source file or directory to be moved
     * @param destinationDir the destination directory
     * @param createDestDir  whether creating the destination or not
     * @throws IOException
     */
    public static void moveTo(File src, File destinationDir, boolean createDestDir) throws IOException {
        FileUtils.moveToDirectory(src, destinationDir, createDestDir);
    }

    /**
     * move file/directory to directory,same as moveTo(src,destinationDir,false)
     *
     * @param src            the source file or directory to be moved
     * @param destinationDir the destination directory which should not exists.
     * @throws IOException
     */
    public static void moveTo(File src, File destinationDir) throws IOException {
        moveTo(src, destinationDir, false);
    }

    public static void move(File src, File destination) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("source file not found:" + src);
        }
        if (src.isFile()) {
            FileUtils.moveFile(src, destination);
        } else if (src.isDirectory()) {
            FileUtils.moveDirectory(src, destination);
        } else {
            throw new IllegalArgumentException("unknown file type:" + src);
        }
    }

    public static void delete(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("file not found:" + file);
        }
        if (file.isFile()) {
            FileUtils.forceDelete(file);
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (File c : children) {
                delete(c);
            }
            FileUtils.deleteDirectory(file);
        }
    }

    public static String baseName(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String extensionName(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static String path(String[] parts) {
        if (parts.length == 0) {
            throw new IllegalArgumentException("empty array");
        }
        File file = new File(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            file = new File(file, parts[i]);
        }
        return file.getPath();
    }

    public static String getUserDirectoryPath() {
        return FileUtils.getUserDirectoryPath();
    }

    public static File getUserDirectory() {
        return new File(getUserDirectoryPath());
    }

    public static String getTempDirectoryPath() {
        return FileUtils.getTempDirectoryPath();
    }

    public static File getTempDirectory() {
        return new File(getTempDirectoryPath());
    }

    public static String getSizeDesc(long size) {
        if (size<1024) {
            return size + "B";
        } else if (size < 1024*1024) {
            return String.format("%.2fKB",size/1024f);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB",size/(1024*1024f));
        } else {
            return String.format("%.2fGB",size/(1024*1024*1024f));
        }
    }

    public static boolean wildcardMatch(String fileName,String wildcardMatcher) {
        return FilenameUtils.wildcardMatch(fileName,wildcardMatcher, IOCase.SYSTEM);
    }

    public static boolean wildcardMatch(String fileName,String wildcardMatcher,boolean caseSensitive) {
        return FilenameUtils.wildcardMatch(fileName,wildcardMatcher,caseSensitive ? IOCase.SENSITIVE:IOCase.INSENSITIVE);
    }

}
