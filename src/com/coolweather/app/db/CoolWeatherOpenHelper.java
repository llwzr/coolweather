package com.coolweather.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class CoolWeatherOpenHelper extends SQLiteOpenHelper {
	
	/**
	 *  Province表建表语句
	 */
	public static final String CREATE_PROVINCE = "create table Province ("
				+ "id integer primary key autoincrement, " //自增长
				+ "province_name text, "                   //省名字
				+ "province_code text)";                   //省级代号
	/**
	 *  City表建表语句
	 */
	public static final String CREATE_CITY = "create table City ("
				+ "id integer primary key autoincrement, "    //自增长
				+ "city_name text, "                          //市名
				+ "city_code text, "                          //市级代号
				+ "province_id integer)";                     //City表 关联 Province表 的外键
	/**
	 *  County表建表语句
	 */
	public static final String CREATE_COUNTY = "create table County ("
				+ "id integer primary key autoincrement, "    //自增长
				+ "county_name text, "                        //县名
				+ "county_code text, "                        //县级代号
				+ "city_id integer)";                         //County表 关联 City表 的外键

	//构造函数
	public CoolWeatherOpenHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	
	//创建数据库db，其中包含3张表
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_PROVINCE);  // 创建Province表
		db.execSQL(CREATE_CITY);      // 创建City表
		db.execSQL(CREATE_COUNTY);    // 创建County表
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}

}
