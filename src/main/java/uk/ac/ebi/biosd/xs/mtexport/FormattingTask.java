package uk.ac.ebi.biosd.xs.mtexport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class FormattingTask
{
 private final XMLFormatter          formatter;
 private final BlockingQueue<Object> groupQueue;
 private final BlockingQueue<Object> sampleQueue;
 
 private final boolean groupedSamplesOnly;
 private final boolean sourcesByAcc;
 private final boolean sourcesByName;
 private final AtomicLong maxCount;
 
 public FormattingTask(XMLFormatter formatter, boolean grpSmp, boolean collByAcc, boolean collByName, BlockingQueue<Object> groupQueue, BlockingQueue<Object> sampleQueue, long limit)
 {
  super();
  this.formatter = formatter;
  this.groupQueue = groupQueue;
  this.sampleQueue = sampleQueue;
  
  groupedSamplesOnly=grpSmp;
  sourcesByAcc=collByAcc;
  sourcesByName = collByName;
  
  if( limit > 0 )
   maxCount = new AtomicLong(limit);
  else
   maxCount = null;
 }

 
 public boolean confirmOutput()
 {
  if( maxCount == null )
   return true;
  
  long cnt = maxCount.decrementAndGet();
  
  return cnt >= 0;
 }
 
 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 public BlockingQueue<Object> getGroupQueue()
 {
  return groupQueue;
 }

 public BlockingQueue<Object> getSampleQueue()
 {
  return sampleQueue;
 }

 public boolean isGroupedSamplesOnly()
 {
  return groupedSamplesOnly;
 }

 public boolean isSourcesByAcc()
 {
  return sourcesByAcc;
 }

 public boolean isSourcesByName()
 {
  return sourcesByName;
 }


}
