//Actividad principal de SimDSP
package com.example.simdsp;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;


import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;


public class MainActivity extends AppCompatActivity {
    Process process = null;
    ProcessBuilder processBuilder;
    Thread threadConsole;
    Thread threadFileDescriptor;
    DataOutputStream os =null ;
    DataInputStream  is=null;
    EditText text;
    TextView textView;
    TextView textViewFreq;
    TextView textViewNoise;
    EditText textFreq;
    EditText textNoise;
    Button button1;
    ToggleButton toggleButton;
    Button button3;
    Button button4;
    SeekBar freqSeekBar;
    SeekBar noiseSeekBar;
    CheckBox noiseCheckBox;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private  static  final double MAX_NOISE_SNR=40;

    boolean isRunning=false;

    boolean viewModeFlag=true;
    boolean actionModeFlag=true;
    boolean signalModeFlag=true;
    boolean noiseFlag=false;

    LineChart timeChart;
    LineChart freqChart;

    TimePlot timePlotIn;
    FreqPlot freqPlotIn;

    TimePlot timePlotOut;
    FreqPlot freqPlotOut;



    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    static {
        System.loadLibrary("sharedmem");
    }


    public native int writeFileDescriptor();
    public native void closeFileDescriptor();
    public native double[] readInputSignal();
    public native double[] readOutputSignal();
    public native double readSamplingRate();
    public native void writeInputSource(double src);
    public native void writeSineFrequency(double freq);
    public native void writeNoiseFlag(double noiseFlag);
    public native void writeNoisePower(double noisePower);

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        text = (EditText) findViewById(R.id.editText);
        button1=(Button)findViewById(R.id.button);
        toggleButton=(ToggleButton)findViewById(R.id.toggleButton2);
        button3=(Button)findViewById(R.id.button3);
        button4=(Button)findViewById(R.id.button4);

        freqChart = findViewById(R.id.freqChart);
        timeChart = findViewById(R.id.timeChart);
        textView=findViewById(R.id.textView);

        noiseCheckBox=findViewById(R.id.checkBox);
        freqSeekBar=findViewById(R.id.seekBar);
        noiseSeekBar=findViewById(R.id.seekBar2);
        textFreq=findViewById(R.id.textFreq);
        textNoise=findViewById(R.id.textNoise);

        textViewFreq=findViewById(R.id.textViewFreq);
        textViewNoise=findViewById(R.id.textViewNoise);



        timeChart.setNoDataText("Start SimDSP sketch by hitting START DSP button");
        freqChart.setNoDataText("");


        timePlotIn = new TimePlot(timeChart, true, MainActivity.this);
        freqPlotIn = new FreqPlot(freqChart, true, 8000.0, MainActivity.this);

        timePlotOut = new TimePlot(timeChart, false, MainActivity.this);
        freqPlotOut = new FreqPlot(freqChart, false, 8000.0, MainActivity.this);




        noiseSeekBar.setEnabled(false);
        noiseCheckBox.setEnabled(false);
        textNoise.setEnabled(false);
        textNoise.setTextColor(0x3F000000);
        textViewNoise.setTextColor(0x3F000000);

        freqSeekBar.setEnabled(false);
        textFreq.setEnabled(false);
        textFreq.setTextColor(0x3F000000);
        textViewFreq.setTextColor(0x3F000000);
        textViewNoise.setEnabled(false);
        textViewFreq.setEnabled(false);

        toggleButton.setEnabled(false);
        toggleButton.setBackgroundColor(0x3FFF6D00);

        button3.setEnabled(false);
        button3.setBackgroundColor(0x3F2525F8);

        button4.setEnabled(false);
        button4.setBackgroundColor(0x3FFF6D00);

        text.setTypeface(Typeface.MONOSPACE);
        text.setEnabled(false);

        noiseSeekBar.setMax(100);
        freqSeekBar.setMax(100);
        noiseSeekBar.setProgress(100);

        int latency;

        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        try{
            Method m = am.getClass().getMethod("getOutputLatency", int.class);
            latency = (Integer)m.invoke(am, AudioManager.STREAM_MUSIC);
        }catch(Exception e){
        }

        noiseSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                double noisePower;
                double SNR;

