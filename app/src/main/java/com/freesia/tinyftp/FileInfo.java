package com.freesia.tinyftp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.FileUtils;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileInfo {
    private final String fileName;
    private final String fileSize;
    private final String fileTime;
    private final Boolean isDir;

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileTime() {
        return fileTime;
    }

    public Boolean getIsDir() {
        return isDir;
    }

    public FileInfo(String fileName, long size, @NonNull Calendar time, Boolean isDir)
    {
        this.fileName = fileName;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fileTime = simpleDateFormat.format(time.getTime());
        fileSize = Long2Str(size);
        this.isDir = isDir;
    }

    public static File Uri2File(Uri uri, Context context)
    {
        File file = null;
        if(uri == null)
        {
            return null;
        }
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE))
        {
            file = new File(uri.getPath());
        }
        else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
        {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            String displayName="";
            if (cursor.moveToFirst())
            {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                displayName = cursor.getString(index);
            }
            cursor.close();
            try
            {
                InputStream is = contentResolver.openInputStream(uri);
                File cache = new File(context.getCacheDir().getAbsolutePath(), displayName);
                FileOutputStream fos = new FileOutputStream(cache);
                FileUtils.copy(is, fos);
                file = cache;
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @NonNull
    private static String Long2Str(long size)
    {
        if(size <=0 || size >= 1024 * 1024 * 1024)
        {
            return "0B";
        }

        double dSize;

        try {
            dSize = size;
        } catch (Exception e) {
            e.printStackTrace();
            return "0B";
        }

        double divideBasic = 1024;
        if (size < 1024) {
            //1kb以内
            if (size < 1000) {
                return size + "B";
            } else {
                //大于1000B,转化为kb
                return String.format("%.2f", dSize / divideBasic) + "K";
            }
        } else if (size < 1024 * 1024) {
            //1M以内
            if (size < 1024 * 1000) {
                return String.format("%.2f", dSize / divideBasic) + "K";
            } else {
                //大于1000Kb,转化为M
                return String.format("%.2f", dSize / divideBasic / divideBasic) + "M";
            }
        } else {
            //1TB以内
            if (size < 1024 * 1024 * 1000) {
                return String.format("%.2f", dSize / divideBasic / divideBasic) + "M";
            } else {
                //大于1000Mb,转化为G
                return String.format("%.2f", dSize / divideBasic / divideBasic / divideBasic) + "G";
            }
        }
    }
}

