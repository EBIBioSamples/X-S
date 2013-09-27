package uk.ac.ebi.biosd.xs.service.mtexport;

import java.io.IOException;

public class ControlMessage
{
 public enum Type
 {
  OUTPUT_FINISH,
  OUTPUT_ERROR,
  PROCESS_FINISH,
  PROCESS_ERROR
 }

 private final Type      type;
 private final Object    subject;
 private IOException exception;

 public ControlMessage(Type type, Object subject)
 {
  super();
  this.type = type;
  this.subject = subject;
 }

 public ControlMessage(Type type, Object subject, IOException exception)
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

 public IOException getException()
 {
  return exception;
 }

}
