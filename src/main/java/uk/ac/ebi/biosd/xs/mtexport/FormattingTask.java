package uk.ac.ebi.biosd.xs.mtexport;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class FormattingTask
{
 private final XMLFormatter          formatter;
 private final BlockingQueue<Object> groupQueue;
 private final BlockingQueue<Object> sampleQueue;
 
 private final boolean publicOnly;
 private final boolean groupedSamplesOnly;
 private final boolean sourcesByAcc;
 private final boolean sourcesByName;
 
 public FormattingTask(XMLFormatter formatter, boolean grpSmp, boolean collByAcc, boolean collByName, boolean pub, BlockingQueue<Object> groupQueue, BlockingQueue<Object> sampleQueue)
 {
  super();
  this.formatter = formatter;
  this.groupQueue = groupQueue;
  this.sampleQueue = sampleQueue;
  
  groupedSamplesOnly=grpSmp;
  sourcesByAcc=collByAcc;
  sourcesByName = collByName;
  publicOnly = pub;
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

 public boolean isPublicOnly()
 {
  return publicOnly;
 }

}
