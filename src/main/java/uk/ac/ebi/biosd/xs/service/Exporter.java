package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

public interface Exporter
{
 void export( long since, Appendable out, long limit) throws IOException;
 void export( long since, Appendable out, long limit, Double grMul, Double smpMul) throws IOException;
}
