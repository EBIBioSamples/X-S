package uk.ac.ebi.biosd.xs.log;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.xs.test.StringUtils;

public class StreamLogger implements TimeLogger
{
 private final PrintStream out = System.out;
 private long initTime;
 
 private final Map<String, TagStat> stat=new HashMap<>();

 public StreamLogger()
 {
  init();
 }
 
 @Override
 public boolean init()
 {
  initTime = System.currentTimeMillis();
  
  return true;
 }

 
 @Override
 public boolean entry(String msg, String... tags)
 {
  
  if( tags.length == 0 )
  {
   setEntry(msg,null);
   return true;
  }
  
  for( String t : tags )
   setEntry(msg,t);
  
  return true;
 }

 private void setEntry(String msg, String tag)
 {
  TagStat ts = stat.get(tag);
  
  if( ts == null )
   stat.put(tag, ts=new TagStat());
  
  if( tag == null )
   tag = "";
  else
   tag = tag+":";
  
  long tm = System.currentTimeMillis();
  
  if( ts.getEntry() != 0 )
   out.println("Double entry!");
  
  out.println(tag+" (ENTRY) "+msg+" Time: "+StringUtils.millisToString(tm-initTime)+" Delta: "+StringUtils.millisToString(tm-ts.getCheckpoint()) );
  
  ts.setEntry(tm);
  ts.setCheckpoint(tm);
  ts.setNEntries( ts.getNEntries() + 1 );
 }
 
 private void setCheckpoint(String msg, String tag)
 {
  TagStat ts = stat.get(tag);
  
  if( ts == null )
   stat.put(tag, ts=new TagStat());
  
  if( tag == null )
   tag = "";
  else
   tag = tag+":";
  
  long tm = System.currentTimeMillis();

  
  out.println(tag+" (ChP) "+msg+" Time: "+StringUtils.millisToString(tm-initTime)+" Delta: "+StringUtils.millisToString(tm-ts.getCheckpoint()) );
  
  ts.setCheckpoint(tm);
  
 }

 @Override
 public boolean checkpoint(String msg, String... tags)
 {
  if( tags.length == 0 )
  {
   setCheckpoint(msg,null);
   return true;
  }
  
  for( String t : tags )
   setCheckpoint(msg,t);
  
  return true;
 }

 @Override
 public boolean exit(String msg, String... tags)
 {
  if( tags.length == 0 )
  {
   setExit(msg,null);
   return true;
  }
  
  for( String t : tags )
   setExit(msg,t);
  
  return true;
 }
 
 private void setExit(String msg, String tag)
 {
  TagStat ts = stat.get(tag);
  
  if( ts == null )
   stat.put(tag, ts=new TagStat());
  
  if( tag == null )
   tag = "";
  else
   tag = tag+":";
  
  long tm = System.currentTimeMillis();
  
  if( ts.getEntry() == 0 )
   out.println("Double exit!");
  
  out.println(tag+" (EXIT) "+msg+" Time: "+StringUtils.millisToString(tm-initTime)+" Delta: "+StringUtils.millisToString(tm-ts.getCheckpoint())+" Block: "+StringUtils.millisToString(tm-ts.getEntry()) );
  
  ts.setSummary(ts.getSummary()+ ( tm-ts.getEntry() ) );

  ts.setEntry(0);
  ts.setCheckpoint(tm);
  
 }

 @Override
 public boolean summary()
 {
  out.println("Summary:");
  
  TagStat ts = stat.get(null);
  
  if( ts != null )
   out.println("[Default]: "+StringUtils.millisToString(ts.getSummary()));
  
  for( Map.Entry<String, TagStat> me : stat.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   out.println(me.getKey()+": "+StringUtils.millisToString(me.getValue().getSummary()));

  }
  
  return true;
 }

}
