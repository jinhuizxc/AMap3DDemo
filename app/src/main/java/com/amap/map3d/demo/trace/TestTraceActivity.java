package com.amap.map3d.demo.trace;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.trace.LBSTraceClient;
import com.amap.api.trace.TraceListener;
import com.amap.api.trace.TraceLocation;
import com.amap.api.trace.TraceOverlay;
import com.amap.map3d.demo.HistoryEnity;
import com.amap.map3d.demo.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jinhui on 2017/11/18.
 *
 * 学习下轨迹纠偏与路线绘制。
 */

public class TestTraceActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, TraceListener {

    private static final String TAG = TestTraceActivity.class.getSimpleName();

    private Button mGraspButton, mCleanFinishOverlay;
    private Spinner mRecordChoose;
    private TextView mResultShow, mLowSpeedShow;
    private RadioGroup mCoordinateTypeGroup;


    private MapView mMapView;
    private AMap mAMap;

    private String[] mRecordChooseArray;

    private static String mDistanceString, mStopTimeString;
    private static final String DISTANCE_UNIT_DES = " KM";
    private static final String TIME_UNIT_DES = " 分钟";

    private List<TraceLocation> mTraceList;
    private LBSTraceClient mTraceClient;
    private int mCoordinateType = LBSTraceClient.TYPE_AMAP;
    private ConcurrentMap<Integer, TraceOverlay> mOverlayList = new ConcurrentHashMap<Integer, TraceOverlay>();
    private int mSequenceLineID = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);

        mGraspButton = (Button) findViewById(R.id.grasp_button);
        mCleanFinishOverlay = (Button) findViewById(R.id.clean_finish_overlay_button);
        mMapView = (MapView) findViewById(R.id.map);
        mResultShow = (TextView) findViewById(R.id.show_all_dis);
        mLowSpeedShow = (TextView) findViewById(R.id.show_low_speed);
        mRecordChoose = (Spinner) findViewById(R.id.record_choose);
        mCoordinateTypeGroup = (RadioGroup) findViewById(R.id.coordinate_type_group);

        mDistanceString = getResources().getString(R.string.distance);
        mStopTimeString = getResources().getString(R.string.stop_time);

        mCleanFinishOverlay.setOnClickListener(this);
        mGraspButton.setOnClickListener(this);
        mCoordinateTypeGroup.setOnCheckedChangeListener(this);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        init();
    }

    private void init() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
            // 这个方法设置了地图是否允许通过手势来旋转
            mAMap.getUiSettings().setRotateGesturesEnabled(false);
           // 管理缩放控件
            mAMap.getUiSettings().setZoomControlsEnabled(false);
        }

        // 轨迹点
        mTraceList = TraceAsset.parseLocationsData(this.getAssets(),
                "traceRecord" + File.separator + "AMapTrace.txt");
        // 39.995825,longtitude 116.47676,speed 37.0,bearing 44.0,time 1470212510269
        Log.e(TAG, "mTraceList =" + mTraceList);

        // 设置spinner适配器
        mRecordChooseArray = TraceAsset.recordNames(this.getAssets());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mRecordChooseArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 绑定 Adapter到Spinner
        mRecordChoose.setAdapter(adapter);
        mRecordChoose.setOnItemSelectedListener(this);
    }


    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.grasp_button:
                Toast.makeText(this, "轨迹纠偏", Toast.LENGTH_SHORT).show();
                // 开始轨迹纠偏
                traceGrasp();
                break;
            case R.id.clean_finish_overlay_button:
                Toast.makeText(this, "清除线路", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 调起一次轨迹纠偏
     */
    private void traceGrasp() {
        Log.e(TAG, "traceGrasp方法执行");
//        if (mOverlayList.containsKey(mSequenceLineID)) {
//            TraceOverlay overlay = mOverlayList.get(mSequenceLineID);
//            overlay.zoopToSpan();
//            int status = overlay.getTraceStatus();
//            String tipString = "";
//            if (status == TraceOverlay.TRACE_STATUS_PROCESSING) {
//                tipString = "该线路轨迹纠偏进行中...";
//                setDistanceWaitInfo(overlay);
//            } else if (status == TraceOverlay.TRACE_STATUS_FINISH) {
//                setDistanceWaitInfo(overlay);
//                tipString = "该线路轨迹已完成";
//            } else if (status == TraceOverlay.TRACE_STATUS_FAILURE) {
//                tipString = "该线路轨迹失败";
//            } else if (status == TraceOverlay.TRACE_STATUS_PREPARE) {
//                tipString = "该线路轨迹纠偏已经开始";
//            }
//            Toast.makeText(this.getApplicationContext(), tipString,
//                    Toast.LENGTH_SHORT).show();
//            return;
//        }
        TraceOverlay mTraceOverlay = new TraceOverlay(mAMap);
        mOverlayList.put(mSequenceLineID, mTraceOverlay);
        List<LatLng> mapList = traceLocationToMap(mTraceList);
        mTraceOverlay.setProperCamera(mapList);
        Log.e(TAG, "mapList =" + mapList);
        mResultShow.setText(mDistanceString);
        mLowSpeedShow.setText(mStopTimeString);
        mTraceClient = new LBSTraceClient(this.getApplicationContext());
        mTraceClient.queryProcessedTrace(mSequenceLineID, mTraceList,
                mCoordinateType, this);
    }

    /**
     * 轨迹纠偏点转换为地图LatLng
     *
     * @param traceLocationList
     * @return
     */
    private List<LatLng> traceLocationToMap(List<TraceLocation> traceLocationList) {
        Log.e(TAG, "轨迹纠偏点转换为地图LatLng");
        List<LatLng> mapList = new ArrayList<LatLng>();
        for (TraceLocation location : traceLocationList) {
            LatLng latlng = new LatLng(location.getLatitude(),
                    location.getLongitude());
            mapList.add(latlng);
        }
        return mapList;
    }

    private void setDistanceWaitInfo(TraceOverlay overlay) {
        /**
         * 设置显示总里程和等待时间
         *
         * @param overlay
         */
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Toast.makeText(this, "onCheckedChanged", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "onItemSelected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    // ——————————————————————-TraceListener的方法——————————————-

    /**
     * 轨迹纠偏失败回调
     */
    @Override
    public void onRequestFailed(int i, String s) {
        Log.e(TAG, "onRequestFailed");
    }

    /**
     * 轨迹纠偏过程回调
     */
    @Override
    public void onTraceProcessing(int lineID, int i1, List<LatLng> segments) {
        Log.e(TAG, "onTraceProcessing");
        if (segments == null) {
            return;
        }
        if (mOverlayList.containsKey(lineID)) {
            TraceOverlay overlay = mOverlayList.get(lineID);
            overlay.setTraceStatus(TraceOverlay.TRACE_STATUS_PROCESSING);
            overlay.add(segments);
        }
    }

    /**
     * 轨迹纠偏结束回调
     */
    @Override
    public void onFinished(int i, List<LatLng> list, int i1, int i2) {
        Log.e(TAG, "onFinished");
    }
}
