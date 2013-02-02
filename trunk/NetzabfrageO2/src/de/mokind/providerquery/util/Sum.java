package de.mokind.providerquery.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class Sum implements Comparable<Sum>{

	private String name;
	private Integer minutes = 0;
	private Integer minutesMax = 0;
	private Integer smsCount = 0;
	private Integer smsMax = 0;

	private Map<String, Sum> children = new LinkedHashMap<String, Sum>();
	
	public Sum(String name, Integer minutes, Integer minutesMax) {
		super();
		this.name = name;
		this.minutes = minutes;
		this.minutesMax = minutesMax;
	}
	
	/**
	 * Indicates if this {@link Sum} has children
	 * @return
	 */
	public boolean hasChildren(){
		return children == null || children.size() == 0;
	}

	/**
	 * @see Comparable
	 */
	public int compareTo(Sum another) {
		if (another.minutes == minutes && name != null && another.name != null){
			return another.name.compareTo(name);
		}
		return another.minutes - minutes;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMinutes() {
		return this.minutes;
	}
	
	public Integer getSummarizedMinutes() {
		int minCount = 0;
		if (children == null){
			return getMinutes();
		}
		for (Sum s: children.values()){
			minCount += s.getMinutes();
		}
		return minCount;
	}

	public void setMinutes(Integer minutes) {
		this.minutes = minutes;
	}

	public Integer getMinutesMax() {
		return minutesMax;
	}

	public Map<String, Sum> getChildren() {
		return children;
	}

	public void setChildren(Map<String, Sum> children) {
		this.children = children;
	}
	
	
	public Integer getSmsCount() {
		return smsCount;
	}

	public void setSmsCount(Integer smsCount) {
		this.smsCount = smsCount;
	}
	
	public Integer getSummarizedSmsCount() {
		int minCount = 0;
		if (children == null){
			return getSmsCount();
		}
		for (Sum s: children.values()){
			minCount += s.getSmsCount();
		}
		return minCount;
	}

	public Integer getSmsMax() {
		return smsMax;
	}

	public void setSmsMax(Integer smsMax) {
		this.smsMax = smsMax;
	}
	
	
}
