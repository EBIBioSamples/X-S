package uk.ac.ebi.biosd.xs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SliceManager
{
 private static class SliceTopic
 {
  int offset=0;
  Stack<Slice> returns = new Stack<Slice>();
 }
 
 private final Map<String, SliceTopic> topicMap = new HashMap<>();
 
 private int limit=5;
 
 public SliceManager()
 {
 }
 
 public SliceManager( int sz )
 {
  limit = sz;
 }

 
 public Slice getSlice(String topic)
 {
  SliceTopic tpc = null;
  
  synchronized(topicMap)
  {
   tpc = topicMap.get(topic);
   
   if( tpc == null )
    topicMap.put(topic, tpc = new SliceTopic() );
  }
  

  synchronized(tpc)
  {
   if( tpc.returns.size() > 0 )
    return tpc.returns.pop();
   
   Slice newsl = new Slice(tpc.offset, limit);
   tpc.offset += limit;
   return newsl;
  }
  
 }

 public int getSliceSize()
 {
  return limit;
 }
 
 public void returnSlice( String topic, Slice s )
 {
  SliceTopic tpc = null;
  
  synchronized(topicMap)
  {
   tpc = topicMap.get(topic);
   
   if( tpc == null )
    topicMap.put(topic, tpc = new SliceTopic() );
  }
  

  synchronized(tpc)
  {
   tpc.returns.push(s);
  }
 }
}
