package com.example.appiot.ui.slideshow;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.appiot.R;
import com.example.appiot.databinding.FragmentSlideshowBinding;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

public class SlideshowFragment extends Fragment {

    private SlideshowViewModel slideshowViewModel;
    private FragmentSlideshowBinding binding;
    int ACState, h1, m1, h2, m2;
    String t1, t2, prevTemp;
    boolean onOff3;
    SharedPreferences sp;
    MqttAndroidClient client;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        slideshowViewModel =
                new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        String topic = "proyectoIOT1";
        client = new MqttAndroidClient(getActivity(), "tcp://broker.hivemq.com:1883", MqttClient.generateClientId());

        final TextView timeStart = binding.timeStart3;
        final TextView timeEnd = binding.timeEnd3;
        final TextView tempSet = binding.tempSet;
        final Button setTime = binding.setTime3;
        final ToggleButton toggleButton = binding.toggleButton3;

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getActivity().getBaseContext(), "NO Conectado ", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        sp = getActivity().getSharedPreferences("NodeStates", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        ACState = sp.getInt("ACState", -300);

        if(ACState != 0){
            editor.putBoolean("onOff3", false);
            editor.commit();
        }

        onOff3 = sp.getBoolean("onOff3", false);
        prevTemp = sp.getString("prevTemp", "");
        toggleButton.setChecked(onOff3);


        switch (ACState){
            case -300:
                timeStart.setVisibility(View.INVISIBLE);
                timeEnd.setVisibility(View.INVISIBLE);
                setTime.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                tempSet.setVisibility(View.INVISIBLE);
                break;
            case 0:
                timeStart.setVisibility(View.INVISIBLE);
                timeEnd.setVisibility(View.INVISIBLE);
                setTime.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.VISIBLE);
                tempSet.setText(prevTemp);
                tempSet.setVisibility(View.VISIBLE);
                break;
            case 1:
                timeStart.setVisibility(View.VISIBLE);
                timeEnd.setVisibility(View.VISIBLE);
                setTime.setVisibility(View.VISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                tempSet.setVisibility(View.VISIBLE);
                break;
        }

        timeStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog tp = new TimePickerDialog(
                        getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        h1 = hourOfDay;
                        m1 = minute;
                        Calendar c = Calendar.getInstance();
                        c.set(0,0,0,h1, m1);
                        timeStart.setText(DateFormat.format("hh:mm", c));
                        t1 = (String) DateFormat.format("HH:mm", c);
                    }
                }, 12, 0, false
                );
                tp.updateTime(h1, m1);
                tp.show();
            }
        });

        timeEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog tp = new TimePickerDialog(
                        getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        h2 = hourOfDay;
                        m2 = minute;
                        Calendar c = Calendar.getInstance();
                        c.set(0,0,0,h2, m2);
                        timeEnd.setText(DateFormat.format("hh:mm", c));
                        t2 = (String) DateFormat.format("HH:mm", c);
                    }
                }, 12, 0, false
                );
                tp.updateTime(h2, m2);
                tp.show();
            }
        });

        setTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp = tempSet.getText().toString();
                if (h1 >= 0 && h2 >= 0 && !temp.isEmpty()) {
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = ("2,0,0").getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        encodedPayload = ("2,1,1,"+t1+","+t2+","+tempSet.getText()).getBytes("UTF-8");
                        message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(getActivity().getBaseContext(), "Enter both times and set temperature", Toast.LENGTH_SHORT).show();
                }
            }
        });

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String temp = tempSet.getText().toString();
                if(isChecked){
                    if(!temp.isEmpty()){
                        byte[] encodedPayload = new byte[0];
                        try {
                            encodedPayload = ("2,0,0").getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload);
                            client.publish(topic, message);
                            encodedPayload = ("2,2,1,"+tempSet.getText()).getBytes("UTF-8");
                            message = new MqttMessage(encodedPayload);
                            client.publish(topic, message);
                            onOff3 = true;
                        } catch (UnsupportedEncodingException | MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        toggleButton.toggle();
                        Toast.makeText(getActivity().getBaseContext(), "Set a temperature", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = "2,0,0".getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        onOff3 = false;
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }
                editor.putBoolean("onOff3", onOff3);
                editor.putString("prevTemp", temp);
                editor.commit();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}