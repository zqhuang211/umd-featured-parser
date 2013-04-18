package edu.umd.clip.math;

//package edu.umd.clip.maxent;
//
//import java.io.FileOutputStream;
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.logging.Logger;
//import edu.umd.clip.jobs.Job;
//import edu.umd.clip.jobs.JobGroup;
//import edu.umd.clip.jobs.JobManager;
////import edu.umd.clip.ling.WordTagSequence;
////import edu.umd.clip.util.BiSet;
////import edu.umd.clip.util.Numberer;
////import edu.umd.clip.util.TopFeatureWriter;
////import edu.umd.clip.util.UniCounter;
//
///**
// * @author Vladimir Eidelman
// *
// */
//public class runLBFGS_MaxEnt {
//
//    static int ndim, msave = 7;
//    static int nwork = ndim * (2 * msave + 1) + 2 * msave;
//    private double[] mod_exp;
//    private double[] emp_exp;
//    private static int sentCount;
//    private static int tagSetSize;
//    //private double bar_div;
//    //private boolean gaussian_prior = true;
//    private int offSet;
//    private static Logger logger = Logger.getLogger(runLBFGS_MaxEnt.class.getName());
//    //private boolean useOld = false;
//    private static String topFeatFile;
//
//    public runLBFGS_MaxEnt(String topFeatFileName) {
//        this.topFeatFile = topFeatFileName;
//    }
//
//    public double[] run(Collection<WordTagSequence> collection, double useRegWeight) {
//        FeatureExtractor_MaxEnt ff = new FeatureExtractor_MaxEnt();
//        emp_exp = ff.get_FeatureExp();
//
//        /*	 System.out.print("EMP: ");
//        for (int q=0;q<emp_exp.length;q++)
//        System.out.print( "  "+emp_exp[q] );
//        System.out.println();
//         */
//        offSet = ff.getOffset();
//        tagSetSize = ff.getTagSetSize();
//        System.out.println("off " + offSet);
//        sentCount = ff.sentCount;
//        ndim = ff.totalFeatNum;
//        System.out.println("lambda size: " + ndim + "  " + offSet);
//
//        //  display all features in the model
//        if (false) {
//            for (int bb = 0; bb < FeatureList.uni_features.total(); bb++) {
//                System.out.println(bb + " " + FeatureList.uni_features.object((bb)));
//            }
//        }
//
//        //dimension of feature/weight vector
//        double x[], g[], diag[], w[];
//        x = new double[ndim];	//solution weight vector (lambdas)
//        g = new double[ndim];	//gradient vector
//        diag = new double[ndim];
//        w = new double[nwork];
//
//        double f, eps, xtol, gtol, stpmin, stpmax;
//        int iprint[], iflag[] = new int[1], icall, n, m, mp, lp, j;
//        iprint = new int[2];
//        boolean diagco;
//
//        n = ndim;
//
//        m = 5;
//        iprint[ 1 - 1] = 1;
//        iprint[ 2 - 1] = 0;
//        diagco = false;
//        eps = 1.0e-5;
//        xtol = 1.0e-16;
//        icall = 0;
//        iflag[0] = 0;
//
//        //x : Initial solution (lambdas) vector estimate
//        for (j = 0; j < ndim; j++) {
//            x[j] = 0;
//        }
//
//
//        do {
//            logger.info("ITERATION " + icall);
//
//            f = 0; // initial objective value
//
//            //calculate objective
//            //given a sentence, calculate feature counts for it and compute exp(f(x,y) \dot lambda)
//            double model_objective;
//            //double model_z = getModelZ(collection,x);
//            //	 logger.info("Calculating partition: ");
//            model_objective = getModelZ3(collection, x);
//
//
//            //	 logger.info("Back from partition: " );
//
//            double norm = 0;
//            final double mean = 0.0;
//            final double sigsq = 1.0;
//
//            double diff = 0;
//            //calculate gradient
//            for (j = 0; j < ndim; j++) {
//                final double param = (x[j] - mean);
//                norm += param * param;
//                g[j] = mod_exp[j] - (emp_exp[j]);
//                diff += g[j];
//
//                g[j] += useRegWeight * param / (1.0 * sigsq);
//
//            }
//
//            double reg = norm / (2.0 * sigsq);
//            System.out.println("LL" + model_objective);
//
//            f = (-1 * model_objective);
//            f += useRegWeight * reg;
//
//            logger.info("Regularization term: " + reg + " Diff: " + diff);
//
//
//
//            if (false) {
//                System.out.println("Calculating gradient: ");
//                for (int q = 0; q < n; q++) {
//                    System.out.print("  " + g[q]);
//                }
//                System.out.println();
//            }
//
//            if (false) {
//                System.out.print("Lambdas: ");
//                for (int q = 0; q < n; q++) {
//                    System.out.print("  " + x[q]);
//                }
//                System.out.println();
//            }
//
//            logger.info("Calling LBFGS");
//
//            try {
//                System.out.println("ndim" + n + " " + m);
//                LBFGS.lbfgs(n, m, x, f, g, diagco, diag, iprint, eps, xtol, iflag);
//            } catch (LBFGS.ExceptionWithIflag e) {
//                System.err.println("runLBFGS: lbfgs failed.\n" + e);
//                return null;
//            }
//
//            //	iflag[0] =2;
//            icall += 1;
//        } while (iflag[0] != 0 && icall <= 200);
//        //while ( icall <= 20000);
//
//        // System.out.print("exp: ");
//        //  for (int q=0;q<mod_exp.length;q++)
//        //	System.out.println( "  "+mod_exp[q] + "::" + emp_exp[q] );
//
//
//
//        logger.info("Writing feature weights to file");
//        writeModel(x, Numberer.getGlobalNumberer("uni_featList"));
//
//        return x;
//    }
//
//    /*
//    public static void main( String args[] )
//    {
//    double x [ ] , g [ ] , diag [ ] , w [ ];
//    x = new double [ ndim ];
//    g = new double [ ndim ];
//    diag = new double [ ndim ];
//    w = new double [ nwork ];
//
//    double f, eps, xtol, gtol, t1, t2, stpmin, stpmax;
//    int iprint [ ] , iflag[] = new int[1], icall, n, m, mp, lp, j;
//    iprint = new int [ 2 ];
//    boolean diagco;
//
//    n=100;
//    m=5;
//    iprint [ 1 -1] = 1;
//    iprint [ 2 -1] = 1;
//    diagco= false;
//    eps= 1.0e-5;
//    xtol= 1.0e-16;
//    icall=0;
//    iflag[0]=0;
//
//    //x : Initial solution vector estimate
//    for ( j = 1 ; j <= n ; j += 2 )
//    {
//    x [ j -1] = - 1.2e0;
//    x [ j + 1 -1] = 1.e0;
//    }
//
//    do
//    {
//    f= 0;
//    for ( j = 1 ; j <= n ; j += 2 )
//    {
//    t1 = 1.e0 - x [ j -1];
//    t2 = 1.e1 * ( x [ j + 1 -1] - x [ j -1] * x[j-1] );
//    g [ j + 1 -1] = 2.e1 * t2;
//    g [ j -1] = - 2.e0 * ( x [ j -1] * g [ j + 1 -1] + t1 );
//    f= f+t1*t1+t2*t2;
//    }
//
//    //for (int q=0;q<n;q++)
//    //System.out.print( "  "+g[q] );
//    //System.out.println( "" );
//    //
//
//    try
//    {
//    LBFGS.lbfgs ( n , m , x , f , g , diagco , diag , iprint , eps , xtol , iflag );
//    }
//    catch (LBFGS.ExceptionWithIflag e)
//    {
//    System.err.println( "Sdrive: lbfgs failed.\n"+e );
//    return;
//    }
//
//    icall += 1;
//    }
//    while ( iflag[0] != 0 && icall <= 200 );
//    }
//     */
//}
