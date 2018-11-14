package testdomain.test.mochamobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.util.Log;

public class Decompress {
    private static final int BUFFER_SIZE = 1024 * 10;
    private static final String TAG = "Decompress";

    public static void unzipFromAssets(Context context, String zipFile, String destination)
    throws Exception
    {
        if (destination == null || destination.length() == 0)
            destination = context.getFilesDir().getAbsolutePath();
        InputStream stream = context.getAssets().open(zipFile);
        unzip(stream, destination);
    }

    public static void unzip(String zipFile, String location) 
    throws Exception
    {
        FileInputStream fin = new FileInputStream(zipFile);
        unzip(fin, location);
    }

    public static void unzip(InputStream stream, String destination) 
    throws Exception
    {
        dirChecker(destination, "");
        byte[] buffer = new byte[BUFFER_SIZE];
        
        ZipInputStream zin = new ZipInputStream(stream);
        ZipEntry ze = null;

        while ((ze = zin.getNextEntry()) != null) {
            Log.v(TAG, "Unzipping " + ze.getName());

            // if (ze.isDirectory() || ze.getSize() > 0) {
            if (ze.isDirectory()) {
                dirChecker(destination, ze.getName());
            } else {
                File f = new File(destination, ze.getName());
                if (!f.exists()) {
                    boolean success = f.createNewFile();
                    if (!success) {
                        throw new Exception("Failed to create file " + f.getName());
                    }
                    FileOutputStream fout = new FileOutputStream(f);
                    int count;
                    while ((count = zin.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                    fout.close();
                }
            }
            zin.closeEntry();
        }
        zin.close();
    }

    private static void dirChecker(String destination, String dir) 
    throws Exception
    {
        File f = new File(destination, dir);

        if (!f.isDirectory()) {
            boolean success = f.mkdirs();
            if (!success) {
                throw new Exception("Failed to create folder " + f.getName());
            }
        }
    }

    public static void installZipFromAssets(Context context, String sourceFilename,
          String destinationDir) throws IOException,
          FileNotFoundException {
        File destinationFile;

        // Attempt to open the zip archive
        ZipInputStream inputStream = new ZipInputStream(context.getAssets().open(sourceFilename));

        // Loop through all the files and folders in the zip archive (but there should just be one)
        for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream
            .getNextEntry()) {
          Log.v(TAG, "Unzipping " + entry.getName());

          destinationFile = new File(destinationDir, entry.getName());

          if (entry.isDirectory()) {
            destinationFile.mkdirs();
          } else {
            // Note getSize() returns -1 when the zipfile does not have the size set
            long zippedFileSize = entry.getSize();

            // Create a file output stream
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            final int BUFFER = 8192;

            // Buffer the output to the file
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER);
            int unzippedSize = 0;

            // Write the contents
            int count = 0;
            Integer percentComplete = 0;
            Integer percentCompleteLast = 0;
            byte[] data = new byte[BUFFER];
            while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
              bufferedOutputStream.write(data, 0, count);
              unzippedSize += count;
              percentComplete = (int) ((unzippedSize / (long) zippedFileSize) * 100);
              if (percentComplete > percentCompleteLast) {
                //publishProgress("Uncompressing data for " + languageName + "...", 
                //    percentComplete.toString(), "0");
                percentCompleteLast = percentComplete;
              }
            }
            bufferedOutputStream.close();
          }
          inputStream.closeEntry();
        }
        inputStream.close();
    }
}