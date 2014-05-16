package uk.ac.ebi.biosd.xs.mtexport;



public class ControlMessage
{
 public enum Type
 {
  OUTPUT_FINISH,
  OUTPUT_ERROR,
  PROCESS_FINISH,
  PROCESS_ERROR,
  TERMINATE
 }

 private final Type      type;
 private final Object    subject;
 private Throwable exception;

 public ControlMessage(Type type, Object subject)
 {
  super();
  this.type = type;
  this.subject = subject;
 }

 public ControlMessage(Type type, Object subject, Throwable exception)
 {
  super();
  this.type = type;
  this.subject = subject;
  this.exception = exception;
 }

 public Type getType()
 {
  return type;
 }

 public Object getSubject()
 {
  return subject;
 }

 public Throwable getException()
 {
  return exception;
 }

}
