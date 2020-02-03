package com.wex.jiraissuescount;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wex.jiraissuescount.model.JiraSprintDTO;
import com.wex.jiraissuescount.model.JiraTaskDTO;
import com.wex.jiraissuescount.model.JiraVersionEnum;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Main {

	static DateTimeFormatter jiraFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd");
	static DateTimeFormatter docsFormat = DateTimeFormatter.ofPattern("MM/dd/YYYY");
	static DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM");
	static Gson gson = new Gson();
	static OkHttpClient client = new OkHttpClient();
	static Period twoWeeks = Period.ofWeeks(2);
	static Map<String,Integer> jiraStoryPointMap = oldJiraStoryPointsMap();

    public static void main(String[] args) throws IOException {
    	
    	if(args != null && args.length > 1){
	    	String oldJiraToken = args[0];
	    	String newJiraToken = args[1];
	    	
	    	List<JiraSprintDTO> sprints = new ArrayList<JiraSprintDTO>();
	    	
	    	if(!oldJiraToken.equals("no")){
		        //Old Jira
	    		List<JiraSprintDTO> sprintsOnOld = requestJira(JiraVersionEnum.SERVER, LocalDate.of(2018, 12, 29), LocalDate.of(2019, 06, 28) ,
						"https://jira.devtools.wexinc.com/rest/api/2/search?fields=issuekey, summary, assignee, customfield_10033&jql=project = WOLAPI AND assignee in (W000848, esilva, asouza, fneto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
						oldJiraToken);
	    		//sprintsOnOld.stream().forEach(sprint -> sprint.getTasks().stream().forEach(task -> System.out.println(task.getTaskId()+";"+task.getTaskName())));
	    		sprints.addAll(sprintsOnOld);
	    	}
	    	if(!newJiraToken.equals("no")){
				//Global Jira
	    		sprints.addAll(requestJira(JiraVersionEnum.GLOBAL, LocalDate.of(2019, 6, 1), LocalDate.now(),
						"https://wexinc.atlassian.net/rest/api/2/search/?fields=issuekey, summary, assignee, customfield_10024&jql=assignee in (douglas.lima, elielson.silva, pedro.lourenco, Felix.Neto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
						newJiraToken));
	    	}
	    	filterRepeatedTasksOnDifferentVersions(sprints);
	    	filterRepeatedIds(sprints);
	    	parseJira(sprints);
			System.out.println("end");
    	}else{
    		System.out.println("odd parameters passed");
    	}
	}
    private static void filterRepeatedTasksOnDifferentVersions(List<JiraSprintDTO> sprints) {
    	System.out.println("Removing repeated");
    	//Taking off tasks from old Jira version that are on new version
    	List<JiraSprintDTO> globalSprints = sprints.stream().filter(sprint -> sprint.getJiraVersion().equals(JiraVersionEnum.GLOBAL)).collect(Collectors.toList());
    	List<String> taskNamesOnGlobal = new ArrayList<String>();
    	globalSprints.stream().forEach(sprint -> 
    									taskNamesOnGlobal.addAll(sprint.getTasks().stream().
    														map(JiraTaskDTO::getTaskName).collect(Collectors.toList())));
    	List<JiraSprintDTO> serverSprints = sprints.stream().filter(sprint -> sprint.getJiraVersion().equals(JiraVersionEnum.SERVER)).collect(Collectors.toList());
    	for(JiraSprintDTO sprint : serverSprints ){
    		List<JiraTaskDTO> toRemoveFromSprint = new ArrayList<JiraTaskDTO>();
    		for(JiraTaskDTO task : sprint.getTasks()){
    			if(taskNamesOnGlobal.contains(task.getTaskName())){
    				System.out.println("Repeated "+task.getTaskName());
    				toRemoveFromSprint.add(task);
    			}
    		}
    		sprint.getTasks().removeAll(toRemoveFromSprint);
    	}
	}
    private static void filterRepeatedIds(List<JiraSprintDTO> sprints) {
    	List<JiraTaskDTO> tasks = new ArrayList<JiraTaskDTO>();
    	sprints.stream().forEach( s -> 
    								s.getTasks().forEach( t ->{
    									if(tasks.contains(t)){
    										s.getTasks().remove(t);
    									}else{
    										tasks.add(t);
    									}}));
    	
    }
	private static List<JiraSprintDTO> requestJira(JiraVersionEnum jiraVersion, LocalDate initialDate, LocalDate stopDate, String baseUrl, String token) throws IOException {
		LocalDate endDate = initialDate.plus(twoWeeks).minus(Period.ofDays(1));
		
		List<JiraSprintDTO> sprints = new ArrayList<JiraSprintDTO>();

		while (stopDate.compareTo(endDate) >= 0) {
			String url = String.format(baseUrl, initialDate.format(jiraFormat), endDate.format(jiraFormat));
			JiraSprintDTO sprint = new JiraSprintDTO();
			List<JiraTaskDTO> list = new ArrayList<JiraTaskDTO>();
			
			sprint.setStartDate(initialDate);
			sprint.setFinishDate(endDate);
			sprint.setJiraVersion(jiraVersion);

			Request request = new Request.Builder()
					.url(url)
					.get()
					.header("Authorization", "Basic " + token)
					.build();

			Response response = client.newCall(request).execute();
			if(response.isSuccessful()) {
				JsonObject root = gson.fromJson(response.body().string(), JsonObject.class);

				for (JsonElement el : root.get("issues").getAsJsonArray()) {
					String user = getAssignee(el);
					Integer storyPoint = getStoryPoint(el);
					String taskName = getTaskName(el);
					String taskId = getTaskId(el);

					JiraTaskDTO task = new JiraTaskDTO();
					task.setAssignee(user);
					task.setStoryPoint(jiraStoryPointMap.containsKey(taskId) ? jiraStoryPointMap.get(taskId) : storyPoint);
					task.setTaskName(taskName);
					task.setJiraVersion(jiraVersion);
					task.setTaskId(taskId);

					list.add(task);
				}
			}
			initialDate = endDate.plus(Period.ofDays(1));
			endDate = initialDate.plus(twoWeeks).minus(Period.ofDays(1));
			
			sprint.setTasks(list);
			sprints.add(sprint);
		}
		return sprints;
	}

	private static void parseJira(List<JiraSprintDTO> sprints) throws IOException {
		for(JiraSprintDTO sprint : sprints){
			
	
			Map<String, List<Integer>> usersTaskStoryPoints = new HashMap<String, List<Integer>>();
		
			int totalTasks = sprint.getTasks().size();

			for(JiraTaskDTO task : sprint.getTasks()){
				if (!usersTaskStoryPoints.containsKey(task.getAssignee())) {
					usersTaskStoryPoints.put(task.getAssignee(), new ArrayList<Integer>());
				}
	
				usersTaskStoryPoints.get(task.getAssignee()).add(task.getStoryPoint());
			}
	
			for (Map.Entry<String, List<Integer>> entry : usersTaskStoryPoints.entrySet()) {
				System.out.println(String.format("%s;%s;Fleet;%s;%s;%s;%s;%s;%s",
						docsFormat.format(sprint.getFinishDate()),
						monthFormat.format(sprint.getFinishDate()),
                        entry.getKey(),
						entry.getValue().size(),
						entry.getValue().stream().filter(t -> t == 1).count(),
						entry.getValue().stream().filter(t -> t == 3).count(),
						entry.getValue().stream().filter(t -> t == 5).count(),
						entry.getValue().stream().filter(t -> t == 8).count()));
			}
		}
	}

	private static String getAssignee(JsonElement el) {
		return el.getAsJsonObject().get("fields").getAsJsonObject().get("assignee").getAsJsonObject().get("displayName").getAsString();
	}

	private static Integer getStoryPoint(JsonElement el) {
    	JsonObject jObj = el.getAsJsonObject().get("fields").getAsJsonObject();
    	JsonElement field = null;
    	if(jObj.has("customfield_10033")){
			field = jObj.get("customfield_10033");
		}
		if(jObj.has("customfield_10024")){
			field = jObj.get("customfield_10024");
		}
		return field == null || field.isJsonNull() ? 1 : field.getAsInt();
	}
	
	private static String getTaskName(JsonElement el) {
		return el.getAsJsonObject().get("fields").getAsJsonObject().get("summary").getAsString();
	}
	
	private static String getTaskId(JsonElement el) {
		return el.getAsJsonObject().get("key").getAsString();
	}
	
	private static Map<String,Integer> oldJiraStoryPointsMap(){
		Map<String,Integer> oldMap = new HashMap<String, Integer>();
		
		oldMap.put("WOLAPI-22", 3);
		oldMap.put("WOLAPI-31", 3);
		oldMap.put("WOLAPI-33", 3);
		oldMap.put("WOLAPI-6",	3);
		oldMap.put("WOLAPI-12",	5);
		oldMap.put("WOLAPI-13",	5);
		oldMap.put("WOLAPI-26",	5);
		oldMap.put("WOLAPI-30",	5);
		oldMap.put("WOLAPI-32",	5);
		oldMap.put("WOLAPI-4",	5);
		oldMap.put("WOLAPI-5",	5);
		oldMap.put("WOLAPI-16",	3);
		oldMap.put("WOLAPI-53",	5);
		oldMap.put("WOLAPI-51",	5);
		oldMap.put("WOLAPI-34",	3);
		oldMap.put("WOLAPI-8",	3);
		oldMap.put("WOLAPI-52",	5);
		oldMap.put("WOLAPI-49",	3);
		oldMap.put("WOLAPI-48",	3);
		oldMap.put("WOLAPI-47",	3);
		oldMap.put("WOLAPI-41",	8);
		oldMap.put("WOLAPI-38",	3);
		oldMap.put("WOLAPI-35",	3);
		oldMap.put("WOLAPI-11",	5);
		oldMap.put("WOLAPI-63",	5);
		oldMap.put("WOLAPI-58",	3);
		oldMap.put("WOLAPI-56",	3);
		oldMap.put("WOLAPI-55",	8);
		oldMap.put("WOLAPI-54",	5);
		oldMap.put("WOLAPI-50",	5);
		oldMap.put("WOLAPI-19",	5);
		oldMap.put("WOLAPI-57",	3);
		oldMap.put("WOLAPI-25",	5);
		oldMap.put("WOLAPI-20",	5);
		oldMap.put("WOLAPI-18",	5);
		oldMap.put("WOLAPI-101", 3);
		oldMap.put("WOLAPI-100", 3);
		oldMap.put("WOLAPI-99",	3);
		oldMap.put("WOLAPI-98",	3);
		oldMap.put("WOLAPI-97",	3);
		oldMap.put("WOLAPI-96",	3);
		oldMap.put("WOLAPI-95",	3);
		oldMap.put("WOLAPI-94",	3);
		oldMap.put("WOLAPI-93",	3);
		oldMap.put("WOLAPI-92",	3);
		oldMap.put("WOLAPI-91",	3);
		oldMap.put("WOLAPI-90",	3);
		oldMap.put("WOLAPI-89",	3);
		oldMap.put("WOLAPI-82",	3);
		oldMap.put("WOLAPI-81",	3);
		oldMap.put("WOLAPI-80",	3);
		oldMap.put("WOLAPI-76",	8);
		oldMap.put("WOLAPI-75",	8);
		oldMap.put("WOLAPI-73",	5);
		oldMap.put("WOLAPI-71",	3);
		oldMap.put("WOLAPI-68",	3);
		oldMap.put("WOLAPI-67",	5);
		oldMap.put("WOLAPI-18",	5);
		oldMap.put("WOLAPI-112", 3);
		oldMap.put("WOLAPI-111", 3);
		oldMap.put("WOLAPI-109", 5);
		oldMap.put("WOLAPI-79",	1);
		oldMap.put("WOLAPI-77",	5);
		oldMap.put("WOLAPI-74",	3);
		return oldMap;
	}
	
}
