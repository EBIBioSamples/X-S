package uk.ac.ebi.biosd.xs.service;


public class Counter extends Number
{
 private static final long serialVersionUID = 698465133888087160L;

 private int count;
 /**
  * 
  */
 public Counter()
 {
  count  = 0;
 }

 public Counter( int init )
 {
  count  = init;
 }

 public int inc()
 {
  return ++count;
 }

 public int add( int v )
 {
  return count+=v;
 }

 
 public int dec()
 {
  return --count;
 }
 
 @Override
 public int intValue()
 {
  return count; 
 }
 
 @Override
 public String toString()
 {
  return String.valueOf(count); 
 }

 /* (non-Javadoc)
  * @see java.lang.Number#doubleValue()
  */
 @Override
 public double doubleValue()
 {
  // TODO Auto-generated method stub
  return count;
 }

 /* (non-Javadoc)
  * @see java.lang.Number#floatValue()
  */
 @Override
 public float floatValue()
 {
  // TODO Auto-generated method stub
  return count;
 }

 /* (non-Javadoc)
  * @see java.lang.Number#longValue()
  */
 @Override
 public long longValue()
 {
  // TODO Auto-generated method stub
  return count;
 }

}
