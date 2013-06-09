package com.nonstop.android.SoC.Data;

public class GlasOSensorData {
	
	public enum Alarm{
		NOUSE,AROUND,ONTIME
	}
	char heartbeat_=0;
	char speed_=0;
	Alarm alarm_=Alarm.NOUSE;
	char length_=0;
	int cadence_=0;	
	GlasOPacket sensor_packet_;
	
	public GlasOSensorData(){
		heartbeat_=0;
		speed_=0;
		alarm_=Alarm.NOUSE;
		length_=0;
		cadence_=0;	
		sensor_packet_ = new GlasOPacket();
		sensor_packet_.setMode((char)0x20);
	}

	public void setHeartbeat_(int progress) {
		this.heartbeat_ = (char)progress;
		sensor_packet_.setData1((char)progress);
	}

	public void setSpeed_(int progress) {
		this.speed_ = (char)progress;
		sensor_packet_.setData2((char)progress);
	}

	public void setAlarm_(Alarm alarm_) {
		this.alarm_ = alarm_;
		switch(alarm_){
		case AROUND:
			sensor_packet_.setData2((char)0x10);
			break;
		case ONTIME:
			sensor_packet_.setData2((char)0xFF);
			break;
		default:
			sensor_packet_.setData5((char)0x00);
			break;
		}
	}

	public void setLength_(char length_) {
		this.length_ = length_;
		sensor_packet_.setData6((char)speed_);
	}

	public void setCadence_(int cadence_) {
		this.cadence_ = cadence_;
		sensor_packet_.setData3((char)cadence_);
		sensor_packet_.setData4((char)cadence_);
	}
	public String makeSensorPacket(){
		return sensor_packet_.makePacket();
	}
	
}
