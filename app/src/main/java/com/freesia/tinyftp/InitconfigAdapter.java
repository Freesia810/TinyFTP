package com.freesia.tinyftp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class InitconfigAdapter extends ArrayAdapter<String> {
    int selectedIndex = -1;

    public InitconfigAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
    }

    public void setSelectedIndex(int index){
        selectedIndex = index;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        String conf = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.init_config_item, parent, false);
        RadioButton rbSelect = view.findViewById(R.id.radio);
        TextView confText = view.findViewById(R.id.init_conf);
        rbSelect.setChecked(selectedIndex == position);
        confText.setText(conf);
        return view;
    }
}
