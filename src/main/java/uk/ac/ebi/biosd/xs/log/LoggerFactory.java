package uk.ac.ebi.biosd.xs.log;

public class LoggerFactory
{
 private static TimeLogger logger = new NullLogger();

 public static TimeLogger getLogger()
 {
  return logger;
 }

 public static boolean setLogger(TimeLogger logger)
 {
  LoggerFactory.logger = logger;
  
  return true;
 }
 
}
