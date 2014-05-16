package uk.ac.ebi.biosd.xs.task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.biosd.xs.init.TaskInfo;

public class TaskManager
{
 private static TaskManager defaultInstance;
 
 private final Map<String, TaskInfo > tasks = new HashMap<String, TaskInfo>();

 public static TaskManager getDefaultInstance()
 {
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

}
