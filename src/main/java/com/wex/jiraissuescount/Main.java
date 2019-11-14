package com.wex.jiraissuescount;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class Main {

	static DateTimeFormatter jiraFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd");
	static DateTimeFormatter docsFormat = DateTimeFormatter.ofPattern("MM/dd/YYYY");
	static DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM");
	static Gson gson = new Gson();
	static OkHttpClient client = new OkHttpClient();
	static Period twoWeeks = Period.ofWeeks(2);

    public static void main(String[] args) throws IOException {
        //Old Jira
		requestJira(LocalDate.of(2018, 12, 28), LocalDate.of(2019, 5, 31),
				"https://jira.devtools.wexinc.com/rest/api/2/search?fields=assignee,customfield_10033&jql=project = WOLAPI AND assignee in (W000848, esilva, asouza, fneto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
				"");
		//Global Jira
		requestJira(LocalDate.of(2019, 5, 31),LocalDate.now(),
				"https://wexinc.atlassian.net/rest/api/2/search/?fields=assignee,customfield_10024&jql=assignee in (douglas.lima, elielson.silva, pedro.lourenco, Felix.Neto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
				"");
		System.out.println("end");
	}

	private static void requestJira(LocalDate initialDate, LocalDate stopDate, String baseUrl, String token) throws IOException {
		LocalDate endDate = initialDate.plus(twoWeeks);

		while (stopDate.compareTo(endDate) >= 0) {
			String url = String.format(baseUrl, initialDate.format(jiraFormat), endDate.format(jiraFormat));

			Request request = new Request.Builder()
					.url(url)
					.get()
					.header("Authorization", "Basic " + token)
					.build();

			Map<String, Integer> usersQtdTaskMap = new HashMap<String, Integer>();
			Map<String, Integer> usersStoryPointMap = new HashMap<String, Integer>();
			Response response = client.newCall(request).execute();
			JsonObject root = gson.fromJson(response.body().string(), JsonObject.class);
			int totalTasks = root.get("issues").getAsJsonArray().size();

			for (JsonElement el : root.get("issues").getAsJsonArray()) {
				String user = getAssignee(el);
				Integer storyPoint = getStoryPoint(el);

				if (!usersQtdTaskMap.containsKey(user)) {
					usersQtdTaskMap.put(user, 0);
					usersStoryPointMap.put(user, 0);
				}

				usersQtdTaskMap.put(user, usersQtdTaskMap.get(user) + storyPoint);
				usersStoryPointMap.put(user, usersStoryPointMap.get(user) + storyPoint);
			}

			System.out.println();
			System.out.println(String.format("%s - %s = %s tasks ",
								jiraFormat.format(initialDate),
								jiraFormat.format(endDate),
								totalTasks));
			for (Map.Entry<String,Integer> entry : usersQtdTaskMap.entrySet()) {
				System.out.println(String.format("%s %s Fleet %s %s %s",
						docsFormat.format(endDate),
						monthFormat.format(endDate),
                        entry.getKey(),
						entry.getValue(),
						usersStoryPointMap.get(entry.getKey())));
			}
			initialDate = endDate;
			endDate = initialDate.plus(twoWeeks);
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
}
