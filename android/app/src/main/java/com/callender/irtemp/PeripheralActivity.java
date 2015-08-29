package com.callender.irtemp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class PeripheralActivity extends Activity implements BleWrapperUiCallbacks {

    private static final String TAG = "IRTEMP";

    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";

    final static public String IRTempServiceName = "IRTempService";

    private int mPeriod = 5;  // 5 second period (interval)

    private ListView           mListView;
    private LoggerListAdapter  mLogAdapter;
    private LayoutInflater     mInflater;

    private String mDeviceName;
    private String mDeviceRSSI;
    private String mDeviceAddress;

    private BleWrapper mBleWrapper;

    private TextView mDeviceNameView;
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;

    BluetoothGattService         mIRTempService  = null;
    BluetoothGattCharacteristic  mTemperature    = null;
    BluetoothGattCharacteristic  mInterval = null;


    private static final int SAMPLE_SIZE = 20;

    private XYPlot dynamicPlot = null;

    SimpleXYSeries TEMP_1 = null;


    public void uiDeviceConnected(final BluetoothGatt gatt,
                                  final BluetoothDevice device)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceStatus.setText("connected");
                invalidateOptionsMenu();
            }
        });
    }
    
    public void uiDeviceDisconnected(final BluetoothGatt gatt,
                                     final BluetoothDevice device)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceStatus.setText("disconnected");
                invalidateOptionsMenu();
            }
        });
    }
    
    public void uiNewRssiAvailable(final BluetoothGatt gatt,
                                   final BluetoothDevice device,
                                   final int rssi)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceRSSI = rssi + " db";
                mDeviceRssiView.setText(mDeviceRSSI);
            }
        });
    }

    public void uiAvailableServices(final BluetoothGatt gatt,
                                    final BluetoothDevice device,
                                    final List<BluetoothGattService> services)
    {
        configureIRTempService(gatt, device, services);
    }
    
    public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
                                            final BluetoothDevice device,
                                            final BluetoothGattService service,
                                            final BluetoothGattCharacteristic characteristic,
                                            final String strValue,
                                            final short[] data,
                                            final String timestamp)
    {   
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                UUID uuid = characteristic.getUuid();

                updateUI(data);

                if (uuid.equals(BleDefinedUUIDs.Characteristic.TEMPERATURE_CHAR)) {
                    mLogAdapter.addLine(strValue);
                }
            }
        });
    }
    
    public void uiCharacteristicWriteStatus(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic,
                                            final int status) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                UUID uuid = characteristic.getUuid();

                if (uuid.equals(BleDefinedUUIDs.Characteristic.INTERVAL_CHAR)) {
                    Log.d(TAG, String.format("onCharacteristicWrite: INTERVAL_CHAR: status: %d", status));

                    if (mTemperature != null) {
                        Log.d(TAG, String.format("Enable Notify's for Temperature Measure"));
                        mBleWrapper.setNotificationForCharacteristic(mTemperature, true);
                    }
                }

                if (uuid.equals(BleDefinedUUIDs.Characteristic.TEMPERATURE_CHAR)) {
                    Log.d(TAG, String.format("onCharacteristicWrite: TEMPERATURE: status: %d", status));
                }
            }
        });
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        // no need to handle that in this Activity (here, we are not scanning)
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        connectViewsVariables();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceRSSI = intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        mDeviceNameView.setText(mDeviceName);
        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRSSI);
        getActionBar().setTitle(mDeviceName);

        mListView = (ListView)findViewById(android.R.id.list);
        mLogAdapter = new LoggerListAdapter(this);
        mListView.setAdapter(mLogAdapter);

        /*  Plot initialization */

        dynamicPlot = (XYPlot) findViewById(R.id.dynamicPlot);

        dynamicPlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        dynamicPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);

        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0.0"));
        dynamicPlot.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);

        dynamicPlot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        dynamicPlot.setTicksPerDomainLabel(1);
        dynamicPlot.setTicksPerRangeLabel(1);

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setTextSize(30);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setTextSize(20);

        dynamicPlot.getGraphWidget().setDomainLabelWidth(40);
        dynamicPlot.getGraphWidget().setRangeLabelWidth(80);

        dynamicPlot.setDomainLabel("time");
        dynamicPlot.setDomainStep(XYStepMode.SUBDIVIDE, 11);
        dynamicPlot.setDomainValueFormat(new DecimalFormat("#"));
        dynamicPlot.getDomainLabelWidget().pack();

        dynamicPlot.setRangeLabel("Temp");
        dynamicPlot.setRangeStep(XYStepMode.SUBDIVIDE, 17);
        dynamicPlot.getRangeLabelWidget().pack();

        dynamicPlot.setRangeBoundaries(-20, 80, BoundaryMode.FIXED);
        dynamicPlot.setDomainBoundaries(0, SAMPLE_SIZE, BoundaryMode.FIXED);

        Paint textPaint = dynamicPlot.getLegendWidget().getTextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24);

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(10);
        dynamicPlot.getLegendWidget().setBorderPaint(bgPaint);

        dynamicPlot.getLegendWidget().setTableModel(new DynamicTableModel(1,8));
        dynamicPlot.getLegendWidget().setSize(new SizeMetrics(240, SizeLayoutType.ABSOLUTE, 140, SizeLayoutType.ABSOLUTE));
        dynamicPlot.getLegendWidget().position(30, XLayoutStyle.ABSOLUTE_FROM_RIGHT,
                                               30, YLayoutStyle.ABSOLUTE_FROM_TOP,
                                               AnchorPosition.RIGHT_TOP);

        dynamicPlot.getLegendWidget().setPadding(10,1,1,1);

        TEMP_1 = new SimpleXYSeries("Temp-1");
        TEMP_1.useImplicitXVals();

        // Blue line for LED-1 axis.
        LineAndPointFormatter fmt1 = new LineAndPointFormatter(Color.BLUE, null, null, null);
        dynamicPlot.addSeries(TEMP_1, fmt1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);

        if (mBleWrapper.initialize() == false) {
            finish();
        }

        // start automatically connecting to the device
        mDeviceStatus.setText("connecting ...");
        mBleWrapper.connect(mDeviceAddress);
    };

    @Override
    protected void onPause() {
        super.onPause();

        mLogAdapter.resetLines();

        mBleWrapper.stopMonitoringRssiValue();
        mBleWrapper.disconnect();
        mBleWrapper.close();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.peripheral, menu);
        if (mBleWrapper.isConnected()) {
            menu.findItem(R.id.device_connect).setVisible(false);
            menu.findItem(R.id.device_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.device_connect).setVisible(true);
            menu.findItem(R.id.device_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

            case R.id.device_connect:
                mDeviceStatus.setText("connecting ...");
                mBleWrapper.connect(mDeviceAddress);
                return true;

            case R.id.device_disconnect:
                mBleWrapper.disconnect();
                return true;

            case android.R.id.home:
                mBleWrapper.disconnect();
                mBleWrapper.close();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void updateUI(short[] data) {

        // Update the Plot

        // Remove oldest vector data.
        if (TEMP_1.size() > SAMPLE_SIZE) {
            TEMP_1.removeFirst();
        }

        // Add the latest vector data.
        TEMP_1.addLast(null, data[0]);

        // Redraw the Plots.
        dynamicPlot.redraw();
    }

    private void connectViewsVariables() {
        mDeviceNameView    = (TextView) findViewById(R.id.peripheral_name);
        mDeviceAddressView = (TextView) findViewById(R.id.peripheral_address);
        mDeviceRssiView    = (TextView) findViewById(R.id.peripheral_rssi);
        mDeviceStatus      = (TextView) findViewById(R.id.peripheral_status);
    }

    private void intervalUpdate() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);

                    Log.d(TAG, "writeDataToCharacteristic(Config(Interval) : " + mPeriod);

                    ByteBuffer buffer = ByteBuffer.allocate(4);  // sizeof uint32_t
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putInt(mPeriod);
                    mBleWrapper.writeDataToCharacteristic(mInterval, buffer.array());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void configureIRTempService(final BluetoothGatt gatt,
                                        final BluetoothDevice device,
                                        final List<BluetoothGattService> services)
    {
        Log.d(TAG, "configureIRTempService");

        mIRTempService = null;
        mTemperature = null;

        /* Find IRTemp Service in Service-list for device. */
        for (BluetoothGattService service : mBleWrapper.getCachedServices()) {

            String uuid = BleNamesResolver.resolveServiceName(service.getUuid().toString());

            if (uuid.equals(IRTempServiceName) == true) {
                Log.d(TAG, "service found: " + IRTempServiceName);
                mIRTempService = service;
                continue;
            }
        }

        /* If IRTempService was found, then find the interesting characteristics */
        if (mIRTempService != null) {

            for (BluetoothGattCharacteristic characteristic : mIRTempService.getCharacteristics()) {

                String uuid = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString());

                Log.d(TAG, "char found: " + uuid);

                if (uuid.equals("Temperature Characteristic")) {
                    mTemperature = characteristic;
                }

                if (uuid.equals("Interval Characteristic")) {
                    mInterval = characteristic;
                }
            }

            if (mInterval != null) {
                Log.d(TAG, String.format("Send Interval config"));
                intervalUpdate();
            }
        }
    }

    /*
     * This is the list adapter for the Logger, it holds an array of strings and adds them
     * to the list view recycling views for obvious performance reasons.
     */
    public class LoggerListAdapter extends BaseAdapter {

        private ArrayList<String> mLines;

        public LoggerListAdapter(Context c) {
            mLines = new ArrayList<String>();
            mInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mLines.size();
        }

        public long getItemId(int pos) {
            return pos;
        }

        public Object getItem(int pos) {
            return mLines.get(pos);
        }

        public View getView(int pos, View convertView, ViewGroup parent) {
            TextView holder;
            String line = mLines.get(pos);

            if (convertView == null) {
                // Inflate the view here because there's no existing view object.
                convertView = mInflater.inflate(R.layout.log_item, parent, false);

                holder = (TextView) convertView.findViewById(R.id.log_line);
                holder.setTypeface(Typeface.MONOSPACE);

                convertView.setTag(holder);
            }
            else {
                holder = (TextView) convertView.getTag();
            }

            holder.setText(line);

            mListView.setSelection(this.getCount() - 1);

            return convertView;
        }

        public void addLine(String line) {
            mLines.add(line);
            notifyDataSetChanged();
        }
 
        public void resetLines() {
            mLines.clear();
            notifyDataSetChanged();
        }

        public void updateView() {
            notifyDataSetChanged();
        }
    }
}
