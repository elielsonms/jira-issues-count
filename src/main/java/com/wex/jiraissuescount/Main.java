package com.wex.jiraissuescount;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
				"https://jira.devtools.wexinc.com/rest/api/2/search?fields=assignee&jql=assignee in (W000848, esilva, asouza, fneto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
				"");
		//Global Jira
		requestJira(LocalDate.of(2019, 5, 31),LocalDate.now(),
				"https://wexinc.atlassian.net/rest/api/2/search/?fields=assignee&jql=assignee in (douglas.lima, elielson.silva, pedro.lourenco, Felix.Neto) AND status changed TO (RESOLVED,DONE) during(%s,%s)",
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

			Map<String, Integer> usersMap = new HashMap<String, Integer>();
			Response response = client.newCall(request).execute();
			JsonObject root = gson.fromJson(response.body().string(), JsonObject.class);
			int totalTasks = root.get("issues").getAsJsonArray().size();

			for (JsonElement el : root.get("issues").getAsJsonArray()) {
				String user = el.getAsJsonObject().get("fields").getAsJsonObject().get("assignee").getAsJsonObject().get("displayName").getAsString();
				if (!usersMap.containsKey(user)) {
					usersMap.put(user, 0);
				}
				usersMap.put(user, usersMap.get(user) + 1);
			}

			System.out.println();
			System.out.println(String.format("%s - %s = %s tasks ", jiraFormat.format(initialDate), jiraFormat.format(endDate), totalTasks));
			for (Map.Entry<String,Integer> entry : usersMap.entrySet()) {
				System.out.println(String.format("%s %s Fleet %s %s",
						docsFormat.format(endDate),
						monthFormat.format(endDate),
                        entry.getKey(),
						entry.getValue()));
			}
			initialDate = endDate;
			endDate = initialDate.plus(twoWeeks);
		}
	}
}
