package uk.ac.ebi.biosd.xs.init;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.task.ExportTask;

public class TaskInfo extends TimerTask
{
 private final Logger log = LoggerFactory.getLogger(TaskInfo.class);
 
 private ExportTask task;
 private long timerDelay;
 private Timer timer;

 
 public ExportTask getTask()
 {
  return task;
 }

 public void setTask(ExportTask task)
 {
  this.task = task;
 }

 public long getTimerDelay()
 {
  return timerDelay;
 }

 public void setTimerDelay(long timerDelay)
 {
  this.timerDelay = timerDelay;
 }


 @Override
 public void run()
 {
  log.info("Starting scheduled task: " + task.getName());

  new Thread(new Runnable()
  {

   @Override
   public void run()
   {
    try
    {
     task.export(task.getRequestConfig().getLimit(-1), task.getRequestConfig().getThreads(-1));
    }
    catch(Throwable e)
    {
     log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
    }

    log.info("Finishing scheduled task: " + task.getName());

   }
  }, "Task '"+task.getName()+"' export");
 }

 public Timer getTimer()
 {
  return timer;
 }

 public void setTimer(Timer timer)
 {
  this.timer = timer;
 }

}
