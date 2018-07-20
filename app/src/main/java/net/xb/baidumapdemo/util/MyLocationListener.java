package net.xb.baidumapdemo.util;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import net.xb.baidumapdemo.R;

/**
 * Created by asus on 2017/4/14.
 * 定位
 */

public class MyLocationListener implements BDLocationListener {

    private BaiduMap mbaiduMap;
    private volatile boolean isFristLocation = true;
    private int mXDirection=100;
    public MyLocationListener(BaiduMap mbaiduMap,int mXDirection){
        this.mbaiduMap=mbaiduMap;
        this.mXDirection=mXDirection;
    }

    @Override
    public void onReceiveLocation(BDLocation location) {
        if (location == null) {
            return;
        }
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(mXDirection).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
      mbaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, BitmapDescriptorFactory.fromResource(R.mipmap.icon_marka)));
        mbaiduMap.setMyLocationData(locData);

        if (isFristLocation) {
            isFristLocation=false;
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            mbaiduMap.animateMapStatus(u);
        }
    }
    @Override
    public void onConnectHotSpotMessage(String s, int i) {

    }
}
