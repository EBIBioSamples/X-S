package uk.ac.ebi.biosd.xs.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class RangeManager
{
 
 private static class TopicQueue
 {
  Queue<Range> ranges = new LinkedList<>();
  int requested=0;
 }


 private final Map<String, TopicQueue> topicMap = new HashMap<String, RangeManager.TopicQueue>();
 
 private final int nWays;
 private final long initMin;
 private final long initMax;
 

 public RangeManager( long min, long max, int ways)
 {
  nWays=ways;
  
  initMin = min;
  initMax = max;
 }
 
 private void fillQueue( TopicQueue tc, long min, long max )
 {
  int nWaysx2 =nWays*2;

  int smx = Long.signum(max);
  
  if(  tc.ranges.size() > nWaysx2 || ( ( smx != 0 && Long.signum(min) == smx || max <= nWaysx2 && -min <= nWaysx2 ) && max - min <= nWaysx2 ) )
  {
   tc.ranges.add( new Range(min, max) );
   return;
  }
  
  
  long step = max/nWays-min/nWays;
  long start = min;
  
  for(int i=1; i < nWays; i++ )
  {
   Range r = new Range(start, start+step);
   start=start+step+1;
   
   tc.ranges.add(r);
   
//   System.out.println("("+Thread.currentThread().getName()+") Added to the range queue "+r+", requested: "+requested+" Queue:"+ranges.size());

  }
  
  Range r = new Range(start, max);

//  System.out.println("("+Thread.currentThread().getName()+") Added to the range queue "+r+", requested: "+requested+" Queue:"+ranges.size());
  
  tc.ranges.add( r );
 }

 public Range getRange( String tpc )
 {

  TopicQueue tc = null;
  
  synchronized(this)
  {
   tc = topicMap.get(tpc);
   
   if( tc == null )
   {
    topicMap.put( tpc, tc=new TopicQueue() );
    fillQueue(tc, initMin, initMax);
   }
   
  }
    
  
  synchronized(tc)
  {
   return getRange(tc);
  }

 }
 
 private Range getRange( TopicQueue tc )
 {

  while(tc.ranges.size() == 0)
  {
   if(tc.requested == 0)
    return null;

   try
   {
    tc.wait();
   }
   catch(InterruptedException e)
   {
   }

  }

  tc.requested++;

  Range r = tc.ranges.poll();

  //  System.out.println("("+Thread.currentThread().getName()+") Getting range "+r+", requested: "+requested+" Queue:"+ranges.size());

  return r;
 }
 
 public void returnRange(String tpc, Range r)
 {
  TopicQueue tc = null;
  
  synchronized(this)
  {
   tc = topicMap.get(tpc);
   
   if( tc == null )
    return;   
  }

  synchronized(tc)
  {
   tc.requested--;

   if( r != null && r.getMin() <= r.getMax() )
    fillQueue(tc, r.getMin(), r.getMax());
   
   tc.notifyAll();
  }

  
 }
 
 
 public synchronized Range returnAndGetRange(String tpc, Range r)
 {
  
  TopicQueue tc = null;
  
  synchronized(this)
  {
   tc = topicMap.get(tpc);
   
   if( tc == null )
    return null;   
  }
  
  
  synchronized(tc)
  {
   if( r == null || r.getMin() > r.getMax() )
   {
    tc.requested--;
    
    r = getRange( tc );
    
    if( r == null )
     notifyAll();
    
    return r;
   }

   if( tc.ranges.size() != 0 || r.getMax()-r.getMin() < nWays )
   {
//    System.out.println("("+Thread.currentThread().getName()+") Continue range "+r+", requested: "+requested+" Queue:"+ranges.size());

    return r;
   }
   
   tc.requested--;

   fillQueue(tc, r.getMin(), r.getMax());
   
   r = tc.ranges.poll();
   
   notifyAll();
   
   return r;
   
  }

 }

 public synchronized void shutdown()
 {
  for( TopicQueue tc : topicMap.values() )
  {
   tc.ranges.clear();
   tc.requested=0;
   tc.notifyAll();
  }

 }


 
}
