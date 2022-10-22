package com.freesia.tinyftp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Environment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.freesia.tinyftp.FileInfo;
import com.freesia.tinyftp.FileInfoAdapter;
import com.freesia.tinyftp.R;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class repoFragment extends Fragment {
    private SwipeRefreshLayout refreshLayout;
    private ListView fileList;
    private List<FileInfo> fileInfoList;
    private ActivityResultLauncher<Intent> openFile;
    private ProgressBar progressBar;
    private ImageButton backBtn;
    private File uploadFile;
    private static String curPath = "/";

    private class TaskInfo{
        int mode;
        int resCode;
        String dlName;
    }
    private static final int GETLIST = 0;
    private static final int UPLOADFILE = 1;
    private static final int DOWNLOADFILE = 2;
    private static final int DELETEFILE = 3;
    private static final int RENAME = 4;
    private static final int DOWNLOADOPENFILE = 5;

    public static String hostname = "";
    public static int port = -1;
    public static String username = "";
    public static String password = "";

    public repoFragment() {
        // Required empty public constructor
    }

    public void RefreshList(){
        FTPTask task = new FTPTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode()== Activity.RESULT_OK)
            {
                Uri videoUri = result.getData().getData();
                uploadFile = FileInfo.Uri2File(videoUri,getActivity());
                if(uploadFile != null)
                {
                    FTPTask uploadTask = new FTPTask();
                    uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,UPLOADFILE,curPath,uploadFile);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_repo, container, false);

        refreshLayout = mView.findViewById(R.id.srLayout);
        refreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        refreshLayout.setColorSchemeColors(Color.BLUE);
        fileList = mView.findViewById(R.id.fileList);
        progressBar = mView.findViewById(R.id.progressBar);
        backBtn = mView.findViewById(R.id.backBtn);
        ImageButton imageButton = mView.findViewById(R.id.uploadBtn);


        backBtn.setVisibility(View.GONE);

        FTPTask firstTask = new FTPTask();
        firstTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);

        refreshLayout.setOnRefreshListener(() -> {
            FTPTask task = new FTPTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
        });

        fileList.setOnItemClickListener((parent, view, position, id) -> {
            FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);
            if(fileInfo.getIsDir())
            {
                //打开文件夹
                curPath += fileInfo.getFileName() + "/";

                FTPTask task = new FTPTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
            }
            else{
                //下载并打开文件
                FTPTask downloadTask = new FTPTask();
                downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DOWNLOADOPENFILE, curPath, fileInfo.getFileName());

            }
        });

        fileList.setOnItemLongClickListener((parent, view, position, id) -> {
            FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);
            if(fileInfo.getIsDir())
            {
                showDirMenu(view,fileInfo);
            }
            else{
                //打开文件菜单
                showFileMenu(view, fileInfo);
            }

            return true; //保证不会进行click事件
        });

        backBtn.setOnClickListener(view -> {
            curPath = curPath.substring(0, curPath.length()-1);
            int pos = curPath.lastIndexOf('/');
            curPath = curPath.substring(0, pos + 1);

            FTPTask task = new FTPTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
        });

        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            openFile.launch(intent);
        });
        return mView;
    }

    private void showFileMenu(View view, FileInfo fileInfo) {
        PopupMenu menu = new PopupMenu(getContext(),view);
        menu.getMenuInflater().inflate(R.menu.file_menu,menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId())
            {
                case R.id.download:
                    FTPTask downloadTask = new FTPTask();
                    downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DOWNLOADFILE, curPath, fileInfo.getFileName());
                    break;
                case R.id.remove:
                    FTPTask deleteTask = new FTPTask();
                    deleteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DELETEFILE, curPath, fileInfo.getFileName());
                    break;
                case R.id.renameFile:
                    final EditText editText = new EditText(getContext());
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setIcon(R.drawable.ic_rename).setView(editText)
                            .setNegativeButton(getResources().getString(R.string.cancel), null).setTitle(getResources().getString(R.string.rename));
                    editText.setText(fileInfo.getFileName());
                    builder.setPositiveButton(getResources().getString(R.string.apply), (dialog, which) -> {
                        String newName = editText.getText().toString();
                        FTPTask renameTask = new FTPTask();
                        renameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, RENAME, curPath, fileInfo.getFileName(), newName);
                    });
                    builder.show();
                    break;
                default:
                    break;
            }
            return false;
        });

        menu.show();
    }

    private void showDirMenu(View view, FileInfo fileInfo) {
        PopupMenu menu = new PopupMenu(getContext(),view);
        menu.getMenuInflater().inflate(R.menu.dir_menu,menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId())
            {
                case R.id.open:
                    //打开文件夹
                    curPath += fileInfo.getFileName() + "/";

                    FTPTask task = new FTPTask();
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST,curPath);
                    break;
                case R.id.renameDir:
                    final EditText editText = new EditText(getContext());
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setIcon(R.drawable.ic_rename).setView(editText)
                            .setNegativeButton(getResources().getString(R.string.cancel), null).setTitle(getResources().getString(R.string.rename));
                    editText.setText(fileInfo.getFileName());
                    builder.setPositiveButton(getResources().getString(R.string.apply), (dialog, which) -> {
                        String newName = editText.getText().toString();
                        FTPTask renameTask = new FTPTask();
                        renameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, RENAME, curPath, fileInfo.getFileName(), newName);
                    });
                    builder.show();
                    break;
                default:
                    break;
            }
            return false;
        });

        menu.show();
    }

    private class FTPTask extends AsyncTask<Object, Integer, TaskInfo> {
        @Override
        protected void onPreExecute() {
            progressBar.setProgress(0);
        }

        @Override
        protected TaskInfo doInBackground(Object... objects){
            int mode = (int)objects[0];
            int resCode = 0;
            String dlName = "";

            FTPClient ftpClient = new FTPClient();
            ftpClient.setControlEncoding("UTF-8");

            try {
                ftpClient.connect(hostname,port);
                ftpClient.login(username,password);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.setRemoteVerificationEnabled(false);
                ftpClient.changeWorkingDirectory((String)objects[1]);

                switch (mode) {
                    case GETLIST:
                        FTPFile[] ftpFiles = ftpClient.listFiles();
                        fileInfoList = new ArrayList<>();
                        for(FTPFile ftpFile:ftpFiles) {
                            fileInfoList.add(new FileInfo(ftpFile.getName(),ftpFile.getSize(),ftpFile.getTimestamp(),ftpFile.isDirectory()));
                        }
                        publishProgress(0);
                        break;
                    case UPLOADFILE:
                        File uploadFile = (File)objects[2];
                        try (FileInputStream fis = new FileInputStream(uploadFile); OutputStream os = ftpClient.storeFileStream(uploadFile.getName())) {
                            byte[] bytes = new byte[1024];
                            long total = uploadFile.length();
                            int count = 0;
                            int i;
                            while ((i = fis.read(bytes)) != -1) {
                                os.write(bytes, 0, i);
                                count += i;
                                publishProgress((int) ((count / (float) total) * 100));
                            }

                            resCode = 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                            resCode = -2;
                        } finally {
                            ftpClient.completePendingCommand();
                        }
                        break;
                    case DOWNLOADFILE: case DOWNLOADOPENFILE:
                        String downloadFileName = (String)objects[2];
                        File appFolder = new File(Environment.getExternalStorageDirectory() + "/TinyFTP");
                        if (!appFolder.exists()){
                            appFolder.mkdir();
                        }
                        File file = new File(Environment.getExternalStorageDirectory()+ "/TinyFTP/" + downloadFileName);
                        dlName = file.getName();
                        if (!file.exists()){
                            file.createNewFile();
                        }
                        long total = ftpClient.mlistFile(downloadFileName).getSize();
                        try (FileOutputStream fos = new FileOutputStream(file); InputStream inputStream = ftpClient.retrieveFileStream(downloadFileName)) {
                            byte[] bytes = new byte[1024];
                            int count = 0;
                            int i;
                            while ((i = inputStream.read(bytes)) != -1) {
                                fos.write(bytes, 0, i);
                                count += i;
                                publishProgress((int) ((count / (float) total) * 100));
                            }

                            resCode = 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                            resCode = -2;
                        } finally {
                            ftpClient.completePendingCommand();
                        }
                        break;
                    case DELETEFILE:
                        if(!ftpClient.deleteFile((String) objects[2])) {
                            resCode = -1;
                        }
                        publishProgress(0);
                        break;
                    case RENAME:
                        if(!ftpClient.rename((String) objects[2], (String) objects[3])) {
                            resCode = -1;
                        }
                        publishProgress(0);
                        break;
                    default:
                        break;
                }

                ftpClient.disconnect();
            } catch (Exception e) {
                //操作失败
                e.printStackTrace();
                resCode = -1;
            }
            TaskInfo t = new TaskInfo();
            t.mode = (Integer) objects[0];
            t.resCode = resCode;
            t.dlName = dlName;
            return t;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(TaskInfo taskInfo) {
            switch (taskInfo.resCode){
                case 0:
                    FTPTask task = new FTPTask();
                    switch (taskInfo.mode) {
                        case GETLIST:
                            FileInfoAdapter adapter = new FileInfoAdapter(getActivity(), R.layout.file_item, fileInfoList);
                            fileList.setAdapter(adapter);
                            refreshLayout.setRefreshing(false);

                            if(curPath.length() == 1) {
                                backBtn.setVisibility(View.GONE);
                            }
                            else{
                                backBtn.setVisibility(View.VISIBLE);
                            }
                            break;
                        case UPLOADFILE:
                            Toast.makeText(getActivity(), getResources().getString(R.string.upload_ok), Toast.LENGTH_SHORT).show();
                            progressBar.setProgress(0);
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
                            break;
                        case DOWNLOADFILE:
                            Toast.makeText(getActivity(), getResources().getString(R.string.download_ok), Toast.LENGTH_SHORT).show();
                            progressBar.setProgress(0);
                            break;
                        case DELETEFILE: case RENAME:
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GETLIST, curPath);
                            break;
                        case DOWNLOADOPENFILE:
                            progressBar.setProgress(0);
                            File file = new File(Environment.getExternalStorageDirectory() + "/TinyFTP/" + taskInfo.dlName);
                            Intent intent = new Intent("android.intent.action.VIEW");
                            intent.setDataAndType(FileProvider.getUriForFile(getContext(),getContext().getApplicationContext().getPackageName() + ".provider", file), "*/*");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            getContext().startActivity(intent);
                        default:
                            break;
                    }
                    break;
                case -1:
                    if(port != -1){
                        if(taskInfo.mode == GETLIST){
                            Toast.makeText(getActivity(), getResources().getString(R.string.refresh_fail), Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getActivity(), getResources().getString(R.string.do_fail), Toast.LENGTH_SHORT).show();
                        }
                    }
                    refreshLayout.setRefreshing(false);
                    break;
                case -2:
                    Toast.makeText(getActivity(), getResources().getString(R.string.io_err), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}