package uk.ac.ebi.biosd.xs.starter;

import uk.ac.ebi.biosd.xs.init.TaskInfo;
import uk.ac.ebi.biosd.xs.task.TaskManager;
import uk.ac.ebi.biosd.xs.util.ParamPool;

public class TaskStarter
{
 static final String LimitParameter = "limit";
 static final String ThreadsParameter = "threads";
 
 public static boolean start( String taskName, ParamPool prms, String prmPfx, StarterLog log )
 {
  final TaskInfo taskInfo = TaskManager.getDefaultInstance().getTask(taskName);
  

  if(taskInfo == null)
  {
   log.sendErrorMsg("Task '"+taskName+"' was not initialized.");
   return false;
  }

  int limit = -1;

  String limStr = prms.getParameter(prmPfx+LimitParameter);

  if(limStr != null)
  {
   try
   {
    limit = Integer.parseInt(limStr);
   }
   catch(Exception e)
   {
    log.sendErrorMsg("Invalid value of '" + prmPfx+LimitParameter + "' parameter");
    return false;
   }
  }


  String prm = prms.getParameter(prmPfx+ThreadsParameter);

  int nThrs = -1;

  if(prm != null)
  {
   try
   {
    nThrs = Integer.parseInt(prm);
   }
   catch(Exception e)
   {
   }
  }

  if(taskInfo.getTask().isBusy())
  {
   log.sendErrorMsg("Task '"+taskName+"' is busy");
   return false;
  }

  final int fLimit = limit;
  final int fThreads = nThrs;

  new Thread(new Runnable()
  {

   @Override
   public void run()
   {
    try
    {
     taskInfo.getTask().export(fLimit, fThreads);
    }
    catch(Throwable e)
    {
     e.printStackTrace();
    }
   }
  }, "Manual task '"+taskName+"'").start();

  log.sendInfoMsg("Task '"+taskName+"' has been initiated");

  return true;
 }

}
