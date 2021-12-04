package com.example.appiot.ui.gallery;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
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

import com.example.appiot.MainActivity;
import com.example.appiot.R;
import com.example.appiot.databinding.FragmentGalleryBinding;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

public class GalleryFragment extends Fragment {

    private GalleryViewModel galleryViewModel;
    private FragmentGalleryBinding binding;
    int lampState, h1, m1, h2, m2;
    boolean onOff;
    String t1, t2;
    SharedPreferences sp;
    MqttAndroidClient client;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        String topic = "proyectoIOT1";
        h1 = -500;
        h2 = -500;

        client = new MqttAndroidClient(getActivity(), "tcp://broker.hivemq.com:1883", MqttClient.generateClientId());

        final TextView nightMode = binding.NightMode;
        final TextView timeStart = binding.timeStart;
        final TextView timeEnd = binding.timeEnd;
        final Button setTime = binding.setTime;
        final ToggleButton toggleButton = binding.toggleButton;

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
        lampState = sp.getInt("lampState", -300);

        if(lampState != 0){
            editor.putBoolean("onOff", false);
            editor.commit();
        }

        onOff = sp.getBoolean("onOff", false);
        toggleButton.setChecked(onOff);

        switch (lampState){
            case -300:
                nightMode.setVisibility(View.VISIBLE);
                nightMode.setText("Select Operating Mode");
                timeStart.setVisibility(View.GONE);
                timeEnd.setVisibility(View.GONE);
                setTime.setVisibility(View.GONE);
                toggleButton.setVisibility(View.GONE);
                break;
            case 0:
                nightMode.setVisibility(View.GONE);
                timeStart.setVisibility(View.GONE);
                timeEnd.setVisibility(View.GONE);
                setTime.setVisibility(View.GONE);
                toggleButton.setVisibility(View.VISIBLE);
                break;
            case 1:
                nightMode.setVisibility(View.GONE);
                timeStart.setVisibility(View.VISIBLE);
                timeEnd.setVisibility(View.VISIBLE);
                setTime.setVisibility(View.VISIBLE);
                toggleButton.setVisibility(View.GONE);
                break;
            case 2:
                nightMode.setVisibility(View.VISIBLE);
                nightMode.setText("Lamp is running on Night Mode");
                timeStart.setVisibility(View.GONE);
                timeEnd.setVisibility(View.GONE);
                setTime.setVisibility(View.GONE);
                toggleButton.setVisibility(View.GONE);
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
                if (t1 != null && t2 != null) {
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = ("1,0,1,0").getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        encodedPayload = ("1,1,1,"+t1+","+t2).getBytes("UTF-8");
                        message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(getActivity().getBaseContext(), "Enter both times", Toast.LENGTH_SHORT).show();
                }
            }
        });

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = ("1,0,1,0").getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        encodedPayload = "1,2,1,1".getBytes("UTF-8");
                        message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    onOff = true;
                }
                else{
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = ("1,0,1,0").getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        encodedPayload = "1,2,1,0".getBytes("UTF-8");
                        message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    onOff = false;
                }
                editor.putBoolean("onOff", onOff);
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