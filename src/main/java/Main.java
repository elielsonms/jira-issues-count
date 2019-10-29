import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Main {

	public static void main(String[] args) throws IOException {
		String token = "";
		String assignees = "";
		String project = "";
		String component = "";
		
		if(args.length > 0){
			token = args[0];
			assignees = args[1];//douglas.lima, currentUser(), pedro.lourenco, Felix.Neto
			project = args[2];//WMD
			component = args[3];//APIs
		}else{
			token = "";
			assignees = "douglas.lima, currentUser(), pedro.lourenco, Felix.Neto";
			project = "WMD";
			component = "APIs";
		}
		
		OkHttpClient client = new OkHttpClient();
		Period twoWeeks = Period.ofWeeks(2); 
		
		
		LocalDate initialDate = LocalDate.of(2019, 5, 31);
		LocalDate endDate = initialDate.plus(twoWeeks);
		LocalDate now = LocalDate.now();
		String baseUrl = "https://wexinc.atlassian.net/rest/api/2/search/?fields=assignee&jql=project = "+project+" AND component = "+component+" AND assignee in ("+assignees+") AND status changed TO 'DONE' during(%s,%s)";
		DateTimeFormatter jiraFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd");
		DateTimeFormatter docsFormat = DateTimeFormatter.ofPattern("MM/dd/YYYY");
		DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM");
		
		Gson gson = new Gson();
		
		while(now.compareTo(endDate) >= 0){
			String url = String.format(baseUrl, initialDate.format(jiraFormat), endDate.format(jiraFormat));
			
			System.out.println(url);

			Request request = new Request.Builder()
		      .url	(url)
		      .get()
		      .header("Authorization", "Basic "+token)
		      .build();
			
			Map<String,Integer> usersMap = new HashMap<String,Integer>();
			int totalTasks = 0;
		  try (Response response = client.newCall(request).execute()) {
			  JsonObject root = gson.fromJson(response.body().string(), JsonObject.class);
			  totalTasks = root.get("issues").getAsJsonArray().size();
			  
			  for(JsonElement el : root.get("issues").getAsJsonArray()){
				  String user = el.getAsJsonObject().get("fields").getAsJsonObject().get("assignee").getAsJsonObject().get("displayName").getAsString();
				  if(!usersMap.containsKey(user)){
					  usersMap.put(user,0);
				  }
				  usersMap.put(user,usersMap.get(user)+1);
			  }
		  }catch (Exception e) {
			e.printStackTrace();
		  }	
		  System.out.println();
		  System.out.println(String.format("%s - %s = %s tasks ",jiraFormat.format(initialDate), jiraFormat.format(endDate), totalTasks ));
		  for(String user : usersMap.keySet()){
			  System.out.println(String.format("%s %s Fleet %s %s",
					  docsFormat.format(endDate),
					  monthFormat.format(endDate),
					  user,
					  usersMap.get(user)));
		  }
		  initialDate = endDate;
		  endDate = initialDate.plus(twoWeeks);
		}
	}
}
