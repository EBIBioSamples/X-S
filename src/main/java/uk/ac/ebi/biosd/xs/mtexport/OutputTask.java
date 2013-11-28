package uk.ac.ebi.biosd.xs.mtexport;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;

public class OutputTask implements Runnable
{
 private final Appendable out;
 private final BlockingQueue<Object> inQueue;
 private final BlockingQueue<ControlMessage> controlQueue;
 
 public OutputTask( Appendable out, BlockingQueue<Object> inQueue, BlockingQueue<ControlMessage> controlQueue)
 {
  this.out = out;
  this.inQueue = inQueue;
  this.controlQueue = controlQueue;
 }
 
 
 @Override
 public void run()
 {
  
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
    e.printStackTrace();
    
    putIntoQueue(new ControlMessage(Type.OUTPUT_ERROR, this,e));
    return;
   }
   
  }
  
 }

 public BlockingQueue<Object> getIncomingQueue()
 {
  return inQueue;
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
