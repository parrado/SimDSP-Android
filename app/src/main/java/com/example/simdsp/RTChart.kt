package com.example.simdsp

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet


class RTChart (var chart : LineChart, var Npoints:Int, var title: String) {




      init{

          val data = LineData()
          data.setValueTextColor(Color.WHITE)

          // add empty data

          // add empty data
          chart.data = data

        // enable description text
        chart.getDescription().setEnabled(true)

        // enable touch gestures

        // enable touch gestures
        chart.setTouchEnabled(true)

        // enable scaling and dragging

        // enable scaling and dragging
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)

        // if disabled, scaling can be done on x- and y-axis separately

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)

        // set an alternative background color

        // set an alternative background color
        chart.setBackgroundColor(Color.BLACK)

          // get the legend (only possible after setting data)

          // get the legend (only possible after setting data)
          val l: Legend = chart.getLegend()

          // modify the legend ...

          // modify the legend ...
          l.form = Legend.LegendForm.LINE
          //l.typeface = tfLight
          l.textColor = Color.WHITE

          val xl: XAxis = chart.getXAxis()
          //xl.typeface = tfLight
          xl.textColor = Color.WHITE
          xl.setDrawGridLines(false)
          xl.setAvoidFirstLastClipping(true)
          xl.isEnabled = false
          xl.axisMinimum=0f
          xl.axisMaximum=Npoints.toFloat()

         xl.setDrawLabels(true)


          val leftAxis: YAxis = chart.getAxisLeft()
          //leftAxis.typeface = tfLight
          leftAxis.textColor = Color.WHITE
          leftAxis.axisMaximum = 1.toFloat()
          leftAxis.axisMinimum = -1.toFloat()
          leftAxis.setDrawGridLines(true)

          val rightAxis: YAxis = chart.getAxisRight()
          rightAxis.isEnabled = false

    }

     fun addData(values: DoubleArray ) {

         val entries = mutableListOf<Entry>()

         for (i in 0 until Npoints) {
             entries.add(Entry(i.toFloat(), values[i].toFloat()))
         }

             val set = createSet(entries);
             val data = LineData(set)



             chart.data=data



             //chart.moveViewToX(data.entryCount.toFloat().toFloat())
            // chart.setVisibleXRangeMaximum(Npoints.toFloat())
             data.notifyDataChanged()


             chart.notifyDataSetChanged()

             chart.invalidate()





     }

    private fun createSet(data: MutableList<Entry>): LineDataSet? {
        val set = LineDataSet(data, title)
        set.axisDependency = AxisDependency.LEFT
        set.color = 0xFFFF6D00.toInt();
       // set.setCircleColor(Color.WHITE)
        set.lineWidth = 2f

        set.setDrawCircles(false)
       // set.fillAlpha = 65
        //set.fillColor = Color.WHITE
        //set.highLightColor = Color.rgb(244, 117, 117)
        set.valueTextColor = 0xFFFF6D00.toInt();
        set.valueTextSize = 9f
        set.setDrawValues(false)
        return set
    }


}