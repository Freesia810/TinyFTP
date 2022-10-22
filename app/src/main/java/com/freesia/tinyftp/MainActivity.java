package com.freesia.tinyftp;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ListView;
import android.widget.Toast;

import com.freesia.tinyftp.ui.repoFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.freesia.tinyftp.databinding.ActivityMainBinding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        com.freesia.tinyftp.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.repoFragment, R.id.configFragment, R.id.settingFragment)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if(repoFragment.port == -1) {
            SQLiteDatabase db =  new DatabaseHelper(this).getReadableDatabase();

            Cursor cursor = db.query("config", null,null,null,null,null,null);
            if(cursor.getCount() == 0) {
                Toast.makeText(this, getResources().getString(R.string.please_add_conf), Toast.LENGTH_SHORT).show();
                navController.popBackStack(R.id.repoFragment, true);
                navController.navigate(R.id.settingFragment);
            }
            else{
                int host = cursor.getColumnIndex("host");
                int port = cursor.getColumnIndex("port");
                int username = cursor.getColumnIndex("username");
                List<String> initConfigs = new ArrayList<>();
                for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
                    String config = cursor.getString(username) + "@" + cursor.getString(host) + ":" + cursor.getString(port);
                    initConfigs.add(config);
                }
                cursor.close();
                db.close();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View view = LayoutInflater.from(this).inflate(R.layout.init_config, null);
                final ListView initList = view.findViewById(R.id.initList);
                InitconfigAdapter adapter = new InitconfigAdapter(this, R.layout.init_config_item, initConfigs);
                final String[] selectedConf = {""};
                initList.setOnItemClickListener((parent, listview, position, id) ->{
                    adapter.setSelectedIndex(position);
                    adapter.notifyDataSetChanged();
                    selectedConf[0] = (String)parent.getItemAtPosition(position);
                });
                initList.setAdapter(adapter);

                builder.setIcon(R.drawable.ic_rename).setView(view)
                        .setNegativeButton(getResources().getString(R.string.cancel), null).setTitle(getResources().getString(R.string.choose_conf));
                builder.setPositiveButton(getResources().getString(R.string.apply), (dialog, which) -> {
                    if(Objects.equals(selectedConf[0], "")){
                        Toast.makeText(this, getResources().getString(R.string.no_config), Toast.LENGTH_SHORT).show();
                    }
                    else{
                        int split_at = selectedConf[0].indexOf('@');
                        int split_colon = selectedConf[0].indexOf(':');
                        String sel_username = selectedConf[0].substring(0,split_at);
                        String sel_host = selectedConf[0].substring(split_at + 1,split_colon);
                        String sel_port = selectedConf[0].substring(split_colon + 1);

                        SQLiteDatabase pwdb =  new DatabaseHelper(this).getReadableDatabase();
                        Cursor pwcursor = pwdb.rawQuery("SELECT password FROM config WHERE host = ? AND port = ? AND username = ?", new String[] {sel_host, sel_port, sel_username});
                        int pwIndex = pwcursor.getColumnIndex("password");
                        pwcursor.moveToFirst();
                        String password = pwcursor.getString(pwIndex);
                        pwcursor.close();
                        pwdb.close();

                        repoFragment.hostname = sel_host;
                        repoFragment.port = Integer.parseInt(sel_port);
                        repoFragment.username = sel_username;
                        repoFragment.password = password;
                        Toast.makeText(this, getResources().getString(R.string.apply_yy) + selectedConf[0] + getResources().getString(R.string.config), Toast.LENGTH_SHORT).show();


                        ((repoFragment)(this.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main).getChildFragmentManager().getFragments().get(0))).RefreshList();
                    }
                });

                builder.show();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton(getResources().getString(R.string.apply), null)
                    .setTitle(getResources().getString(R.string.about)).setMessage(getResources().getString(R.string.about_info));
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}