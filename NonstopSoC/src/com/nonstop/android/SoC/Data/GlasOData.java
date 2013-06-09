package com.nonstop.android.SoC.Data;


public class GlasOData {
	public enum Direction {
	    LEFT, RIGHT, NORMAL 
	}
	public enum Exceed {
	    EXCEED, EXCEEDHIGH, NORMALSPEED 
	}
	public enum Status{
		NORMAL,WRONG
	}
	public enum NaviStatus{
		ON, OFF
	}
	public enum OnTime{
		ON, OFF
	}
	public enum Alarm{
		NOUSE,AROUND,ONTIME
	}
	
	char heartbeat_=0;
	char velocity_=0;
	Alarm alarm_=Alarm.NOUSE;
	char distance_=0;
	//int cadence_=0;		
	Direction direction_ = Direction.NORMAL;
	Exceed exceed_ = Exceed.NORMALSPEED;
	Status status_ = Status.NORMAL;
	NaviStatus navistatus_=NaviStatus.OFF;
	OnTime ontime_=OnTime.OFF;
	
	GlasOPacket glaso_packet_;
	
	public GlasOData(){
		direction_ = Direction.NORMAL;
		exceed_ = Exceed.NORMALSPEED;
		status_ = Status.NORMAL;
		navistatus_ = NaviStatus.OFF;
		heartbeat_=0;
		velocity_=0;
		alarm_=Alarm.NOUSE;
		distance_=0;
		//cadence_=0;	
		
		glaso_packet_ = new GlasOPacket();

	}

	
	public void setStatus(Status s){
		status_ = s;
		switch(s){
		case NORMAL:
			glaso_packet_.setData1(0x00);
			break;
		case WRONG:
			glaso_packet_.setData1(0x10);
			break;
		default:
			glaso_packet_.setData1(0x00);
			break;
		}
	}
	public void setDirection(Direction d){
		direction_ = d;
		switch(d){
		case LEFT:
			glaso_packet_.setData2(0x10);
			break;
		case RIGHT:
			glaso_packet_.setData2(0x20);
			break;
		case NORMAL:
			glaso_packet_.setData2(0x00);
			break;
		default:
			glaso_packet_.setData2(0x00);
			break;
		}
	}

	public void setExceed(Exceed p){
		exceed_ = p;
		switch(p){
		case NORMALSPEED:
			glaso_packet_.setData3(0x00);
			break;
		case EXCEED:
			glaso_packet_.setData3(0x10);
			break;
		case EXCEEDHIGH:
			glaso_packet_.setData3(0x20);
			break;
		default:
			glaso_packet_.setData2(0x00);
			break;
		}
	}
	
	public void setHeartbeat_(int progress) {
		this.heartbeat_ = (char) progress;
		glaso_packet_.setData4(progress);
	}

	public void setVelocity_(int progress) {
		this.velocity_ = (char) progress;
		glaso_packet_.setData5(progress);
	}
	
	public void setDistance_(int progress) {
		this.distance_ = (char) progress;
		glaso_packet_.setData6(progress);
	}


	public void setAlarm_(Alarm alarm_) {
		this.alarm_ = alarm_;
		switch(alarm_){
		case AROUND:
			glaso_packet_.setData7(0x10);
			break;
		case ONTIME:
			glaso_packet_.setData7(0xFF);
			break;
		default:
			glaso_packet_.setData7(0x00);
			break;
		}
	}
	public void setOnTime(OnTime s){
		ontime_ = s;
		switch(s){
		case ON:
			glaso_packet_.setData8(0x01);
			break;
		case OFF:
			glaso_packet_.setData8(0x00);
			break;
		default:
			glaso_packet_.setData8(0x00);
			break;
		}
	}

	public String makePacket(){
		return glaso_packet_.makePacket();
	}
	
}
