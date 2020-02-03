package com.wex.jiraissuescount.model;

public class JiraTaskDTO {
	String taskId;
	String assignee;
	String taskName;
	int storyPoint;
	JiraVersionEnum jiraVersion;
	
	
	
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	public String getAssignee() {
		return assignee;
	}
	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public int getStoryPoint() {
		return storyPoint;
	}
	public void setStoryPoint(int storyPoint) {
		this.storyPoint = storyPoint;
	}
	public JiraVersionEnum getJiraVersion() {
		return jiraVersion;
	}
	public void setJiraVersion(JiraVersionEnum jiraVersion) {
		this.jiraVersion = jiraVersion;
	}

}