                if(fromUser) {


                    SNR=MAX_NOISE_SNR*((double)seekBar.getProgress())/100.0;
                    if(SNR>MAX_NOISE_SNR)
                        SNR=MAX_NOISE_SNR;

                    noisePower=-SNR;

                    textNoise.setText(String.valueOf(SNR));
                    writeNoisePower(noisePower);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {



            }
        });


        freqSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser) {
                    double freq;

                    freq=(double) seekBar.getProgress() / 100.0 * (readSamplingRate() / 2);

                    if (freq > readSamplingRate() / 2.0)
                        freq = readSamplingRate() / 2.0;

                    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
                    symbols.setDecimalSeparator('.');

                    DecimalFormat oneDigit = new DecimalFormat("#0.0");

                    oneDigit.setDecimalFormatSymbols(symbols);

                    textFreq.setText(String.valueOf(oneDigit.format(freq/1000.0)));
                    writeSineFrequency(freq);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {



            }
        });


        textNoise.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!s.toString().isEmpty()) {

                    double noisePower,SNR;

                    SNR = Double.valueOf(s.toString());

                    if (SNR > MAX_NOISE_SNR) {
                        SNR = MAX_NOISE_SNR;
                        textNoise.setTextColor(0xFFFF0000);
                    }else{
                        textNoise.setTextColor(0xFF000000);
                    }
                    noisePower=-SNR;
                    writeNoisePower(noisePower);
                       noiseSeekBar.setProgress((int)(100.0*(SNR)/MAX_NOISE_SNR));

                    //       textNoise.setText(String.valueOf(noisePower));


                }
            }
        });

        textFreq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!s.toString().isEmpty()) {

                    double freq;

                    freq = Double.valueOf(s.toString()) * 1000.0;

                    if (freq > readSamplingRate() / 2.0) {
                        freq = readSamplingRate() / 2;
                        textFreq.setTextColor(0xFFFF0000);
                    }
                    else
                    {
                        textFreq.setTextColor(0xFF000000);
                    }


                    writeSineFrequency(freq);
                    freqSeekBar.setProgress((int) ((freq * 100) / (readSamplingRate() / 2)));


                    //     textFreq.setText(String.valueOf(freq));


                }
            }
        });

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);



        threadConsole = new Thread(){
            public void run(){

        BufferedReader br=null;






                while(true) {

                    if (isRunning) {

                        if (is != null) {

                            try {



                                if (br == null)
                                    br = new BufferedReader(new InputStreamReader(is));


                                final String data = br.readLine();




                                runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                text.setText(text.getText().append(data + "\n"));

                                            }
                                        }

                                );

                            } catch (IOException e ) {
                                //e.printStackTrace();
                            }
                        } else {
                            br = null;

                        }

                    }

                }
            }
        };

        threadConsole.start();





    }

    public void startDSP(){

        String myExec;

        text.getText().clear();









        try {

            File f = new File("/data/local/tmp/simdspf/simdsp");
            if(f.exists() && !f.isDirectory()) {


                myExec = "cp   /data/local/tmp/simdspf/simdsp /data/data/com.example.simdsp/simdsp";
                process = Runtime.getRuntime().exec(myExec);
                process.waitFor();

                threadFileDescriptor = new Thread() {
                    public void run() {


                        try {


                            if (writeFileDescriptor() != -1) {
                                writeInputSource(0);

                                if(noiseCheckBox.isChecked())
                                writeNoiseFlag(1.0);
                                else
                                writeNoiseFlag(0.0);

                                writeNoisePower(-MAX_NOISE_SNR);
                                writeSineFrequency(0.0);
                                sleep(100);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        button3.setEnabled(true);
                                        button3.setBackgroundColor(0xFF2525F8);

                                        button4.setEnabled(true);
                                        button4.setBackgroundColor(0xFFFF6D00);

                                        button1.setEnabled(true);
                                        button1.setBackgroundColor(0xFF2525F8);
                                        button1.setText("STOP DSP");

                                        toggleButton.setEnabled(true);
                                        toggleButton.setBackgroundColor(0xfFFF6D00);

                                        if(toggleButton.isChecked()){


                                            enableFreqWidgets();
                                            writeSineFrequency(Double.valueOf(textFreq.getText().toString())*1000.0);
                                            writeInputSource(1.0);

                                        }
                                        else{

                                            disableFreqWidgets();
                                            writeInputSource(0.0);




                                        }

                                        enableNoiseWidgets();

                                        if (toggleButton.isChecked()) {

                                            enableFreqWidgets();

                                        }


                                        freqPlotOut.setSamplingRate(readSamplingRate());
                                        freqPlotIn.setSamplingRate(readSamplingRate());


                                        if (viewModeFlag) {
                                            if (actionModeFlag) {
                                                textView.setText("INPUT SIGNAL");
                                                timePlotIn.setEnabled(true);
                                                freqPlotIn.setEnabled(true);
                                            } else {
                                                textView.setText("OUTPUT SIGNAL");
                                                timePlotOut.setEnabled(true);
                                                freqPlotOut.setEnabled(true);
                                            }
                                        } else
                                            textView.setText("CONSOLE");
                                    }
                                });


                            } else {
                                closeFileDescriptor();
                            }
                        } catch (InterruptedException e) {
                            //e.printStackTrace();


                        }


                    }
                };

                threadFileDescriptor.start();


                myExec = "chmod 777 /data/data/com.example.simdsp/simdsp";
                process = Runtime.getRuntime().exec(myExec);
                process.waitFor();

                myExec = "/data/data/com.example.simdsp/simdsp";
                String myEnv[] = {"LD_LIBRARY_PATH=/data/data/com.example.simdsp/lib:$LD_LIBRARY_PATH"};

                try {
                    processBuilder = new ProcessBuilder(myExec);
                    processBuilder.environment().put("LD_LIBRARY_PATH","/data/data/com.example.simdsp/lib");

                    process=processBuilder.start();
                    //        Runtime.getRuntime().exec(myExec, myEnv);

                    if (process == null) {
                        closeFileDescriptor();
                        threadFileDescriptor.interrupt();
                        Toast.makeText(getApplicationContext(), "Couldn't start SimDSP sketch", Toast.LENGTH_SHORT).show();


                    } else {

                        isRunning = true;
                        Toast.makeText(getApplicationContext(), "Starting SimDSP sketch", Toast.LENGTH_SHORT).show();
                        os = new DataOutputStream(process.getOutputStream());
                        is = new DataInputStream(process.getInputStream());

                        button1.setEnabled(false);
                        button1.setBackgroundColor(0x3F2525F8);


                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Couldn't start SimDSP sketch", Toast.LENGTH_SHORT).show();

                }
            }
            else{
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Warning");
                alertDialog.setMessage("You have not loaded a SimDSP sketch from PC.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFFFF6D00);
                    }
                });

                alertDialog.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Couldn't start SimDSP sketch", Toast.LENGTH_SHORT).show();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }





    }

    public void stopDSP(){

        isRunning=false;
        closeFileDescriptor();

        if(process!=null){
            
            process.destroy();
            process=null;
            is=null;
            Toast.makeText(getApplicationContext(),"Stopping SimDSP sketch",Toast.LENGTH_SHORT).show();
            button1.setEnabled(true);
            button1.setBackgroundColor(0xFF2525F8);
            button1.setText("START DSP");


            toggleButton.setEnabled(false);
            toggleButton.setBackgroundColor(0x3FFF6D00);
            button3.setEnabled(false);
            button3.setBackgroundColor(0x3F2525F8);

            button4.setEnabled(false);
            button4.setBackgroundColor(0x3FFF6D00);

         disableFreqWidgets();
         disableNoiseWidgets1();



        }



    }

    public void startStopDSP(View view){
        if(isRunning){
            stopDSP();

        }
        else
        {
            startDSP();
        }

    }
    
    public void inputSignalSelect(View view){

        ToggleButton tb=(ToggleButton)view;

        if(tb.isChecked()){


        enableFreqWidgets();
            writeSineFrequency(Double.valueOf(textFreq.getText().toString())*1000.0);
            writeInputSource(1.0);

        }
        else{

            disableFreqWidgets();
            writeInputSource(0.0);




        }
        
    }

    public void onCheckBoxClick(View view){
        if(noiseCheckBox.isChecked()) {
           enableNoiseWidgets();
            writeNoisePower(-Double.valueOf(textNoise.getText().toString()));
           writeNoiseFlag(1.0);

        }

        else {
         disableNoiseWidgets2();
         writeNoiseFlag(0.0);
        }

    }

    public void actionMode(View view){

        if(viewModeFlag){
            if(actionModeFlag){
                actionModeFlag=false;
                textView.setText("OUTPUT SIGNAL");
                timePlotIn.setEnabled(false);
                freqPlotIn.setEnabled(false);
                timePlotOut.setEnabled(true);
                freqPlotOut.setEnabled(true);



            }
            else{
                actionModeFlag=true;
                textView.setText("INPUT SIGNAL");
                timePlotIn.setEnabled(true);
                freqPlotIn.setEnabled(true);
                timePlotOut.setEnabled(false);
                freqPlotOut.setEnabled(false);

            }


        }else{


            text.getEditableText().clear();
            Toast.makeText(getApplicationContext(),"Clearing console",Toast.LENGTH_SHORT).show();



        }

    }

    public void viewMode(View view){
        if(viewModeFlag){
            timeChart.setVisibility(View.GONE);
            freqChart.setVisibility(View.GONE);
            text.setVisibility(View.VISIBLE);
            timePlotIn.setEnabled(false);
            timePlotOut.setEnabled(false);
            freqPlotIn.setEnabled(false);
            freqPlotOut.setEnabled(false);
            viewModeFlag=false;
            button4.setText("TIME/FREQ VIEW");

            button3.setText("CLEAR CONSOLE");

            textView.setText("CONSOLE");


        }else
        {
            text.setVisibility(View.GONE);
            button4.setText("CONSOLE VIEW");
            button3.setText("INPUT/OUTPUT");
            timeChart.setVisibility(View.VISIBLE);
            freqChart.setVisibility(View.VISIBLE);
            viewModeFlag=true;
            if(actionModeFlag){
                timePlotIn.setEnabled(true);
                freqPlotIn.setEnabled(true);
                timePlotOut.setEnabled(false);
                freqPlotOut.setEnabled(false);

                textView.setText("INPUT SIGNAL");





            }else
            {




                timePlotIn.setEnabled(false);
                freqPlotIn.setEnabled(false);
                timePlotOut.setEnabled(true);
                freqPlotOut.setEnabled(true);



                textView.setText("OUTPUT SIGNAL");

            }

        }



    }

    public void enableNoiseWidgets()
    {
        noiseCheckBox.setEnabled(true);


        if(noiseCheckBox.isChecked()) {
            noiseSeekBar.setEnabled(true);
            textNoise.setEnabled(true);
            textViewNoise.setEnabled(true);
            textViewNoise.setTextColor(0xFF000000);
            textNoise.setTextColor(0xFF000000);
        }
        else {
            noiseSeekBar.setEnabled(false);
            textNoise.setEnabled(false);
            textViewNoise.setEnabled(false);
            textViewNoise.setTextColor(0x3F000000);
            textNoise.setTextColor(0x3F000000);
        }



    }

    public void disableNoiseWidgets1(){
        textNoise.setEnabled(false);
        noiseSeekBar.setEnabled(false);
        noiseCheckBox.setEnabled(false);
        textNoise.setEnabled(false);
        textNoise.setTextColor(0x3F000000);
        textViewNoise.setEnabled(false);
        textViewNoise.setTextColor(0x3F000000);
    }

    public void disableNoiseWidgets2(){
        textNoise.setEnabled(false);
        noiseSeekBar.setEnabled(false);

        textNoise.setEnabled(false);
        textNoise.setTextColor(0x3F000000);
        textViewNoise.setEnabled(false);
        textViewNoise.setTextColor(0x3F000000);
    }

    public void enableFreqWidgets(){
        textFreq.setTextColor(0xFF000000);
        textViewFreq.setTextColor(0xFF000000);



        freqSeekBar.setEnabled(true);
        textFreq.setEnabled(true);
    }

    public void disableFreqWidgets(){


        freqSeekBar.setEnabled(false);
        textFreq.setEnabled(false);
        textFreq.setTextColor(0x3F000000);
        textViewFreq.setEnabled(false);
        textViewFreq.setTextColor(0x3F000000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean r;
        // Handle item selection
        switch (item.getItemId()){
            case R.id.about:
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setIcon(R.drawable.uq);
                alertDialog.setTitle("About SimDSP for Android");
                alertDialog.setMessage("Alexander López Parrado\n" +
                        "Jorge Iván Marín Hurtado\n" +
                        "Luis Miguel Capacho Valbuena\n\n" +
                        "Universidad del Quindío (2021)");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

                r=true;
                break;

            case R.id.exit:
                System.exit(0);
                r=true;
                break;
            default:

                r=super.onOptionsItemSelected(item);
                break;


        }

        return r;

        }


    }




