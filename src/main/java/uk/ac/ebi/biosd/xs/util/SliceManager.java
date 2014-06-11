package uk.ac.ebi.biosd.xs.util;

public class SliceManager
{
 private final int limit=5;
 private int start=0;
 
 public synchronized Slice getSlice()
 {
  Slice sl = new Slice(start, limit);
  
  start+=limit;
  
  return sl;
 }
}
