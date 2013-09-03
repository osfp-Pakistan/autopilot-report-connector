package org.tabtab;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
        
  public class Report
  {
    static List all_data = new ArrayList<String>(), all_fors_finished = new ArrayList<Integer>();
    static int calls = 0, finished_calls = 0;
    static String url = null;
      
    public static void main(String[] args)
    {
      try
      {
        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("rest-api.config")));
        url = properties.getProperty("url", "autopilot-rest-api/");
        
        JSONObject datas = (JSONObject) JSONValue.parse(new FileReader("./querys.config"));
        JSONArray querys = (JSONArray) datas.get("querys");
             
        calls = querys.size();
            
        for (int i = 0; i < querys.size(); i++)
        {
          JSONObject query = (JSONObject) querys.get(i);
          all_data.add("");
          all_fors_finished.add(0);
          ServerRequest(query,i);
        }
      }
      catch(IOException e)
      {
        System.out.println(e.toString());
      }
      catch(ClassCastException e)
      {
        System.out.println(e.toString());
      }
    }
        
    public static void ServerRequest(JSONObject query, int position)
    {
      String[][] args = new String[query.keySet().size()][2];
      int i = 0;
      
      for (Map.Entry<String, Object> element : query.entrySet())
      {
        args[i][0] = element.getKey();
        args[i][1] = (String) element.getValue();
        i++;
      }
      run(args, position);	
    }
    
    private static void run(String[][] args, int position)
    {      
      try
      {
        String query = "";
        for(String[] pair : args) query += pair[0] + ":\"" + pair[1] + "\" AND ";
        query = query.substring(0, query.length() - 5);
        query = URLEncoder.encode(query, "UTF-8").replace("+"," ");
        query = "extq=" + query + "&limit=0&t=timeseries";
        
        Authenticator.setDefault(new MyAuthenticator());
        URL url1 = new URL(url + "search");
        HttpsURLConnection connection1 = (HttpsURLConnection)url1.openConnection();

        connection1.setDoOutput(true);
         
        DataOutputStream wr1 = new DataOutputStream (connection1.getOutputStream ());
        wr1.writeBytes (query);
        wr1.flush ();
        wr1.close ();
	
        JSONObject timeseries = (JSONObject) JSONValue.parse(new InputStreamReader(connection1.getInputStream()));
        JSONArray ts_list = (JSONArray) timeseries.get("data");
        //System.out.println(query);
            
        if (!ts_list.isEmpty())
        {
          all_fors_finished.set(position, ts_list.size());
            
          for(int i = 0; i < ts_list.size(); i++)
          {
            JSONObject ts = (JSONObject) ts_list.get(i);
            URL url2 = new URL(url + ts.get("id").toString());
            HttpsURLConnection connection2 = (HttpsURLConnection)url2.openConnection();
               
            connection2.setDoOutput(true);
        
            DataOutputStream wr2 = new DataOutputStream (connection2.getOutputStream ());
            wr2.writeBytes("values=1&start=" + (new Date().getTime() - 3600000) + "&end=" + (new Date().getTime()));
            wr2.flush ();
            wr2.close ();
                  
            JSONObject result = (JSONObject) JSONValue.parse(new InputStreamReader(connection2.getInputStream()));
            JSONArray values = (JSONArray) result.get("data");
                  
            if (!values.isEmpty()) all_data.set(position, (ts.get("DisplayName") + " => " + ((JSONArray) values.get(values.size() - 1)).get(1)));
            For_Finished(position);
          }
        }
        else
        {
          System.out.println("Empty result !!!");
          Ready();
        }
      }
      catch (Exception e)
      {
        System.out.println(e.toString());
      }
    }
        
    public static void For_Finished(int position)
    {
      all_fors_finished.set(position, ((Integer) all_fors_finished.get(position)) - 1);
      if (((Integer) all_fors_finished.get(position)) == 0) Ready();
    }
        
    public static void Ready()
    {
      finished_calls++;
      if (finished_calls == calls) GiveOut();
    }
        
    public static void GiveOut()
    {
      Calendar now = Calendar.getInstance();
      String now_str = now.get(Calendar.DATE) + "." + (now.get(Calendar.MONTH) + 1) + "." + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
          
      Configuration cfg = new Configuration();
          
      try
      {
        Template template = cfg.getTemplate("report.ftl");
              
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("time", now_str);
        data.put("data", all_data);
 
        Writer out = new OutputStreamWriter(System.out);
        template.process(data, out);
        out.flush();
      }
      catch(TemplateException e)
      {
        System.out.println(e.toString());
      }
      catch(IOException e)
      {
        System.out.println(e.toString());
      } 
    }
  }

  class MyAuthenticator extends Authenticator
  {
    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      String username = "root",password = "";
      try
      {
        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("rest-api.config")));
        username = properties.getProperty("username", "root");
        password = properties.getProperty("password", "");
      }
      catch(IOException e)
      {
        
      }
      return new PasswordAuthentication(username,password.toCharArray());
    }
  }