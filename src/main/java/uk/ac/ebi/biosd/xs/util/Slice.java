package uk.ac.ebi.biosd.xs.util;

public class Slice
{
 public Slice(int start, int limit)
 {
  super();
  this.start = start;
  this.limit = limit;
 }

 public int start;
 public int limit;

 public int getStart()
 {
  return start;
 }

 public void setStart(int start)
 {
  this.start = start;
 }

 public int getLimit()
 {
  return limit;
 }

 public void setLimit(int limit)
 {
  this.limit = limit;
 }
 
 @Override
 public String toString()
 {
  return "Start: "+start+" Limit: "+limit;
 }
}
