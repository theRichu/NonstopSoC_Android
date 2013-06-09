package com.nonstop.android.SoC.Data;

public class GlasONaviData {
	public enum Direction {
	    LEFT, RIGHT, NORMAL 
	}
	public enum Speed {
	    EXCEED, EXCEEDHIGH, NORMALSPEED 
	}
	public enum Status{
		NORMAL,WRONG
	}
	public enum NaviStatus{
		ON, OFF
	}
	
	Direction direction_ = Direction.NORMAL;
	Speed speed_ = Speed.NORMALSPEED;
	Status status_ = Status.NORMAL;
	NaviStatus navistatus_=NaviStatus.OFF;
	
	GlasOPacket navi_packet_;
	
	public GlasONaviData(){
		direction_ = Direction.NORMAL;
		speed_ = Speed.NORMALSPEED;
		status_ = Status.NORMAL;
		navistatus_ = NaviStatus.OFF;
		
		navi_packet_ = new GlasOPacket();
		navi_packet_.setMode((char)0x10);
	}
	
	public void setNaviMode(NaviStatus n){
		navistatus_ = n;
		if(n==NaviStatus.ON) {navi_packet_.setMode((char)0x10);}
		else{navi_packet_.setMode((char)0x0);}
	}
	public void setDirection(Direction d){
		direction_ = d;
		switch(d){
		case LEFT:
			navi_packet_.setData2((char)0x10);
			break;
		case RIGHT:
			navi_packet_.setData2((char)0x20);
			break;
		case NORMAL:
			navi_packet_.setData2((char)0x00);
			break;
		default:
			navi_packet_.setData2((char)0x00);
			break;
		}
	}
	public void setStatus(Status s){
		status_ = s;
		switch(s){
		case NORMAL:
			navi_packet_.setData1((char)0x00);
			break;
		case WRONG:
			navi_packet_.setData1((char)0x10);
			break;
		default:
			navi_packet_.setData2((char)0x00);
			break;
		}
	}
	public void setSpeed(Speed p){
		speed_ = p;
		switch(p){
		case NORMALSPEED:
			navi_packet_.setData3((char)0x00);
			break;
		case EXCEED:
			navi_packet_.setData3((char)0x10);
			break;
		case EXCEEDHIGH:
			navi_packet_.setData3((char)0x20);
			break;
		default:
			navi_packet_.setData2((char)0x00);
			break;
		}
	}
	
	public String makeNaviPacket(){
		return navi_packet_.makePacket();
	}
	
}
