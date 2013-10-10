package uk.ac.ebi.biosd.xs.mtexport;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class FormattingTask
{
 private final XMLFormatter          formatter;
 private final BlockingQueue<Object> groupQueue;
 private final BlockingQueue<Object> sampleQueue;
 
 public FormattingTask(XMLFormatter formatter, BlockingQueue<Object> groupQueue, BlockingQueue<Object> sampleQueue)
 {
  super();
  this.formatter = formatter;
  this.groupQueue = groupQueue;
  this.sampleQueue = sampleQueue;
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

}
