package cn.edu.seu.evmhr.Utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Cookie_D on 2016/9/26.
 */

public class CacheUtil {
    /**
     *
     * @description save object(获取保存的实体对象)
     */
    public static boolean saveObject(Context context,Object ser, String file) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(file, context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(ser);
            oos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                oos.close();
            } catch (Exception e) {
            }
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * @description read object(读取保存的对象)
     */
    public static Object readObject(Context context,String file) {
        if (!isExistDataCache(context,file)) {
            return null;
        }
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis =context.openFileInput(file);
            ois = new ObjectInputStream(fis);
            return ois.readObject();
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
                fis.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     *
     * @description judge the cache file exit or not(判读缓存是否存在)
     */
    private static boolean isExistDataCache(Context context,String cachefile) {
        boolean exist = false;
        File data = context.getFileStreamPath(cachefile);
        if (data.exists())
            exist = true;
        return exist;
    }



    public void deleteFileWithName(Context context, String fileName) {
        File file = new File(context.getFilesDir() + File.separator + fileName);
        if (file.isFile()) {
            file.delete();
        }
    }

    private static int clearCacheFolder(File dir, long curTime) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, curTime);
                    }
                    if (child.lastModified() < curTime) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deletedFiles;
    }

    public static  Long getFileSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    if (child.isDirectory()) {
                        size += getFileSize(child);
                    }else{
                        size += child.length();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return size;
    }
    public static String getSDCacheDir(){
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            File path = Environment.getExternalStorageDirectory();
            return path.getAbsolutePath();
        }else{
            return "";
        }
    }
    public static String getSDCacheDir(String dirName){
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String path = Environment.getExternalStorageDirectory()+ File.separator+dirName;
            File dir = new File(path);
            if(!dir.exists()){
                dir.mkdir();
            }
            return dir.getAbsolutePath();
        }else{
            return "";
        }
    }
}
