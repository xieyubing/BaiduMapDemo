package net.xb.baidumapdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

import net.xb.baidumapdemo.util.MyLocationListener;
import net.xb.baidumapdemo.util.MyOrientationListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnGetSuggestionResultListener {

    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 0;
    TextureMapView mMapView = null;
    private BaiduMap mbaiduMap;
    private LocationClient mLocationClient = null;
    private LinearLayout llCondition;
    private LinearLayout llPattern;
    private LinearLayout llGps;
    private LinearLayout llSearch;
    private Button btuSearch;
    private AutoCompleteTextView actvSearch;
    private volatile boolean isFristLocation = true;
    private MyLocationListener myLocationListener;
    private MyOrientationListener orientationListener;
    private int mXDirection = 100;
    /**
     * 最新一次的经纬度
     */
    private double mCurrentLantitude;
    private double mCurrentLongitude;
    private float mCurrentAccracy;
    private String mCurrentlocation;
    private PoiSearch mPoiSearch;
    private SuggestionSearch mSuggestionSearch;
    private ArrayAdapter<String> sugAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(getApplicationContext());
        SDKInitializer.initialize(getApplicationContext());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        mPoiSearch = PoiSearch.newInstance();
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        llCondition = (LinearLayout) findViewById(R.id.ll_condition);
        llPattern = (LinearLayout) findViewById(R.id.ll_pattern);
        llGps = (LinearLayout) findViewById(R.id.ll_gps);
        llSearch = (LinearLayout) findViewById(R.id.ll_search);
        btuSearch = (Button) findViewById(R.id.btu_search);
        actvSearch = (AutoCompleteTextView) findViewById(R.id.actv_search);
        sugAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line);
        actvSearch.setAdapter(sugAdapter);
        isFristLocation = true;
        llCondition.setOnClickListener(this);
        llPattern.setOnClickListener(this);
        llGps.setOnClickListener(this);
        llSearch.setOnClickListener(this);
        btuSearch.setOnClickListener(this);

        actvSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() <= 0) {
                    return;
                }
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(s.toString()).city("泸州"));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mbaiduMap = mMapView.getMap();
        mbaiduMap.setMyLocationEnabled(true);

        initMyLocation();
        initOritationListener();
        initPoiSearch();
    }

    public void initPoiSearch() {
        mPoiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            public void onGetPoiResult(PoiResult result) {
                //获取POI检索结果
                if (result.getAllPoi() == null) {
                    Toast.makeText(getApplicationContext(), "定位失败,暂无数据信息", Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(), "定位成功，找到数据", Toast.LENGTH_LONG).show();
                }
            }

            public void onGetPoiDetailResult(PoiDetailResult result) {
                //获取Place详情页检索结果
                if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                    Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(MainActivity.this, "成功，查看详情页面", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
                //室内Poi数据
            }

        });
    }


    public void initOritationListener() {
        orientationListener = new MyOrientationListener(getApplicationContext());
        orientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mXDirection = (int) x;
                // 构造定位数据
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(mCurrentAccracy)
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(mXDirection)
                        .latitude(mCurrentLantitude)
                        .longitude(mCurrentLongitude).build();
                // 设置定位数据
                mbaiduMap.setMyLocationData(locData);
                // 设置自定义图标
                mbaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, BitmapDescriptorFactory.fromResource(R.mipmap.icon_marka)));

            }
        });

    }

    public void initMyLocation() {
        myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }


    protected void onStart() {
        // 开启图层定位
        mbaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }
        // 开启方向传感器
        orientationListener.start();
        super.onStart();
    }

    @Override
    protected void onStop() {
        // 关闭图层定位
        mbaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();

        // 关闭方向传感器
        orientationListener.stop();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        mbaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();

    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();

    }

    public void onReceivePoi(BDLocation poiLocation) {
        if (poiLocation == null) {
            return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
/*            case R.id.bmapView:
                if(llSearch.getVisibility()==View.VISIBLE){
                    llSearch.setVisibility(View.GONE);
                    llPattern.setVisibility(View.GONE);
                    llGps.setVisibility(View.GONE);
                    llCondition.setVisibility(View.GONE);
                }else {
                    llSearch.setVisibility(View.VISIBLE);
                    llPattern.setVisibility(View.VISIBLE);
                    llGps.setVisibility(View.VISIBLE);
                    llCondition.setVisibility(View.VISIBLE);
                }
                break;*/
            case R.id.ll_gps:
                //定位当前位置
                LatLng ll = new LatLng(mCurrentLantitude, mCurrentLongitude);
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mbaiduMap.animateMapStatus(u);
                break;
            case R.id.ll_pattern:
                //地图模式切换
                showpopView(v);
                break;
            case R.id.ll_condition:
                //路况

                break;
            case R.id.btu_search:
                mPoiSearch.searchInCity((new PoiCitySearchOption())
                        .city("泸州")
                        .keyword(actvSearch.getText().toString())
                        .pageNum(0));
                break;
        }
    }

    public void showpopView(View v) {
        View view = getLayoutInflater().inflate(R.layout.switchingmap, null);
        PopupWindow popupwindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupwindow.setFocusable(true);
        popupwindow.setOutsideTouchable(true);
        popupwindow.showAsDropDown(v);

        FrameLayout StandardMap = (FrameLayout) view.findViewById(R.id.tv_standardmap);
        FrameLayout SatelliteMap = (FrameLayout) view.findViewById(R.id.tv_satellitemap);
        FrameLayout TransitMap = (FrameLayout) view.findViewById(R.id.tv_transitmap);

        StandardMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mbaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }
        });
        SatelliteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mbaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });
    }

    // 建议查询事件监听所要实现的方法,这个方法主要是为AutoCompleteTextView提供参数
    @Override
    public void onGetSuggestionResult(SuggestionResult suggestionResult) {
        if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
            return;
        }
        sugAdapter.clear();
        for (SuggestionResult.SuggestionInfo info : suggestionResult.getAllSuggestions()) {
            if (info.key != null)
                sugAdapter.add(info.key);
        }
        sugAdapter.notifyDataSetChanged();
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(mXDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mCurrentlocation = location.getAddrStr();
            mCurrentAccracy = location.getRadius();
            mCurrentLantitude = location.getLatitude();
            mCurrentLongitude = location.getLongitude();
            mbaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, BitmapDescriptorFactory.fromResource(R.mipmap.icon_marka)));
            mbaiduMap.setMyLocationData(locData);

            if (isFristLocation) {
                isFristLocation = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mbaiduMap.animateMapStatus(u);
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            Toast.makeText(MainActivity.this, "获取成功", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this,"获取失败",Toast.LENGTH_LONG).show();
        }
    }
}


