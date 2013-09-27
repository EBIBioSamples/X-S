package uk.ac.ebi.biosd.xs.service.mtexport;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biosd.xs.service.mtexport.ControlMessage.Type;

public class OutputTask implements Runnable
{
 private final Appendable out;
 private final BlockingQueue<Object> inQueue;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final long limit;
 
 public OutputTask( Appendable out, BlockingQueue<Object> inQueue, BlockingQueue<ControlMessage> controlQueue, long limit )
 {
  this.out = out;
  this.inQueue = inQueue;
  this.controlQueue = controlQueue;
  
  this.limit = limit;
 }
 
 
 @Override
 public void run()
 {
  long count=0;
  
  while( true )
  {
   Object o = null;
   
   while( true )
   {
    try
    {
     o = inQueue.take();
     break;
    }
    catch(InterruptedException e)
    {
    }
   }
   
   String str = o.toString();
   
   if( str == null )
   {
    putIntoQueue(new ControlMessage(Type.OUTPUT_FINISH, this));
    return;
   }
   
   try
   {
    out.append(str);
   }
   catch(Exception e)
   {
    putIntoQueue(new ControlMessage(Type.OUTPUT_ERROR, this));
    return;
   }
   
   count++;
   
   if( limit > 0 && count >= limit )
   {
    putIntoQueue(new ControlMessage(Type.OUTPUT_FINISH, this));
    return;
   }

   
  }
  
 }

 void  putIntoQueue( ControlMessage o )
 {

  while(true)
  {
   try
   {
    controlQueue.put(o);
    return;
   }
   catch(InterruptedException e)
   {
   }
  }

 }
}
