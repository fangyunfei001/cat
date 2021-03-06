package com.dianping.cat.alarm.spi.decorator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.dianping.cat.alarm.spi.AlertEntity;

public abstract class Decorator {

	protected DateFormat m_format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public String generateContent(AlertEntity alert) {
		return alert.getContent();
	}

	public abstract String generateTitle(AlertEntity alert);

	public abstract String getId();

}
