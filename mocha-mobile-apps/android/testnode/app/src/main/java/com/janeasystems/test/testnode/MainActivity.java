package nodejsmobile.test.testnode;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.text.TextUtils;
import java.io.*;
import java.lang.System;
import java.util.ArrayList;
import java.nio.ByteBuffer; 
import java.nio.channels.*;

public class MainActivity extends Activity {

    private static AssetManager assetManager = null;
    private static String TAG = "MochaMobile";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the args for node
        String nodeArgs = getIntent().getStringExtra("nodeargs");

        if (nodeArgs == null) {
            // Note: use Log.v to keep the app logging diversified from
            // the node logging that uses Log.i and Log.e
            Log.v(TAG, "No input args, copying assets...");
            this.assetManager = this.getAssets();
            try {
                copyTestAssets();
            } catch (IOException e) {
                e.printStackTrace();
                Log.v(TAG, "COPYASSETS:FAIL");
                return;
            }
            Log.v(TAG, "COPYASSETS:PASS");
            return;
        }
        String nodeSubstituteDir=getIntent().getStringExtra("substitutedir");

        if (nodeArgs.startsWith("-p")) {
            RunNode("node " + nodeArgs);
        } else {
            final String testFolderPath = this.getBaseContext().getFilesDir().getAbsolutePath() + "/test/";
            // final String mainjsPath = testFolderPath + "main-test.js";
            final String mainjsPath = testFolderPath + "node_modules/mocha-mobile/mocha-mobile-run.js";
            //final String runMochaPath = testFolderPath + "mochaRun.js";
            // final String runMochaPath = testFolderPath + "node_modules/mocha/bin/_mocha";
            String[] parts = nodeArgs.split(" ");
            ArrayList<String> newArgs = new ArrayList<String>();
            ArrayList<String> mochaPrepArgs = new ArrayList<String>();
            // Node input flags go before main-test.js
            for (int i = 0; i < ( parts.length ); i++) {
                String arg = parts[i];
                String flag = arg.split("=")[0];

                // parameters preparation from node_modules/mocha/bin/mocha.js
                switch (flag) {
                    case "-d":
                        mochaPrepArgs.add("--debug");
                        newArgs.add("--no-timeouts");
                        break;
                    case "debug":
                    case "--debug":
                    case "--debug-brk":
                    case "--inspect":
                    case "--inspect-brk":
                        mochaPrepArgs.add(arg);
                        newArgs.add("--no-timeouts");
                        break;
                    case "-gc":
                    case "--expose-gc":
                        mochaPrepArgs.add("--expose-gc");
                        break;
                    case "--gc-global":
                    case "--es_staging":
                    case "--no-deprecation":
                    case "--no-warnings":
                    case "--prof":
                    case "--log-timer-events":
                    case "--throw-deprecation":
                    case "--trace-deprecation":
                    case "--trace-warnings":
                    case "--use_strict":
                    case "--allow-natives-syntax":
                    case "--perf-basic-prof":
                    case "--napi-modules":
                        mochaPrepArgs.add(arg);
                        break;
                    default:
                        if (arg.indexOf("--harmony") == 0 
                            || arg.indexOf("--trace") == 0
                            || arg.indexOf("--icu-data-dir") == 0
                            || arg.indexOf("--max-old-space-size") == 0
                            || arg.indexOf("--preserve-symlinks") == 0) {
                            mochaPrepArgs.add(arg);
                        } else {
                            // general mocha parameters
                            if (nodeSubstituteDir == null) {
                                newArgs.add(parts[i]);
                            } else {
                                //if there is a dir to substitute in the node arguments, do it.
                                newArgs.add(parts[i].replace(nodeSubstituteDir, testFolderPath));
                            }
                        }
                    break;
                }

                
            }
            // Last arg is the test filename
            String newArgsStr = String.format("node%s %s%s",
                mochaPrepArgs.isEmpty() ? "" : " " + TextUtils.join(" ", mochaPrepArgs),
                mainjsPath,
                newArgs.isEmpty() ? "" : " " + TextUtils.join(" ", newArgs));
            RunNode(newArgsStr);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native Integer startNodeWithArguments(String[] arguments, String nodePath, boolean redirectOutputToLogcat);

    private void RunNode(String args) {
        Log.v(TAG, "Args: " + args);

        final String testFolderPath = this.getBaseContext().getFilesDir().getAbsolutePath();
        final String[] parts = args.split(" ");

        Thread mainNodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startNodeWithArguments(
                    parts,
                    testFolderPath,
                    true);
            }
        });
        mainNodeThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            //If the node thread throws some exception, let's say the test fails.
            public void uncaughtException(Thread t, Throwable e) {
                Log.i(TAG, "RESULT:FAIL");
            }
        });
        mainNodeThread.start();
    }

    private void copyTestAssets() throws IOException {
        String destFolder = this.getBaseContext().getFilesDir().getAbsolutePath() + "/test";
        File folderObject = new File(destFolder);
        if (folderObject.exists()) {
            deleteFolderRecursively(folderObject);
        }
        enumerateAssetFolder("", destFolder);
    }

    private void enumerateAssetFolder(String srcFolder, String destPath) throws IOException {
        String[] files = assetManager.list(srcFolder);

        if (files.length == 0) {
            copyAssetFile(srcFolder, destPath);
        } else {
            new File(destPath).mkdirs();
            for (String file : files) {
                if (srcFolder.equals("")) {
                    enumerateAssetFolder(file, destPath + "/" + file);
                } else {
                    enumerateAssetFolder(srcFolder + "/" + file, destPath + "/" + file);
                }
            }
        }
    }

    private void copyAssetFile(String srcFolder, String destPath) throws IOException {
        InputStream in = assetManager.open(srcFolder);
        new File(destPath).createNewFile();
        OutputStream out = new FileOutputStream(destPath);
        copyFile(in, out);
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16*1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    // private void copyAssetFile(String srcFolder, String destPath)  throws IOException {
    //     AssetFileDescriptor afd = assetManager.openFd(srcFolder);
    //     FileChannel inChannel = new FileInputStream(afd.getFileDescriptor()).getChannel();
    //     new File(destPath).createNewFile();
    //     FileChannel outChannel = new FileOutputStream(destPath).getChannel();
    //     try {
    //         //inChannel.transferTo(0, inChannel.size(), outChannel);
    //         inChannel.transferTo(afd.getStartOffset(), afd.getLength(), outChannel);
    //     } finally {
    //         if (inChannel != null)
    //             inChannel.close();
    //         if (outChannel != null)
    //             outChannel.close();
    //     }
    // }

    private void deleteFolderRecursively(File file) throws IOException {
        for (File childFile : file.listFiles()) {
            if (childFile.isDirectory()) {
                deleteFolderRecursively(childFile);
            } else {
                childFile.delete();
            }
        }
        file.delete();
    }
}
