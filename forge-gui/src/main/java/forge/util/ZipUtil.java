package forge.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
*  https://www.baeldung.com/java-compress-and-uncompress
*/
public class ZipUtil {
    public static String backupAdvFile = "forge.adv";
    public static String backupClsFile = "forge.cls";
    private static boolean isClassic = false;
    public static void zip(File source, File dest, String name) throws IOException {
        isClassic = backupClsFile.equalsIgnoreCase(name);
        try(
        FileOutputStream fos = new FileOutputStream(dest.getAbsolutePath() + File.separator + name);
        ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            zipFile(source, source.getName(), zipOut);
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        //skip loose files like forge.log, etc
        if (fileToZip.isFile() && isClassic && "Forge".equalsIgnoreCase(fileToZip.getParentFile().getName()))
            return;
        if (fileToZip.isDirectory()) {
            //skip adventure since we don't want to overwrite it and adventure has its own backup method
            if (isClassic && "adventure".equalsIgnoreCase(fileToZip.getName()) && "Forge".equalsIgnoreCase(fileToZip.getParentFile().getName()))
                return;
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static String unzip(File fileZip, File destDir) throws IOException {
        isClassic = backupClsFile.equalsIgnoreCase(fileZip.getName());
        StringBuilder val = new StringBuilder();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(fileZip.toPath()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
                if (isClassic && "Forge".equalsIgnoreCase(newFile.getParentFile().getName()))
                    val.append(" * "). append(newFile.getName()).append("\n");
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                if (!isClassic)
                    val.append(" * "). append(newFile.getName()).append("\n");
                // write file content
                try(FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
        return val.toString();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
