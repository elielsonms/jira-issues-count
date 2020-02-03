package com.wex.jiraissuescount.model;

import java.time.LocalDate;
import java.util.List;

public class JiraSprintDTO {
	
	LocalDate startDate;
	LocalDate finishDate;
	List<JiraTaskDTO> tasks;
	JiraVersionEnum jiraVersion;
	
	public LocalDate getStartDate() {
		return startDate;
	}
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}
	public LocalDate getFinishDate() {
		return finishDate;
	}
	public void setFinishDate(LocalDate finishDate) {
		this.finishDate = finishDate;
	}
	public List<JiraTaskDTO> getTasks() {
		return tasks;
	}
	public void setTasks(List<JiraTaskDTO> tasks) {
		this.tasks = tasks;
	}
	public JiraVersionEnum getJiraVersion() {
		return jiraVersion;
	}
	public void setJiraVersion(JiraVersionEnum jiraVersion) {
		this.jiraVersion = jiraVersion;
	}
	
	
	

}
