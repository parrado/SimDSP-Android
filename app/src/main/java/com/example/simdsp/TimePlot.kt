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


class TimePlot(var timePlot: LineChart,var inOut:Boolean,  var activity: MainActivity) {

    private var timeRTChart: RTChart




    private val Ndatos = 512
    private val name = "Time"

    private var enabled = false




    init {


        timeRTChart = RTChart(timePlot, Ndatos, name)

        //   pressureRTChart.chart.addView(Button(pressureRTChart.chart.context))

        thread {


            while (true) {


                if (enabled) {




                    activity.runOnUiThread{
                        if(inOut)
                            timeRTChart.addData(activity.readInputSignal());

                        else
                            timeRTChart.addData(activity.readOutputSignal());

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



