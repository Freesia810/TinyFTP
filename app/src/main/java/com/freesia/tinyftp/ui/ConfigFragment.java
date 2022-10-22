package com.freesia.tinyftp.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.freesia.tinyftp.DatabaseHelper;
import com.freesia.tinyftp.R;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

public class ConfigFragment extends Fragment {
    private SwipeRefreshLayout refreshLayout;
    private ListView configList;
    private List<String> configs;

    public ConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_config, container, false);
        refreshLayout = mView.findViewById(R.id.configLayout);
        refreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        refreshLayout.setColorSchemeColors(Color.BLUE);
        configList = mView.findViewById(R.id.configList);

        RefreshConfig task = new RefreshConfig();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        refreshLayout.setOnRefreshListener(() -> {
            RefreshConfig refreshTask = new RefreshConfig();
            refreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
        configList.setOnItemClickListener((parent, view, position, id) -> {
            String str = (String)parent.getItemAtPosition(position);
            int split_at = str.indexOf('@');
            int split_colon = str.indexOf(':');
            String username = str.substring(0,split_at);
            String host = str.substring(split_at + 1,split_colon);
            String port = str.substring(split_colon + 1);

            SQLiteDatabase db =  new DatabaseHelper(getContext()).getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT password FROM config WHERE host = ? AND port = ? AND username = ?", new String[] {host, port, username});
            int pwIndex = cursor.getColumnIndex("password");
            cursor.moveToFirst();
            String password = cursor.getString(pwIndex);
            cursor.close();
            db.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage(getResources().getString(R.string.yes_no_apply_config))
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) ->{
                        repoFragment.hostname = host;
                        repoFragment.port = Integer.parseInt(port);
                        repoFragment.username = username;
                        repoFragment.password = password;
                        Toast.makeText(getActivity(), getResources().getString(R.string.apply) + str + getResources().getString(R.string.config), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getResources().getString(R.string.no), null)
                    .setTitle(getResources().getString(R.string.main_config));
            builder.show();
        });
        configList.setOnItemLongClickListener((parent, view, position, id) ->{
            String str = (String)parent.getItemAtPosition(position);
            showConfigMenu(view, str);
            return true;
        });
        return mView;
    }
    private void showConfigMenu(View view, @NonNull String str){
        PopupMenu menu = new PopupMenu(getContext(),view);

        int split_at = str.indexOf('@');
        int split_colon = str.indexOf(':');
        String username = str.substring(0,split_at);
        String host = str.substring(split_at + 1,split_colon);
        String port = str.substring(split_colon + 1);

        SQLiteDatabase db =  new DatabaseHelper(getContext()).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT password FROM config WHERE host = ? AND port = ? AND username = ?", new String[] {host, port, username});
        int pwIndex = cursor.getColumnIndex("password");
        cursor.moveToFirst();
        String password = cursor.getString(pwIndex);
        cursor.close();
        db.close();

        menu.getMenuInflater().inflate(R.menu.config_menu,menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId())
            {
                case R.id.applyconfig:
                    repoFragment.hostname = host;
                    repoFragment.port = Integer.parseInt(port);
                    repoFragment.username = username;
                    repoFragment.password = password;
                    Toast.makeText(getActivity(), getResources().getString(R.string.apply_yy) + str + getResources().getString(R.string.config), Toast.LENGTH_SHORT).show();
                    break;
                case R.id.removeconfig:
                    SQLiteDatabase removeDB =  new DatabaseHelper(getContext()).getWritableDatabase();
                    removeDB.execSQL("DELETE FROM config WHERE host = ? AND port = ? AND username = ?", new String[] {host, port, username});
                    Toast.makeText(getActivity(), getResources().getString(R.string.delete_config_ss), Toast.LENGTH_SHORT).show();
                    RefreshConfig task = new RefreshConfig();
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    removeDB.close();
                    break;
                case R.id.updateconfig:
                    SQLiteDatabase updateDB =  new DatabaseHelper(getContext()).getWritableDatabase();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    View diaView = LayoutInflater.from(getContext()).inflate(R.layout.update_config, null);
                    final EditText hostText = diaView.findViewById(R.id.updateHost);
                    final EditText portText = diaView.findViewById(R.id.updatePort);
                    final EditText userText = diaView.findViewById(R.id.updateUser);
                    final EditText passwordText = diaView.findViewById(R.id.updatePassword);
                    portText.setInputType(InputType.TYPE_CLASS_NUMBER);

                    builder.setIcon(R.drawable.ic_rename).setView(diaView)
                            .setNegativeButton(getResources().getString(R.string.cancel), null).setTitle(getResources().getString(R.string.update_config));
                    hostText.setText(host);
                    portText.setText(port);
                    userText.setText(username);
                    passwordText.setText(password);
                    builder.setPositiveButton(getResources().getString(R.string.apply), (dialog, which) -> {
                        String newHost = hostText.getText().toString();
                        String newPort = portText.getText().toString();
                        String newUser = userText.getText().toString();
                        String newPassword = passwordText.getText().toString();
                        String md5 = DigestUtils.md5Hex(newHost + ";" + newPort + ";" + newUser + ";" + newPassword);
                        updateDB.execSQL("DELETE FROM config WHERE host = ? AND port = ? AND username = ?", new String[] {host, port, username});

                        ContentValues values = new ContentValues();
                        values.put("host", newHost);
                        values.put("port", newPort);
                        values.put("username", newUser);
                        values.put("password", newPassword);
                        values.put("md5", md5);

                        long code = updateDB.insert("config", null,values);
                        if(code == -1){
                            builder.setIcon(android.R.drawable.ic_dialog_alert)
                                    .setMessage(getResources().getString(R.string.update_conf_fail))
                                    .setPositiveButton(getResources().getString(R.string.apply), null)
                                    .setTitle(getResources().getString(R.string.error));
                        }
                        else{
                            Toast.makeText(getActivity(), getResources().getString(R.string.conf_saved), Toast.LENGTH_SHORT).show();
                            RefreshConfig rTask = new RefreshConfig();
                            rTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                        updateDB.close();
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


    private class RefreshConfig extends AsyncTask<Void, Void, Boolean> {
        @NonNull
        @Override
        protected Boolean doInBackground(Void... voids) {
            SQLiteDatabase db =  new DatabaseHelper(getContext()).getReadableDatabase();

            Cursor cursor = db.query("config", null,null,null,null,null,null);
            int host = cursor.getColumnIndex("host");
            int port = cursor.getColumnIndex("port");
            int username = cursor.getColumnIndex("username");
            configs = new ArrayList<>();
            for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
                String config = cursor.getString(username) + "@" + cursor.getString(host) + ":" + cursor.getString(port);
                configs.add(config);
            }
            cursor.close();
            db.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean res){
            configList.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, configs));
            refreshLayout.setRefreshing(false);
        }
    }
}