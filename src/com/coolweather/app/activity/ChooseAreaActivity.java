package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;  // 进度条
	private TextView titleText;             //头
	private ListView listView;              
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;      //这是一个单例类
	private List<String> dataList = new ArrayList<String>(); //dataList
	
	/**
	 * 省列表
	 */
	private List<Province> provinceList;   //省列表
	
	/**
	 * 市列表
	 */
	private List<City> cityList;           //市列表
	
	/**
	 * 县列表
	 */  
	private List<County> countyList;       //县列表
	
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;            //共有省市县三种可能
	/**
	 * 是否从WeatherActivity跳转而来
	 */
	private boolean isFromWeatherActivity;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了City，且不是从WeatherActivity页面跳转而来，才会再次进入到WeatherActivity
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);        //隐藏标题栏
		setContentView(R.layout.choose_area);                 //加载页面布局
		listView = (ListView) findViewById(R.id.list_view);   
		titleText = (TextView) findViewById(R.id.title_text); 
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataList);//布局和data传递进适配器
		listView.setAdapter(adapter);                         //适配listview
		coolWeatherDB = CoolWeatherDB.getInstance(this);      //获得coolWeather实例
		listView.setOnItemClickListener(new OnItemClickListener() {//设置点击响应事件
			@Override
			public void onItemClick(AdapterView<?>arg0,View view ,int index,long arg3){
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				}else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}else if (currentLevel ==LEVEL_COUNTY) {
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();
	}
	
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
	 */
	private void queryProvinces(){
		
		provinceList = coolWeatherDB.loadProvinces();//加载省份
		if (provinceList.size()>0) {
			dataList.clear();               //先清空
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();//notifyDataSetChanged()可以在修改适配器绑定的数组后，
			                               //不用重新刷新Activity，通知Activity更新ListView。
			listView.setSelection(0);      //定位到头部
			titleText.setText("你好少年！");
			currentLevel = LEVEL_PROVINCE;
		}else {
			queryFromServer(null,"province");
		}
	}
	
	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());//加载市city列表
		if (cityList.size() > 0) {
			dataList.clear();            //先清空
			for (City city : cityList) {   //for each 循环
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());//修改 title Text内容
			currentLevel = LEVEL_CITY;
		} else {//若从数据库加载失败，则从网络加载
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	//                                       代码                                   省市县
	private void queryFromServer(final String code, final String type){
		
		String address;
		if (! TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog(); 
		//                                  回调函数接口
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result =false;
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
				}
				else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId()); 
				}else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if (result) {
					//通过runOnThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable(	) {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							}else if ("city".equals(type)) {
								queryCities();
							}else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				//通过runOnThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCancelable(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 */
	@Override
	public void onBackPressed(){
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		}
		else if (currentLevel == LEVEL_CITY) {
				queryProvinces();
			}
		else {
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}			
	}
}
