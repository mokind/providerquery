package de.mokind.providerquery.util;

public class Sum {

	public String name;
	public Integer minutes = 0;
	public Integer minutesMax = 0;
	public boolean showProgress = false;
	
	public Sum(String name, Integer minutes, Integer minutesMax, boolean showProgress) {
		super();
		this.name = name;
		this.minutes = minutes;
		this.minutesMax = minutesMax;
		this.showProgress = showProgress;
	}
	
}
