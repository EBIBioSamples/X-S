package uk.ac.ebi.biosd.xs.util;

import java.util.LinkedList;
import java.util.Queue;

public class RangeManager
{
 public static class Range
 {
  long min;
  long max;
  boolean locked;
  
  public Range(long min, long max)
  {
   this.min = min;
   this.max = max;
  }

  public long getMin()
  {
   return min;
  }

  public void setMin(long min)
  {
   this.min = min;
  }

  public long getMax()
  {
   return max;
  }

  public void setMax(long max)
  {
   this.max = max;
  }

  public boolean isLocked()
  {
   return locked;
  }

  public void setLocked(boolean locked)
  {
   this.locked = locked;
  }
  
  @Override
  public String toString()
  {
   return "["+min+","+max+"]";
  }
 }
 
 


 Queue<Range> ranges;
 RangeManager subMngr;
 int requested=0;
 int nWays;

 public RangeManager( long min, long max, int ways)
 {
  ranges = new LinkedList<>();
  nWays=ways;
  
  fillQueue(min, max);
  
 }
 
 private void fillQueue( long min, long max )
 {
  long step = max/nWays-min/nWays;
  long start = min;
  
  for(int i=1; i < nWays; i++ )
  {
   Range r = new Range(start, start+step);
   start=start+step+1;
   
   ranges.add(r);
   
   System.out.println("("+Thread.currentThread().getName()+") Added to the range queue "+r+", requested: "+requested+" Queue:"+ranges.size());

  }
  
  Range r = new Range(start, max);

  System.out.println("("+Thread.currentThread().getName()+") Added to the range queue "+r+", requested: "+requested+" Queue:"+ranges.size());
  
  ranges.add( r );
 }

 public synchronized Range getRange()
 {

  
  if(ranges.size() == 0)
  {
   if(requested == 0)
   {
    System.out.println("("+Thread.currentThread().getName()+") No more ranges, requested: "+requested+" Queue:"+ranges.size());

    return null;
   }
   
   System.out.println("("+Thread.currentThread().getName()+") Waiting for free ranges");

   
   while(ranges.size() == 0)
   {
    
    try
    {
     wait();
    }
    catch(InterruptedException e)
    {
    }
    
    if(requested == 0)
     return null;

   }
   
  }
  
  requested++;
  
  Range r = ranges.poll();
  
  System.out.println("("+Thread.currentThread().getName()+") Getting range "+r+", requested: "+requested+" Queue:"+ranges.size());

  return r;
  
 }
 
 public synchronized Range returnAndGetRange( Range r )
 {
  if( r == null || r.getMin() > r.getMax() )
  {
   requested--;
   
   r = getRange();
   
   if( r == null )
    notifyAll();
   
   return r;
  }

  if( ranges.size() != 0 || r.getMax()-r.getMin() < nWays )
  {
   System.out.println("("+Thread.currentThread().getName()+") Continue range "+r+", requested: "+requested+" Queue:"+ranges.size());

   return r;
  }
  
  
  System.out.println("("+Thread.currentThread().getName()+") Splitting range "+r+", requested: "+requested+" Queue:"+ranges.size());

  
  fillQueue(r.getMin(), r.getMax());
  
  r = ranges.poll();
  
  notifyAll();
  
  return r;
 }

 public synchronized void shutdown()
 {
  ranges.clear();
  requested=0;
  notifyAll();
 }
 
}
