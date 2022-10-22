package com.freesia.tinyftp.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.freesia.tinyftp.DatabaseHelper;
import com.freesia.tinyftp.R;

import org.apache.commons.codec.digest.DigestUtils;


public class settingFragment extends Fragment {
    private EditText hostEdit;
    private EditText portEdit;
    private EditText userEdit;
    private EditText pwEdit;

    public settingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_setting, container, false);

        ImageView rstBtn = mView.findViewById(R.id.resetBtn);
        ImageView aplBtn = mView.findViewById(R.id.applyBtn);
        TextView addText = mView.findViewById(R.id.add_server);
        hostEdit = mView.findViewById(R.id.hostEdit);
        portEdit = mView.findViewById(R.id.portEdit);
        userEdit = mView.findViewById(R.id.userEdit);
        pwEdit = mView.findViewById(R.id.pwEdit);

        int uiMode = getContext().getResources().getConfiguration().uiMode;
        if ((uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            addText.setTextColor(Color.WHITE);
        } else {
            addText.setTextColor(Color.BLACK);
        }

        rstBtn.setOnClickListener(v -> {
            hostEdit.setText("");
            portEdit.setText("");
            userEdit.setText("");
            pwEdit.setText("");
        });

        aplBtn.setOnClickListener(v -> {
            SQLiteDatabase db =  new DatabaseHelper(getContext()).getWritableDatabase();
            String host = hostEdit.getText().toString();
            String port = portEdit.getText().toString();
            String username = userEdit.getText().toString();
            String password = pwEdit.getText().toString();
            String md5 = DigestUtils.md5Hex(host + ";" + port + ";" + username + ";" + password);

            Cursor cursor = db.rawQuery("SELECT * FROM config WHERE md5 = ?", new String[] {md5});

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            if(cursor.getCount() == 0){
                ContentValues values = new ContentValues();
                values.put("host", host);
                values.put("port", port);
                values.put("username", username);
                values.put("password", password);
                values.put("md5", md5);

                long code = db.insert("config", null,values);

                if(code == -1) {
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage(getResources().getString(R.string.add_conf_fail))
                            .setPositiveButton(getResources().getString(R.string.apply), null)
                            .setTitle(getResources().getString(R.string.error));
                }
                else{
                    builder.setIcon(android.R.drawable.ic_dialog_info)
                            .setMessage(getResources().getString(R.string.yes_no_apply_config))
                            .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) ->{
                                repoFragment.hostname = host;
                                repoFragment.port = Integer.parseInt(port);
                                repoFragment.username = username;
                                repoFragment.password = password;
                            })
                            .setNegativeButton(getResources().getString(R.string.no), null)
                            .setTitle(getResources().getString(R.string.main_config));
                }
            }
            else{
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(getResources().getString(R.string.add_conf_fail_same))
                        .setPositiveButton(getResources().getString(R.string.apply), null)
                        .setTitle(getResources().getString(R.string.error));
            }
            builder.show();
            cursor.close();
            db.close();
        });


        return mView;
    }
}