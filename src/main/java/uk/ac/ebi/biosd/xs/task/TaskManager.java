package uk.ac.ebi.biosd.xs.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import uk.ac.ebi.biosd.xs.init.TaskInfo;

public class TaskManager
{
 static final int minInDay = 60*24;
 
 private static TaskManager defaultInstance;
 
 private final Map<String, TaskInfo > tasks = new HashMap<String, TaskInfo>();
 
 private final List<TaskInfo> execQueue = new ArrayList<>(20); 
 
 private final Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

 public static TaskManager getDefaultInstance()
 {
  if( defaultInstance == null )
   defaultInstance = new TaskManager();
  
  return defaultInstance;
 }

 public static void setDefaultInstance(TaskManager defaultInstance)
 {
  TaskManager.defaultInstance = defaultInstance;
 }

 public void addTask( TaskInfo ti )
 {
  tasks.put(ti.getTask().getName(), ti);
 }
 
 public TaskInfo getTask( String tnm )
 {
  return tasks.get(tnm);
 }
 
 public Collection< TaskInfo > getTasks()
 {
  return tasks.values();
 }

 public void run()
 {
  calendar.setTimeInMillis(System.currentTimeMillis());
  
  int minOfDay = calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE);
  
  for( TaskInfo ti : tasks.values() )
  {
   if( ti.isEnqueued() )
    continue;
   
  
  }
 }
 
}
