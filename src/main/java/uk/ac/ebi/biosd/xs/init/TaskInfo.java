package uk.ac.ebi.biosd.xs.init;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.task.ExportTask2;

public class TaskInfo extends TimerTask
{
 private final Logger log = LoggerFactory.getLogger(TaskInfo.class);
 
 private ExportTask2 task;
 private long timerDelay=-1;
 private Timer timer;

 
 public ExportTask2 getTask()
 {
  return task;
 }

 public void setTask(ExportTask2 task)
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
     task.export(task.getTaskConfig().getLimit(-1), task.getTaskConfig().getThreads(-1));
    }
    catch(Throwable e)
    {
     log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
     e.printStackTrace();
    }

    log.info("Finishing scheduled task: " + task.getName());

   }
  }, "Task '"+task.getName()+"' export").start();
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
