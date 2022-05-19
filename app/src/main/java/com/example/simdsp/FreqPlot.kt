package com.example.simdsp

import android.bluetooth.BluetoothSocket
import android.widget.Button
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.random.Random


class FreqPlot(var freqPlot: LineChart, var inOut: Boolean, samplingRate:Double,var activity: MainActivity) {

     var freqRTChart: FFTChart

    var samplingRate=samplingRate
    set(value) {
        field=value
        freqRTChart.samplingRate=value
    }




    private val Ndatos = 512
    private val name = "Frequency [kHz]"

    private var enabled = false





    init {


        freqRTChart = FFTChart(freqPlot, Ndatos, samplingRate,name)



        thread {


            while (true) {


                if (enabled) {




                    activity.runOnUiThread{
                        if(inOut)
                            freqRTChart.addData(activity.readInputSignal());

                        else
                        freqRTChart.addData(activity.readOutputSignal());

                    }

                    sleep(100);


                }


            }
        }
    }

    fun setEnabled(e: Boolean) {
        enabled = e
    }


    }



